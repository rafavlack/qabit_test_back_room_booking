package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.UpdateRoomUseCase;
import jetpackcodesolution.domain.exception.RoomNameConflictException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRoomService implements UpdateRoomUseCase {

    private final RoomRepository roomRepository;

    public UpdateRoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    @Override
    public Room updateRoom(Long id, String name, int capacity) {
        Room existing = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));

        String trimmedName = name.trim();
        if (roomRepository.existsByNameExcludingId(trimmedName, id)) {
            throw new RoomNameConflictException();
        }

        return roomRepository.save(new Room(id, trimmedName, capacity, existing.createdAt()));
    }
}
