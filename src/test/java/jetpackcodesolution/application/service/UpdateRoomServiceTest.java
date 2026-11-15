package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import jetpackcodesolution.domain.exception.RoomNameConflictException;
import jetpackcodesolution.domain.exception.RoomNotFoundException;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateRoomServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private RoomRepository roomRepository;

    private UpdateRoomService service;

    @BeforeEach
    void setUp() {
        service = new UpdateRoomService(roomRepository);
    }

    @Test
    void updatesEditableFieldsKeepingCreationInstant() {
        when(roomRepository.findById(7L))
                .thenReturn(Optional.of(new Room(7L, "Sala Norte", 10, CREATED_AT)));
        when(roomRepository.existsByNameExcludingId("Sala Sur", 7L)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room result = service.updateRoom(7L, "  Sala Sur  ", 20);

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(7L);
        assertThat(saved.getValue().name()).isEqualTo("Sala Sur");
        assertThat(saved.getValue().capacity()).isEqualTo(20);
        assertThat(saved.getValue().createdAt()).isEqualTo(CREATED_AT);
        assertThat(result).isEqualTo(saved.getValue());
    }

    @Test
    void keepingItsOwnNameIsNotAConflict() {
        when(roomRepository.findById(7L))
                .thenReturn(Optional.of(new Room(7L, "Sala Norte", 10, CREATED_AT)));
        when(roomRepository.existsByNameExcludingId("Sala Norte", 7L)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room result = service.updateRoom(7L, "Sala Norte", 15);

        assertThat(result.name()).isEqualTo("Sala Norte");
        assertThat(result.capacity()).isEqualTo(15);
    }

    @Test
    void throwsWhenRoomDoesNotExist() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoom(99L, "Sala Norte", 10))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("No existe una sala con el identificador 99.");

        verify(roomRepository, never()).existsByNameExcludingId(any(), any());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void rejectsNameAlreadyUsedByAnotherRoom() {
        when(roomRepository.findById(7L))
                .thenReturn(Optional.of(new Room(7L, "Sala Norte", 10, CREATED_AT)));
        when(roomRepository.existsByNameExcludingId("Sala Ocupada", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateRoom(7L, "Sala Ocupada", 10))
                .isInstanceOf(RoomNameConflictException.class)
                .hasMessage("Ya existe una sala con ese nombre.");

        verify(roomRepository, never()).save(any());
    }
}
