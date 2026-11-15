package jetpackcodesolution.infrastructure.persistence;

import jetpackcodesolution.domain.exception.BookingConflictException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import jetpackcodesolution.infrastructure.persistence.mapper.BookingPersistenceMapper;
import jetpackcodesolution.infrastructure.persistence.repository.BookingJpaRepository;
import jetpackcodesolution.infrastructure.persistence.repository.RoomJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class BookingPersistenceAdapter implements BookingRepository {

    private final BookingJpaRepository bookingJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final BookingPersistenceMapper bookingPersistenceMapper;

    public BookingPersistenceAdapter(
            BookingJpaRepository bookingJpaRepository,
            RoomJpaRepository roomJpaRepository,
            BookingPersistenceMapper bookingPersistenceMapper) {
        this.bookingJpaRepository = bookingJpaRepository;
        this.roomJpaRepository = roomJpaRepository;
        this.bookingPersistenceMapper = bookingPersistenceMapper;
    }

    @Override
    public boolean existsByRoomId(Long roomId) {
        return bookingJpaRepository.existsByRoomId(roomId);
    }

    @Override
    public boolean existsOverlapping(Long roomId, Instant startTime, Instant endTime) {
        return bookingJpaRepository.countOverlapping(roomId, startTime, endTime) > 0;
    }

    @Override
    public Booking save(Booking booking) {
        try {
            RoomEntity room = roomJpaRepository.getReferenceById(booking.roomId());
            BookingEntity entity = bookingPersistenceMapper.toEntity(booking, room);
            return bookingPersistenceMapper.toDomain(bookingJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new BookingConflictException();
        }
    }
}
