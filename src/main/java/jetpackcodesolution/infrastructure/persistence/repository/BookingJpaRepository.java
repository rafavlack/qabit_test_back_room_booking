package jetpackcodesolution.infrastructure.persistence.repository;

import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, Long> {

    boolean existsByRoomId(Long roomId);
}
