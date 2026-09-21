package com.gameexpert.trial;

import com.gameexpert.engine.trial.TrialSpawnerRuntime;
import com.gameexpert.engine.trial.persistence.TrialWorldStatePersistence;
import com.gameexpert.trial.entity.WorldTrialSite;
import com.gameexpert.trial.repository.WorldTrialSiteRepository;
import com.gameexpert.trial.service.TrialPersistenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OptimisticLockTest {
    private SessionFactory factory;
    private TransactionTemplate transactions;
    private TrialPersistenceService service;

    @BeforeEach
    void prepare() {
        factory = new Configuration()
                .addAnnotatedClass(WorldTrialSite.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:trial_" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        WorldTrialSiteRepository repository = new JpaRepositoryFactory(entityManager)
                .getRepository(WorldTrialSiteRepository.class);
        transactions = new TransactionTemplate(new JpaTransactionManager(factory));
        service = new TrialPersistenceService(repository, mock(TrialWorldStatePersistence.class));
        transactions.executeWithoutResult(status -> service.saveWorld(1L, List.of(snapshot(0))));
    }

    @AfterEach
    void close() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void staleTrialSnapshotMustNotOverwriteCommittedProgress() throws Exception {
        CountDownLatch staleSnapshotLoaded = new CountDownLatch(1);
        CountDownLatch freshProgressCommitted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<Throwable> staleSave = executor.submit(() -> {
                try {
                    transactions.executeWithoutResult(status -> service.saveWorldWithMutation(
                            1L,
                            List.of(snapshot(1)),
                            () -> {
                                // 두 트랜잭션이 같은 revision을 읽은 뒤, 최신 진행 상태를 먼저 커밋합니다.
                                staleSnapshotLoaded.countDown();
                                await(freshProgressCommitted);
                            }
                    ));
                    return null;
                } catch (RuntimeException failure) {
                    return failure;
                }
            });
            try {
                await(staleSnapshotLoaded);
                transactions.executeWithoutResult(status -> service.saveWorld(1L, List.of(snapshot(2))));
            } finally {
                freshProgressCommitted.countDown();
            }
            Throwable failure = staleSave.get(15, TimeUnit.SECONDS);
            assertThat(isOptimisticConflict(failure))
                    .as("먼저 읽은 상태의 저장은 낙관적 락 충돌로 거절되어야 합니다")
                    .isTrue();
            Integer players = transactions.execute(status -> service.hydrateWorld(1L)
                    .getFirst().detectedPlayers());
            assertThat(players).as("먼저 커밋된 진행 상태가 보존되어야 합니다").isEqualTo(2);
            transactions.executeWithoutResult(status -> service.saveWorld(1L, List.of(snapshot(3))));
            Integer updatedPlayers = transactions.execute(status -> service.hydrateWorld(1L)
                    .getFirst().detectedPlayers());
            assertThat(updatedPlayers)
                    .as("충돌 후 새 트랜잭션에서 최신 상태를 읽으면 다시 저장할 수 있습니다")
                    .isEqualTo(3);
        }
    }


    @Test
    void conflictingBatchRollsBackOtherRows() throws Exception {
        transactions.executeWithoutResult(status -> service.saveWorld(1L,
                List.of(snapshot(1L, 0), snapshot(2L, 0))));
        CountDownLatch staleLoaded = new CountDownLatch(1);
        CountDownLatch freshCommitted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<Throwable> stale = executor.submit(() -> {
                try {
                    transactions.executeWithoutResult(status -> service.saveWorldWithMutation(1L,
                            List.of(snapshot(1L, 1), snapshot(2L, 1)), () -> {
                                staleLoaded.countDown();
                                await(freshCommitted);
                            }));
                    return null;
                } catch (RuntimeException failure) {
                    return failure;
                }
            });
            try {
                await(staleLoaded);
                transactions.executeWithoutResult(status -> service.saveWorld(1L,
                        List.of(snapshot(1L, 0), snapshot(2L, 2))));
            } finally {
                freshCommitted.countDown();
            }
            assertThat(isOptimisticConflict(stale.get(15, TimeUnit.SECONDS))).isTrue();
            List<Integer> counts = transactions.execute(status -> service.hydrateWorld(1L).stream()
                    .map(TrialSpawnerRuntime.SiteSnapshot::detectedPlayers).toList());
            assertThat(counts).containsExactly(0, 2);
        }
    }

    @Test
    void independentWorldsCanBothCommit() throws Exception {
        transactions.executeWithoutResult(status -> service.saveWorld(2L, List.of(snapshot(0))));
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> first = executor.submit(() -> transactions.executeWithoutResult(status ->
                    service.saveWorldWithMutation(1L, List.of(snapshot(1)), () -> {
                        loaded.countDown();
                        await(committed);
                    })));
            try {
                await(loaded);
                transactions.executeWithoutResult(status -> service.saveWorld(2L, List.of(snapshot(2))));
            } finally {
                committed.countDown();
            }
            first.get(15, TimeUnit.SECONDS);
            Integer firstCount = transactions.execute(status -> service.hydrateWorld(1L).getFirst().detectedPlayers());
            Integer secondCount = transactions.execute(status -> service.hydrateWorld(2L).getFirst().detectedPlayers());
            assertThat(firstCount).isEqualTo(1);
            assertThat(secondCount).isEqualTo(2);
        }
    }

    private static TrialSpawnerRuntime.SiteSnapshot snapshot(long trialId, int detectedPlayers) {
        return new TrialSpawnerRuntime.SiteSnapshot(
                (int) trialId * 4, 64, 0, trialId, 90L, detectedPlayers,
                false, 18_090L, 3, null, "hero", null, false, 0L, List.of()
        );
    }

    private static TrialSpawnerRuntime.SiteSnapshot snapshot(int detectedPlayers) {
        return new TrialSpawnerRuntime.SiteSnapshot(
                0, 64, 0, 1L, 90L, detectedPlayers,
                false, 18_090L, 3, null, "hero", null, false, 0L, List.of()
        );
    }

    private static boolean isOptimisticConflict(Throwable failure) {
        if (failure == null) {
            return false;
        }
        if (failure instanceof OptimisticLockException
                || failure instanceof OptimisticLockingFailureException
                || failure instanceof StaleObjectStateException) {
            return true;
        }
        return failure.getCause() != failure && isOptimisticConflict(failure.getCause());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("트랜잭션 동기화 대기 시간이 초과되었습니다");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
