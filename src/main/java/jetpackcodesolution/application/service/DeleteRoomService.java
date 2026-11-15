package jetpackcodesolution.application.service;

import jetpackcodesolution.application.port.input.DeleteRoomUseCase;
import jetpackcodesolution.domain.exception.RoomHasBookingsException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.repository.BookingRepository;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteRoomService implements DeleteRoomUseCase {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public DeleteRoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    @Override
    public void deleteRoom(Long id) {
        if (roomRepository.findById(id).isEmpty()) {
            throw new RoomNotFoundException(id);
        }
        if (bookingRepository.existsByRoomId(id)) {
            throw new RoomHasBookingsException();
        }
        roomRepository.deleteById(id);
    }
}
