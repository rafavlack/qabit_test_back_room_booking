package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.UpdateBookingUseCase;
import jetpackcodesolution.domain.exception.BookingConflictException;
import jetpackcodesolution.domain.exception.BookingNotFoundException;
import jetpackcodesolution.domain.exception.CancelledBookingUpdateException;
import jetpackcodesolution.domain.exception.InvalidBookingTimeException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UpdateBookingService implements UpdateBookingUseCase {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public UpdateBookingService(
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            Clock clock) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public Booking updateBooking(
            Long id,
            Long roomId,
            String responsible,
            Instant startTime,
            Instant endTime) {
        Instant now = clock.instant();
        validateInterval(startTime, endTime, now);

        Booking existing = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (existing.hasCancellation()) {
            throw new CancelledBookingUpdateException(id);
        }

        if (bookingRepository.existsOverlappingExcluding(
                roomId, startTime, endTime, id)) {
            throw new BookingConflictException();
        }

        return bookingRepository.save(existing.updateBooking(
                roomId, responsible.trim(), startTime, endTime));
    }

    private void validateInterval(Instant startTime, Instant endTime, Instant now) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidBookingTimeException(
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (!startTime.isAfter(now)) {
            throw new InvalidBookingTimeException("La reserva debe comenzar posterior al momento actual.");
        }
    }
}
