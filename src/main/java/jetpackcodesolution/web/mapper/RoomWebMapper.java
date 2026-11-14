package jetpackcodesolution.web.mapper;

import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.web.response.RoomResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomWebMapper {

    RoomResponse toResponse(Room room);
}
