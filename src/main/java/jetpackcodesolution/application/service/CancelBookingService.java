package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.CancelBookingUseCase;
import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CancelBookingService implements CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    public CancelBookingService(BookingRepository bookingRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (booking.hasCancellation()) {
            return;
        }

        bookingRepository.save(booking.cancelBooking(clock.instant()));
    }
}
