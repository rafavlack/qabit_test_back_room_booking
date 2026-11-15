package jetpackcodesolution.infrastructure.persistence;

import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.infrastructure.persistence.repository.BookingJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BookingPersistenceAdapter implements BookingRepository {

    private final BookingJpaRepository bookingJpaRepository;

    public BookingPersistenceAdapter(BookingJpaRepository bookingJpaRepository) {
        this.bookingJpaRepository = bookingJpaRepository;
    }

    @Override
    public boolean existsByRoomId(Long roomId) {
        return bookingJpaRepository.existsByRoomId(roomId);
    }
}
