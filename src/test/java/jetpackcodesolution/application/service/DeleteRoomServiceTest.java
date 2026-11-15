package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import jetpackcodesolution.domain.exception.RoomHasBookingsException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteRoomServiceTest {

    private static final Room EXISTING_ROOM =
            new Room(3L, "Sala Norte", 10, Instant.parse("2026-01-01T10:00:00Z"));

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    private DeleteRoomService service;

    @BeforeEach
    void setUp() {
        service = new DeleteRoomService(roomRepository, bookingRepository);
    }

    @Test
    void deletesRoomWithoutBookings() {
        when(roomRepository.findById(3L)).thenReturn(Optional.of(EXISTING_ROOM));
        when(bookingRepository.existsByRoomId(3L)).thenReturn(false);

        assertThatCode(() -> service.deleteRoom(3L)).doesNotThrowAnyException();

        verify(roomRepository).deleteById(3L);
    }

    @Test
    void throwsWhenRoomDoesNotExist() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRoom(99L))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("No existe una sala con el identificador 99.");

        verify(bookingRepository, never()).existsByRoomId(anyLong());
        verify(roomRepository, never()).deleteById(anyLong());
    }

    @Test
    void rejectsRoomWithBookings() {
        when(roomRepository.findById(3L)).thenReturn(Optional.of(EXISTING_ROOM));
        when(bookingRepository.existsByRoomId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteRoom(3L))
                .isInstanceOf(RoomHasBookingsException.class)
                .hasMessage("No se puede eliminar la sala porque tiene reservas asociadas.");

        verify(roomRepository, never()).deleteById(anyLong());
    }
}
