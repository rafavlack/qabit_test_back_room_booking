package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.CreateRoomUseCase;
import jetpackcodesolution.domain.exception.RoomNameConflictException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class CreateRoomService implements CreateRoomUseCase {

    private final RoomRepository roomRepository;
    private final Clock clock;

    public CreateRoomService(RoomRepository roomRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public Room createRoom(String roomName, int capacity) {
        String name = roomName.trim();
        if (roomRepository.existsByName(name)) {
            throw new RoomNameConflictException();
        }

        return roomRepository.save(new Room(null, name, capacity, Instant.now(clock)));
    }
}
