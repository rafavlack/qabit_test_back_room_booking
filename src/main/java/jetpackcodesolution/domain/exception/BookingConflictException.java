package jetpackcodesolution.domain.exception;

public class BookingConflictException extends RuntimeException {

    public BookingConflictException() {
        super("Ya existe una reserva que se solapa en esa sala.");
    }
}
