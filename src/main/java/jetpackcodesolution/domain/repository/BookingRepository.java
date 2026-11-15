package jetpackcodesolution.domain.repository;

import jetpackcodesolution.domain.model.Booking;

import java.time.Instant;

public interface BookingRepository {

    boolean existsByRoomId(Long roomId);

    boolean existsOverlapping(Long roomId, Instant startTime, Instant endTime);

    Booking save(Booking booking);
}
