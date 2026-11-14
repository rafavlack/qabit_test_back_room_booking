package jetpackcodesolution.infrastructure.persistence.repository;

import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Long> {

    boolean existsByName(String name);

    List<RoomEntity> findAllByOrderByIdAsc();
}
