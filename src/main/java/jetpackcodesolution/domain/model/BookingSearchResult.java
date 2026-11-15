package jetpackcodesolution.domain.model;

import java.util.List;

public record BookingSearchResult(List<Booking> bookings, long totalElements) {
}
