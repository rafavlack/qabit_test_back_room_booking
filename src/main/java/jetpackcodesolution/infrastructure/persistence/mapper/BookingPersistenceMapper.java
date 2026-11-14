package jetpackcodesolution.infrastructure.persistence.mapper;

import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingPersistenceMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "booking.id")
    @Mapping(target = "cancelledAt", source = "booking.cancelledAt")
    @Mapping(target = "room", source = "room")
    @Mapping(target = "responsible", source = "booking.responsible")
    @Mapping(target = "startTime", source = "booking.startTime")
    @Mapping(target = "endTime", source = "booking.endTime")
    @Mapping(target = "createdAt", source = "booking.createdAt")
    BookingEntity toEntity(Booking booking, RoomEntity room);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "responsible", source = "responsible")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "cancelledAt", source = "cancelledAt")
    Booking toDomain(BookingEntity entity);
}
