package jetpackcodesolution.web.mapper;

import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.web.response.BookingResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingWebMapper {

    BookingResponse toResponse(Booking booking);
}
