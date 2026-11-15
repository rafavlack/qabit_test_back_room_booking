package jetpackcodesolution.domain.exception;

public class RoomHasBookingsException extends RuntimeException {

    public RoomHasBookingsException() {
        super("No se puede eliminar la sala porque tiene reservas asociadas.");
    }
}
