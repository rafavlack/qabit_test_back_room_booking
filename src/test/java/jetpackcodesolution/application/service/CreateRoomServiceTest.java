package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jetpackcodesolution.domain.exception.RoomNameConflictException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRoomServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RoomRepository roomRepository;

    private CreateRoomService service;

    @BeforeEach
    void setUp() {
        service = new CreateRoomService(roomRepository, FIXED_CLOCK);
    }

    @Test
    void createsRoomWithTrimmedNameAndFixedInstant() {
        when(roomRepository.existsByName("Sala Atlas")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            return new Room(1L, room.name(), room.capacity(), room.createdAt());
        });

        Room result = service.createRoom("  Sala Atlas  ", 12);

        assertThat(result.name()).isEqualTo("Sala Atlas");
        assertThat(result.capacity()).isEqualTo(12);
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        verify(roomRepository).existsByName("Sala Atlas");
    }

    @Test
    void rejectsDuplicateRoomName() {
        when(roomRepository.existsByName("Sala Atlas")).thenReturn(true);

        assertThatThrownBy(() -> service.createRoom("Sala Atlas", 12))
                .isInstanceOf(RoomNameConflictException.class)
                .hasMessage("Ya existe una sala con ese nombre.");

        verify(roomRepository, never()).save(any());
    }
}
