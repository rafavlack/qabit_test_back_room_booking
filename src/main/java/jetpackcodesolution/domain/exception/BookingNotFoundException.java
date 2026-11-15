package jetpackcodesolution.domain.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("No existe una reserva con el identificador " + id + ".");
    }
}
