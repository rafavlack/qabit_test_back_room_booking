package jetpackcodesolution.domain.model;

import java.time.Instant;

public record Booking(
        Long id,
        Long roomId,
        String responsible,
        Instant startTime,
        Instant endTime,
        Instant createdAt,
        Instant cancelledAt) {

    public Booking updateBooking(Long roomId, String responsible, Instant startTime, Instant endTime) {
        return new Booking(id, roomId, responsible, startTime, endTime, createdAt, cancelledAt);
    }

    public Booking cancelBooking(Instant cancelledAt) {
        return this.cancelledAt == null
                ? new Booking(id, roomId, responsible, startTime, endTime, createdAt, cancelledAt)
                : this;
    }

    public boolean hasCancellation() {
        return cancelledAt != null;
    }
}
