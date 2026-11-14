package jetpackcodesolution.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uq_rooms_name", columnNames = "name"))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Room(String name, int capacity, Instant createdAt) {
        this.name = name;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }

    public void update(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }
}
