package jetpackcodesolution.infrastructure.persistence.repository;

import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Long> {

    boolean existsByName(String name);
}
