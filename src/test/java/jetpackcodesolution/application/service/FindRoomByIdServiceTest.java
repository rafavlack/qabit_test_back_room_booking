package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindRoomByIdServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private FindRoomByIdService service;

    @BeforeEach
    void setUp() {
        service = new FindRoomByIdService(roomRepository);
    }

    @Test
    void returnsRoomWhenItExists() {
        Room room = new Room(7L, "Sala Norte", 10, Instant.now());
        when(roomRepository.findById(7L)).thenReturn(Optional.of(room));

        assertThat(service.findById(7L)).isEqualTo(room);
    }

    @Test
    void throwsWhenRoomDoesNotExist() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("No existe una sala con el identificador 99.");
    }
}
