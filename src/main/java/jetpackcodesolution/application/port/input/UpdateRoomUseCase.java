package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Room;

public interface UpdateRoomUseCase {

    Room updateRoom(Long id, String name, int capacity);
}
