package jetpackcodesolution.web.mapper;

import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.web.response.BookingResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingWebMapper {

    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toResponseList(List<Booking> bookings);
}
