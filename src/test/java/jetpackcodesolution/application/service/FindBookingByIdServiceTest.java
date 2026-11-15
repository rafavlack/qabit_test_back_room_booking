package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindBookingByIdServiceTest {

    private static final Booking BOOKING = new Booking(
            9L,
            3L,
            "ana",
            Instant.parse("2026-12-01T10:00:00Z"),
            Instant.parse("2026-12-01T11:00:00Z"),
            Instant.parse("2026-11-01T08:00:00Z"),
            null);

    @Mock
    private BookingRepository bookingRepository;

    private FindBookingByIdService service;

    @BeforeEach
    void setUp() {
        service = new FindBookingByIdService(bookingRepository);
    }

    @Test
    void returnsBookingWhenItExists() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(BOOKING));

        assertThat(service.findById(9L)).isEqualTo(BOOKING);
    }

    @Test
    void throwsWhenBookingDoesNotExist() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("No existe una reserva con el identificador 99.");
    }
}
