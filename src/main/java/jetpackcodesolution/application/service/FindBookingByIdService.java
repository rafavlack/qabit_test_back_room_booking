package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.FindBookingByIdUseCase;
import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindBookingByIdService implements FindBookingByIdUseCase {

    private final BookingRepository bookingRepository;

    public FindBookingByIdService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }
}
