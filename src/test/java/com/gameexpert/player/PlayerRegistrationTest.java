package com.gameexpert.player;

import com.gameexpert.common.ConflictException;
import com.gameexpert.player.dto.CreatePlayerRequest;
import com.gameexpert.player.entity.Player;
import com.gameexpert.player.repository.PlayerRepository;
import com.gameexpert.player.service.PlayerService;
import java.util.Optional;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerRegistrationTest {

    @Test
    void acceptsValidNicknames() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            for (String nickname : new String[]{"Ab", "player_12", "Abcdef12345_"}) {
                assertTrue(validator.validate(new CreatePlayerRequest(nickname)).isEmpty(), nickname);
            }
        }
    }

    @Test
    void rejectsInvalidNicknames() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            for (String nickname : new String[]{null, "", "  ", "A", "abcdefghijklm", "한글", "ab cd", "ab-cd", "ab!"}) {
                assertFalse(validator.validate(new CreatePlayerRequest(nickname)).isEmpty(),
                        "거절해야 하는 닉네임: " + nickname);
            }
        }
    }

    @Test
    void savesNewPlayer() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);

        service.createPlayer(new CreatePlayerRequest("player_1"));

        ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals("player_1", saved.getValue().getNickname());
    }

    @Test
    void rejectsDuplicateWithoutSaving() {
        PlayerRepository repository = mock(PlayerRepository.class);
        when(repository.existsByNickname("player_1")).thenReturn(true);
        when(repository.findByNickname("player_1"))
                .thenReturn(Optional.of(new Player("player_1")));
        PlayerService service = new PlayerService(repository);

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.createPlayer(new CreatePlayerRequest("player_1")));

        assertEquals("DUPLICATE_NICKNAME", error.getError());
        verify(repository, never()).saveAndFlush(any(Player.class));
    }
}
