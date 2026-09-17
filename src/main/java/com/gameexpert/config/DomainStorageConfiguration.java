package com.gameexpert.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gameexpert.api.persistence.PlayerAccess;
import com.gameexpert.api.persistence.PlayerStore;
import com.gameexpert.api.persistence.WorldAccess;
import com.gameexpert.api.persistence.WorldStore;
import com.gameexpert.engine.Difficulty;
import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.world.WorldGenerationProfile;
import com.gameexpert.world.entity.World;
import com.gameexpert.world.repository.WorldRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainStorageConfiguration {
    @Bean
    WorldStore worldStore(WorldRepository repository) {
        return new WorldStore() {
            public WorldAccess getReferenceById(Long id) {
                return repository.getReferenceById(id);
            }
            public Optional<WorldAccess> findById(Long id) {
                return repository.findById(id).map(world -> world);
            }
            public Optional<WorldAccess> findByIdForUpdate(Long id) {
                return repository.findByIdForUpdate(id).map(world -> world);
            }
            public Optional<WorldAccess> findByIdForShare(Long id) {
                return repository.findByIdForShare(id).map(world -> world);
            }
            public Optional<WorldAccess> findByIdForShareNoWait(Long id) {
                return repository.findByIdForShareNoWait(id).map(world -> world);
            }
            public List<WorldAccess> findAll() {
                return new ArrayList<>(repository.findAll());
            }
            public List<WorldAccess> findRootWorlds() {
                return new ArrayList<>(repository.findRootWorlds());
            }
            public boolean existsById(Long id) {
                return repository.existsById(id);
            }
            public boolean isDimensionChild(Long id) {
                return repository.isDimensionChild(id);
            }
            public long countRootWorlds() {
                return repository.countRootWorlds();
            }
            public WorldAccess save(WorldAccess world) {
                return repository.save((World) world);
            }
            public WorldAccess saveAndFlush(WorldAccess world) {
                return repository.saveAndFlush((World) world);
            }
            public void delete(WorldAccess world) {
                repository.delete((World) world);
            }
            public WorldAccess create(String name, long seed, Difficulty difficulty,
                    String ownerNickname, WorldGenerationProfile profile) {
                return new World(name, seed, difficulty, ownerNickname, profile);
            }
        };
    }

    @Bean
    PlayerStore playerStore(PlayerRepository repository) {
        return new PlayerStore() {
            public PlayerAccess getReferenceById(Long id) {
                return repository.getReferenceById(id);
            }
            public Optional<PlayerAccess> findById(Long id) {
                return repository.findById(id).map(player -> player);
            }
            public Optional<PlayerAccess> findByNickname(String nickname) {
                return repository.findByNickname(nickname).map(player -> player);
            }
        };
    }
}
