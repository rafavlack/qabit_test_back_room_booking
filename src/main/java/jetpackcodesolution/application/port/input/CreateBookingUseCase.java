package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Booking;

import java.time.Instant;

public interface CreateBookingUseCase {

    Booking createBooking(Long roomId, String responsible, Instant startTime, Instant endTime);
}
