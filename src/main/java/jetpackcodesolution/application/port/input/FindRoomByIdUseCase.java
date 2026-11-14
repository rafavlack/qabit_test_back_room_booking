package jetpackcodesolution.application.port.input;

import jetpackcodesolution.domain.model.Room;

public interface FindRoomByIdUseCase {

    Room findById(Long id);
}
