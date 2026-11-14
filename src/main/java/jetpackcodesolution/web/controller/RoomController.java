package jetpackcodesolution.web.controller;

import jetpackcodesolution.application.port.input.CreateRoomUseCase;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.web.mapper.RoomWebMapper;
import jetpackcodesolution.web.request.CreateRoomRequest;
import jetpackcodesolution.web.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final RoomWebMapper roomWebMapper;

    public RoomController(CreateRoomUseCase createRoomUseCase, RoomWebMapper roomWebMapper) {
        this.createRoomUseCase = createRoomUseCase;
        this.roomWebMapper = roomWebMapper;
    }

    @Operation(summary = "Crear una sala")
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room room = createRoomUseCase.createRoom(request.name(), request.capacity());
        RoomResponse response = roomWebMapper.toResponse(room);
        return ResponseEntity.created(URI.create("/api/rooms/" + response.id())).body(response);
    }
}
