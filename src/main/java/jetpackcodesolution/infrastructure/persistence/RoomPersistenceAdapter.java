package jetpackcodesolution.infrastructure.persistence;

import jetpackcodesolution.domain.exception.RoomNameConflictException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import jetpackcodesolution.infrastructure.persistence.mapper.RoomPersistenceMapper;
import jetpackcodesolution.infrastructure.persistence.repository.RoomJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomPersistenceAdapter implements RoomRepository {

    private final RoomJpaRepository roomJpaRepository;
    private final RoomPersistenceMapper roomPersistenceMapper;

    public RoomPersistenceAdapter(
            RoomJpaRepository roomJpaRepository,
            RoomPersistenceMapper roomPersistenceMapper) {
        this.roomJpaRepository = roomJpaRepository;
        this.roomPersistenceMapper = roomPersistenceMapper;
    }

    @Override
    public boolean existsByName(String name) {
        return roomJpaRepository.existsByName(name);
    }

    @Override
    public Room save(Room room) {
        try {
            return roomPersistenceMapper.toDomain(
                    roomJpaRepository.saveAndFlush(roomPersistenceMapper.toEntity(room)));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomNameConflictException();
        }
    }

    @Override
    public List<Room> findAll() {
        return roomJpaRepository.findAllByOrderByIdAsc().stream()
                .map(roomPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return roomJpaRepository.findById(id).map(roomPersistenceMapper::toDomain);
    }
}
