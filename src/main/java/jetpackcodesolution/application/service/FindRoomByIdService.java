package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.FindRoomByIdUseCase;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindRoomByIdService implements FindRoomByIdUseCase {

    private final RoomRepository roomRepository;

    public FindRoomByIdService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }
}
