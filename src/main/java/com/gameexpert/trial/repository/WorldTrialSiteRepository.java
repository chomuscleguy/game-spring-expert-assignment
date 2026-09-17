package com.gameexpert.trial.repository;

import com.gameexpert.trial.entity.WorldTrialSite;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorldTrialSiteRepository extends JpaRepository<WorldTrialSite, Long> {
    List<WorldTrialSite> findAllByWorldIdOrderByTrialIdAsc(Long worldId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WorldTrialSite s where s.worldId = :worldId and s.trialId = :trialId")
    Optional<WorldTrialSite> lockByWorldIdAndTrialId(@Param("worldId") Long worldId,
            @Param("trialId") long trialId);
    void deleteAllByWorldId(Long worldId);
}
