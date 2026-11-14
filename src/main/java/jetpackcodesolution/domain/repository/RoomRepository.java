package jetpackcodesolution.domain.repository;

import jetpackcodesolution.domain.model.Room;

public interface RoomRepository {

    boolean existsByName(String name);

    Room save(Room room);
}
