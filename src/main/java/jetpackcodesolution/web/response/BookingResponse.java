package jetpackcodesolution.web.response;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long roomId,
        String responsible,
        Instant startTime,
        Instant endTime,
        Instant createdAt,
        Instant cancelledAt) {
}
