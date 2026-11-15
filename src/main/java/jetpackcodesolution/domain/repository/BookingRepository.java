package jetpackcodesolution.domain.repository;

import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.model.BookingSearchCriteria;
import jetpackcodesolution.domain.model.BookingSearchResult;

import java.time.Instant;
import java.util.Optional;

public interface BookingRepository {

    boolean existsByRoomId(Long roomId);

    boolean existsOverlapping(Long roomId, Instant startTime, Instant endTime);

    Booking save(Booking booking);

    Optional<Booking> findById(Long id);

    Optional<Booking> findByIdForUpdate(Long id);

    boolean existsOverlappingExcluding(Long roomId, Instant startTime, Instant endTime, Long id);

    BookingSearchResult search(BookingSearchCriteria criteria);
}
