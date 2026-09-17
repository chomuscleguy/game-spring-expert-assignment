package com.gameexpert.world.repository;

import com.gameexpert.world.entity.World;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface WorldRepository extends JpaRepository<World, Long> {
    @Query("select count(d) > 0 from WorldDimension d where d.child.id = :id")
    boolean isDimensionChild(@Param("id") Long id);

    @Query("select world from World world where not exists "
            + "(select d.id from WorldDimension d where d.child.id = world.id) order by world.id")
    List<World> findRootWorlds();

    @Query("select count(world) from World world where not exists "
            + "(select d.id from WorldDimension d where d.child.id = world.id)")
    long countRootWorlds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select world from World world where world.id = :id")
    Optional<World> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select world from World world where world.id = :id")
    Optional<World> findByIdForShare(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select world from World world where world.id = :id")
    Optional<World> findByIdForShareNoWait(@Param("id") Long id);
}
