package jetpackcodesolution.domain.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long id) {
        super("No existe una sala con el identificador " + id + ".");
    }
}
