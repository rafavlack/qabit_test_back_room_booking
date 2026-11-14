package jetpackcodesolution.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jetpackcodesolution.room.Room;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookings_room"))
    private Room room;

    @Column(nullable = false, length = 100)
    private String responsible;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public Booking(Room room, String responsible, Instant startTime, Instant endTime, Instant createdAt) {
        this.room = room;
        this.responsible = responsible;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
    }

    public void update(Room room, String responsible, Instant startTime, Instant endTime) {
        this.room = room;
        this.responsible = responsible;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void cancel(Instant now) {
        if (cancelledAt == null) {
            cancelledAt = now;
        }
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

}
