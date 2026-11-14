package jetpackcodesolution.infrastructure.persistence.mapper;

import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomPersistenceMapper {

    RoomEntity toEntity(Room room);

    Room toDomain(RoomEntity entity);
}
