package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelBookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant START = Instant.parse("2026-01-02T09:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2025-12-01T08:00:00Z");
    private static final Instant CANCELLED_AT = Instant.parse("2025-12-15T00:00:00Z");
    private static final Booking BOOKING =
            new Booking(9L, 5L, "luis", START, END, CREATED_AT, null);

    @Mock
    private BookingRepository bookingRepository;

    private CancelBookingService service;

    @BeforeEach
    void setUp() {
        service = new CancelBookingService(bookingRepository, FIXED_CLOCK);
    }

    @Test
    void cancelsBookingWithCurrentInstantPreservingItsData() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BOOKING));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelBooking(9L);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(9L);
        assertThat(saved.getValue().roomId()).isEqualTo(5L);
        assertThat(saved.getValue().responsible()).isEqualTo("luis");
        assertThat(saved.getValue().startTime()).isEqualTo(START);
        assertThat(saved.getValue().endTime()).isEqualTo(END);
        assertThat(saved.getValue().createdAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getValue().cancelledAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotSaveAgainWhenBookingIsAlreadyCancelled() {
        Booking cancelled = BOOKING.cancelBooking(CANCELLED_AT);
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(cancelled));

        service.cancelBooking(9L);

        verify(bookingRepository, never()).save(any());
        assertThat(cancelled.cancelledAt()).isEqualTo(CANCELLED_AT);
    }

    @Test
    void throwsWhenBookingDoesNotExist() {
        when(bookingRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelBooking(99L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("No existe una reserva con el identificador 99.");

        verify(bookingRepository, never()).save(any());
    }
}
