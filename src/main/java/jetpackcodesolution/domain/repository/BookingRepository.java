package jetpackcodesolution.domain.repository;

public interface BookingRepository {

    boolean existsByRoomId(Long roomId);
}
