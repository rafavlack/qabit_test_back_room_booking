package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.CreateBookingUseCase;
import jetpackcodesolution.domain.exception.BookingConflictException;
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
public class CreateBookingService implements CreateBookingUseCase {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public CreateBookingService(
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            Clock clock) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public Booking createBooking(
            Long roomId,
            String responsible,
            Instant startTime,
            Instant endTime) {
        Instant now = clock.instant();
        validateInterval(startTime, endTime, now);

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (bookingRepository.existsOverlapping(roomId, startTime, endTime)) {
            throw new BookingConflictException();
        }

        return bookingRepository.save(new Booking(
                null, roomId, responsible.trim(), startTime, endTime, now, null));
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
