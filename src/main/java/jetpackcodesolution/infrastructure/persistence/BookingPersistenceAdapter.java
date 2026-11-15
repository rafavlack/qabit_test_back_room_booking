package jetpackcodesolution.infrastructure.persistence;

import jetpackcodesolution.domain.exception.BookingConflictException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.model.BookingSearchCriteria;
import jetpackcodesolution.domain.model.BookingSearchResult;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import jetpackcodesolution.infrastructure.persistence.mapper.BookingPersistenceMapper;
import jetpackcodesolution.infrastructure.persistence.repository.BookingJpaRepository;
import jetpackcodesolution.infrastructure.persistence.repository.RoomJpaRepository;
import jetpackcodesolution.infrastructure.persistence.specification.BookingSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class BookingPersistenceAdapter implements BookingRepository {

    private static final Sort SEARCH_SORT = Sort.by(
            Sort.Order.asc("startTime"), Sort.Order.asc("id"));

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

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingJpaRepository.findById(id).map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdForUpdate(Long id) {
        return bookingJpaRepository.findByIdForUpdate(id)
                .map(bookingPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsOverlappingExcluding(
            Long roomId, Instant startTime, Instant endTime, Long id) {
        return bookingJpaRepository.countOverlappingExcluding(
                roomId, startTime, endTime, id) > 0;
    }

    @Override
    public BookingSearchResult search(BookingSearchCriteria criteria) {
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), SEARCH_SORT);
        Page<BookingEntity> page = bookingJpaRepository.findAll(
                BookingSpecifications.matching(criteria), pageable);
        return new BookingSearchResult(
                page.getContent().stream().map(bookingPersistenceMapper::toDomain).toList(),
                page.getTotalElements());
    }
}
