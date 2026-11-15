package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import jetpackcodesolution.domain.exception.BookingConflictException;
import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.exception.CancelledBookingUpdateException;
import jetpackcodesolution.domain.exception.InvalidBookingTimeException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateBookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant START = Instant.parse("2026-01-02T09:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2025-12-01T08:00:00Z");
    private static final Room ROOM = new Room(6L, "Sala Atlas", 8, NOW);
    private static final Booking BOOKING =
            new Booking(9L, 5L, "luis", START, END, CREATED_AT, null);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    private UpdateBookingService service;

    @BeforeEach
    void setUp() {
        service = new UpdateBookingService(roomRepository, bookingRepository, FIXED_CLOCK);
    }

    @Test
    void updatesBookingPreservingCreationTimeAndCancellationState() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BOOKING));
        when(roomRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(ROOM));
        when(bookingRepository.existsOverlappingExcluding(6L, START, END, 9L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = service.updateBooking(9L, 6L, "  ana  ", START, END);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(9L);
        assertThat(saved.getValue().roomId()).isEqualTo(6L);
        assertThat(saved.getValue().responsible()).isEqualTo("ana");
        assertThat(saved.getValue().startTime()).isEqualTo(START);
        assertThat(saved.getValue().endTime()).isEqualTo(END);
        assertThat(saved.getValue().createdAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getValue().cancelledAt()).isNull();
        assertThat(result.id()).isEqualTo(9L);
    }

    @Test
    void rejectsIntervalWhoseEndIsNotAfterStart() {
        assertThatThrownBy(() -> service.updateBooking(9L, 6L, "ana", START, START))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessage("La fecha de fin debe ser posterior a la fecha de inicio.");

        verifyNoInteractions(bookingRepository, roomRepository);
    }

    @Test
    void rejectsBookingThatDoesNotStartInTheFuture() {
        assertThatThrownBy(() -> service.updateBooking(9L, 6L, "ana", NOW, END))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessage("La reserva debe comenzar posterior al momento actual.");

        verifyNoInteractions(bookingRepository, roomRepository);
    }

    @Test
    void throwsWhenBookingDoesNotExist() {
        when(bookingRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBooking(99L, 6L, "ana", START, END))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("No existe una reserva con el identificador 99.");

        verify(roomRepository, never()).findByIdForUpdate(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void throwsWhenRoomDoesNotExist() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BOOKING));
        when(roomRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBooking(9L, 99L, "ana", START, END))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("No existe una sala con el identificador 99.");

        verify(bookingRepository, never())
                .existsOverlappingExcluding(anyLong(), any(), any(), anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsCancelledBooking() {
        Booking cancelled = BOOKING.cancelBooking(Instant.parse("2025-12-15T00:00:00Z"));
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(cancelled));
        when(roomRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(ROOM));

        assertThatThrownBy(() -> service.updateBooking(9L, 6L, "ana", START, END))
                .isInstanceOf(CancelledBookingUpdateException.class)
                .hasMessage("No se puede modificar la reserva 9 porque está cancelada.");

        verify(bookingRepository, never())
                .existsOverlappingExcluding(anyLong(), any(), any(), anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void checksRoomBeforeCancellationState() {
        Booking cancelled = BOOKING.cancelBooking(Instant.parse("2025-12-15T00:00:00Z"));
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(cancelled));
        when(roomRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBooking(9L, 99L, "ana", START, END))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void rejectsOverlapExcludingTheBookingItself() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BOOKING));
        when(roomRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(ROOM));
        when(bookingRepository.existsOverlappingExcluding(6L, START, END, 9L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateBooking(9L, 6L, "ana", START, END))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Ya existe una reserva que se solapa en esa sala.");

        verify(bookingRepository, never()).save(any());
    }
}
