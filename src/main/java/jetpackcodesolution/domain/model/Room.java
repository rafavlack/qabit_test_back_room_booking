package jetpackcodesolution.domain.model;

import java.time.Instant;

public record Room(Long id, String name, int capacity, Instant createdAt) {
}
