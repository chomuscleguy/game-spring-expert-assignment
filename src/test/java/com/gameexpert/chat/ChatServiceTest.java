package com.gameexpert.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.chat.entity.ChatMessage;
import com.gameexpert.chat.repository.ChatMessageRepository;
import com.gameexpert.chat.service.ChatService;
import com.gameexpert.common.NotFoundException;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class ChatServiceTest {
    private SessionFactory factory;
    private EntityManager entityManager;
    private TransactionTemplate transactions;
    private World world;
    private World otherWorld;
    private ChatService service;
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void prepare() {
        factory = new Configuration()
                .addAnnotatedClass(World.class)
                .addAnnotatedClass(ChatMessage.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:chat_" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        transactions = new TransactionTemplate(new JpaTransactionManager(factory));
        world = createWorld("first");
        otherWorld = createWorld("second");
        WorldRepository worlds = mock(WorldRepository.class);
        when(worlds.existsById(world.getId())).thenReturn(true);
        when(worlds.existsById(otherWorld.getId())).thenReturn(true);
        when(worlds.findById(world.getId())).thenReturn(Optional.of(world));
        service = new ChatService(
                new JpaRepositoryFactory(entityManager).getRepository(ChatMessageRepository.class),
                worlds,
                mock(ApplicationEventPublisher.class)
        );
    }

    @AfterEach
    void close() {
        if (factory != null) {
            factory.close();
        }
    }

    private World createWorld(String name) {
        World fixture = BeanUtils.instantiateClass(World.class);
        ReflectionTestUtils.setField(fixture, "name", name);
        transactions.executeWithoutResult(status -> entityManager.persist(fixture));
        return fixture;
    }

    private Long insert(World target, String content, LocalDateTime time) {
        return transactions.execute(status -> {
            ChatMessage message = new ChatMessage(entityManager.getReference(World.class, target.getId()), "Alice", content);
            entityManager.persist(message);
            entityManager.flush();
            // 저장 시각을 고정하여 같은 시각의 메시지도 재현합니다.
            entityManager.createQuery("update ChatMessage m set m.createdAt = :time where m.id = :id")
                    .setParameter("time", time)
                    .setParameter("id", message.getId())
                    .executeUpdate();
            return message.getId();
        });
    }

    @Test
    void savesMessageAndReturnsStoredFields() {
        ChatMessageResponse response = transactions.execute(status -> service.saveMessage(world.getId(), "Alice", "안녕하세요"));
        List<ChatMessage> saved = transactions.execute(status -> entityManager
                .createQuery("select m from ChatMessage m", ChatMessage.class).getResultList());
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getWorld().getId()).isEqualTo(world.getId());
        assertThat(saved.getFirst().getSenderNickname()).isEqualTo("Alice");
        assertThat(saved.getFirst().getContent()).isEqualTo("안녕하세요");
        assertThat(response.getSender()).isEqualTo("Alice");
        assertThat(response.getContent()).isEqualTo("안녕하세요");
        assertThat(response.getCreatedAt()).isNotNull().isEqualTo(saved.getFirst().getCreatedAt());
    }

    @Test
    void selectsLatestThenReturnsAscendingAndIsolatesWorlds() {
        insert(world, "newer", TIME.plusSeconds(1));
        insert(world, "old", TIME.minusSeconds(1));
        insert(world, "tie-first", TIME);
        insert(otherWorld, "foreign", TIME.plusDays(1));
        insert(world, "tie-last", TIME);
        List<ChatMessageResponse> messages = transactions.execute(status -> service.getRecentMessages(world.getId(), 3));
        assertThat(messages).extracting(ChatMessageResponse::getContent)
                .containsExactly("tie-first", "tie-last", "newer");
    }

    @Test
    void appliesLimitBoundsAndReturnsEmptyWhenNoMessages() {
        List<ChatMessageResponse> empty = transactions.execute(status -> service.getRecentMessages(world.getId(), 10));
        assertThat(empty).isEmpty();
        IntStream.rangeClosed(1, 105).forEach(index -> insert(world, "message-" + index, TIME));
        List<ChatMessageResponse> minimum = transactions.execute(status -> service.getRecentMessages(world.getId(), 0));
        List<ChatMessageResponse> maximum = transactions.execute(status -> service.getRecentMessages(world.getId(), 200));
        assertThat(minimum).extracting(ChatMessageResponse::getContent).containsExactly("message-105");
        assertThat(maximum).hasSize(100);
        assertThat(maximum.getFirst().getContent()).isEqualTo("message-6");
        assertThat(maximum.getLast().getContent()).isEqualTo("message-105");
    }

    @Test
    void refusesToStoreInMissingWorld() {
        assertThatThrownBy(() -> transactions.execute(status -> service.saveMessage(-1L, "Alice", "hello")))
                .isInstanceOf(NotFoundException.class);
        Long count = transactions.execute(status -> entityManager.createQuery("select count(m) from ChatMessage m", Long.class).getSingleResult());
        assertThat(count).isZero();
    }
}
