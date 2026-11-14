package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListRoomsServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private ListRoomsService service;

    @BeforeEach
    void setUp() {
        service = new ListRoomsService(roomRepository);
    }

    @Test
    void returnsAllRoomsFromRepository() {
        Room first = new Room(1L, "Sala Uno", 5, Instant.now());
        Room second = new Room(2L, "Sala Dos", 8, Instant.now());
        when(roomRepository.findAll()).thenReturn(List.of(first, second));

        assertThat(service.listRooms()).containsExactly(first, second);
    }

    @Test
    void returnsEmptyListWhenNoRoomsExist() {
        when(roomRepository.findAll()).thenReturn(List.of());

        assertThat(service.listRooms()).isEmpty();
    }
}
