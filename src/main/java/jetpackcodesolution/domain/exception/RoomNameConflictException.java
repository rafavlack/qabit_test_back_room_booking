package jetpackcodesolution.domain.exception;

public class RoomNameConflictException extends RuntimeException {

    public RoomNameConflictException() {
        super("Ya existe una sala con ese nombre.");
    }
}
