package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Booking;

import java.time.Instant;

public interface UpdateBookingUseCase {

    Booking updateBooking(
            Long id,
            Long roomId,
            String responsible,
            Instant startTime,
            Instant endTime);
}
