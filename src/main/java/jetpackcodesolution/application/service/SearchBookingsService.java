package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.SearchBookingsUseCase;
import jetpackcodesolution.domain.exception.InvalidPageRequestException;
import jetpackcodesolution.domain.model.BookingSearchCriteria;
import jetpackcodesolution.domain.model.BookingSearchResult;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class SearchBookingsService implements SearchBookingsUseCase {

    static final int MAX_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final ZoneId bookingZone;

    public SearchBookingsService(BookingRepository bookingRepository, ZoneId bookingZone) {
        this.bookingRepository = bookingRepository;
        this.bookingZone = bookingZone;
    }

    @Transactional(readOnly = true)
    @Override
    public BookingSearchResult searchBookings(
            Long roomId,
            LocalDate date,
            boolean includeCancelled,
            int page,
            int size) {
        validatePagination(page, size);

        Instant dayStart = null;
        Instant dayEnd = null;
        if (date != null) {
            dayStart = date.atStartOfDay(bookingZone).toInstant();
            dayEnd = date.plusDays(1).atStartOfDay(bookingZone).toInstant();
        }

        return bookingRepository.search(new BookingSearchCriteria(
                roomId, dayStart, dayEnd, includeCancelled, page, size));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("El parámetro 'page' no puede ser negativo.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageRequestException(
                    "El parámetro 'size' debe estar entre 1 y " + MAX_SIZE + ".");
        }
    }
}
