package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.model.BookingSearchResult;

import java.time.LocalDate;

public interface SearchBookingsUseCase {

    BookingSearchResult searchBookings(
            Long roomId,
            LocalDate date,
            boolean includeCancelled,
            int page,
            int size);
}
