package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Room;

public interface CreateRoomUseCase {

    Room createRoom(String name, int capacity);
}
