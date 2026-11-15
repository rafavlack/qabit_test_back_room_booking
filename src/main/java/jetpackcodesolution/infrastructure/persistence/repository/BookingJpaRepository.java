package jetpackcodesolution.infrastructure.persistence.repository;

import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, Long> {

    boolean existsByRoomId(Long roomId);

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
