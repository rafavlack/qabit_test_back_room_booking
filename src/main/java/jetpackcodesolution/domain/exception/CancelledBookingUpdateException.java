package jetpackcodesolution.domain.exception;

public class CancelledBookingUpdateException extends RuntimeException {

    public CancelledBookingUpdateException(Long id) {
        super("No se puede modificar la reserva " + id + " porque está cancelada.");
    }
}
