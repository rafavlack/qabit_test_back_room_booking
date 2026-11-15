package jetpackcodesolution.domain.model;

import java.time.Instant;

public record BookingSearchCriteria(
        Long roomId,
        Instant dayStart,
        Instant dayEnd,
        boolean includeCancelled,
        int page,
        int size) {
}
