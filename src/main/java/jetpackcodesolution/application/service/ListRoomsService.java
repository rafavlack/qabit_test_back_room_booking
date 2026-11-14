package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.ListRoomsUseCase;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRoomsService implements ListRoomsUseCase {

    private final RoomRepository roomRepository;

    public ListRoomsService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }
}
