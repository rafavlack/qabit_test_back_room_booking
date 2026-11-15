package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import jetpackcodesolution.domain.exception.BookingConflictException;
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
class CreateBookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant START = Instant.parse("2026-01-02T09:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T10:00:00Z");
    private static final Room ROOM = new Room(5L, "Sala Atlas", 8, NOW);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    private CreateBookingService service;

    @BeforeEach
    void setUp() {
        service = new CreateBookingService(roomRepository, bookingRepository, FIXED_CLOCK);
    }

    @Test
    void createsBookingWithTrimmedResponsibleAndClockInstant() {
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(ROOM));
        when(bookingRepository.existsOverlapping(5L, START, END)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            return new Booking(11L, booking.roomId(), booking.responsible(),
                    booking.startTime(), booking.endTime(), booking.createdAt(), booking.cancelledAt());
        });

        Booking result = service.createBooking(5L, "  ana  ", START, END);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isNull();
        assertThat(saved.getValue().roomId()).isEqualTo(5L);
        assertThat(saved.getValue().responsible()).isEqualTo("ana");
        assertThat(saved.getValue().startTime()).isEqualTo(START);
        assertThat(saved.getValue().endTime()).isEqualTo(END);
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
        assertThat(saved.getValue().cancelledAt()).isNull();
        assertThat(result.id()).isEqualTo(11L);
    }

    @Test
    void rejectsIntervalWhoseEndIsNotAfterStart() {
        assertThatThrownBy(() -> service.createBooking(5L, "ana", START, START))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessage("La fecha de fin debe ser posterior a la fecha de inicio.");

        verify(roomRepository, never()).findByIdForUpdate(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsBookingThatDoesNotStartInTheFuture() {
        assertThatThrownBy(() -> service.createBooking(5L, "ana", NOW, END))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessage("La reserva debe comenzar posterior al momento actual.");

        verify(roomRepository, never()).findByIdForUpdate(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void validatesIntervalBeforeLookingUpTheRoom() {
        assertThatThrownBy(() -> service.createBooking(9999L, "ana", NOW.minusSeconds(60), NOW))
                .isInstanceOf(InvalidBookingTimeException.class);

        verify(roomRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void throwsWhenRoomDoesNotExist() {
        when(roomRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBooking(99L, "ana", START, END))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("No existe una sala con el identificador 99.");

        verify(bookingRepository, never()).existsOverlapping(anyLong(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsOverlappingBooking() {
        when(roomRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(ROOM));
        when(bookingRepository.existsOverlapping(5L, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.createBooking(5L, "ana", START, END))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Ya existe una reserva que se solapa en esa sala.");

        verify(bookingRepository, never()).save(any());
    }
}
