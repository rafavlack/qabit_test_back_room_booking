package jetpackcodesolution.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, Long>, JpaSpecificationExecutor<BookingEntity> {

    boolean existsByRoomId(Long roomId);

    @EntityGraph(attributePaths = "room")
    @Override
    Optional<BookingEntity> findById(Long id);

    @EntityGraph(attributePaths = "room")
    @Override
    Page<BookingEntity> findAll(Specification<BookingEntity> specification, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.id = :id")
    Optional<BookingEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(b)
            from BookingEntity b
            where b.room.id = :roomId
              and b.id <> :excludedId
              and b.cancelledAt is null
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    long countOverlappingExcluding(
            @Param("roomId") Long roomId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludedId") Long excludedId);

    @Query("""
            select count(b)
            from BookingEntity b
            where b.room.id = :roomId
              and b.cancelledAt is null
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    long countOverlapping(
            @Param("roomId") Long roomId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
