package jetpackcodesolution.web.response;

import java.time.Instant;

public record RoomResponse(Long id, String name, int capacity, Instant createdAt) {
}
