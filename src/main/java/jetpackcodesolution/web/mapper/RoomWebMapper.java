package jetpackcodesolution.web.mapper;

import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.web.response.RoomResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomWebMapper {

    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);
}
