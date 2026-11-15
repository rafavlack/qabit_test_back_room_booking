package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Booking;

public interface FindBookingByIdUseCase {

    Booking findById(Long id);
}
