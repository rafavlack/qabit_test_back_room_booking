package jetpackcodesolution.domain.repository;

import jetpackcodesolution.domain.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    boolean existsByName(String name);

    boolean existsByNameExcludingId(String name, Long id);

    Room save(Room room);

    List<Room> findAll();

    Optional<Room> findById(Long id);

    Optional<Room> findByIdForUpdate(Long id);

    void deleteById(Long id);
}
