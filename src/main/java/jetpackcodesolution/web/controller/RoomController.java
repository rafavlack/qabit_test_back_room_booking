package jetpackcodesolution.web.controller;

import jetpackcodesolution.application.port.input.CreateRoomUseCase;
import jetpackcodesolution.application.port.input.DeleteRoomUseCase;
import jetpackcodesolution.application.port.input.FindRoomByIdUseCase;
import jetpackcodesolution.application.port.input.ListRoomsUseCase;
import jetpackcodesolution.application.port.input.UpdateRoomUseCase;
import jetpackcodesolution.domain.model.Room;
import jetpackcodesolution.web.mapper.RoomWebMapper;
import jetpackcodesolution.web.request.CreateRoomRequest;
import jetpackcodesolution.web.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final ListRoomsUseCase listRoomsUseCase;
    private final FindRoomByIdUseCase findRoomByIdUseCase;
    private final UpdateRoomUseCase updateRoomUseCase;
    private final DeleteRoomUseCase deleteRoomUseCase;
    private final RoomWebMapper roomWebMapper;

    public RoomController(
            CreateRoomUseCase createRoomUseCase,
            ListRoomsUseCase listRoomsUseCase,
            FindRoomByIdUseCase findRoomByIdUseCase,
            UpdateRoomUseCase updateRoomUseCase,
            DeleteRoomUseCase deleteRoomUseCase,
            RoomWebMapper roomWebMapper) {
        this.createRoomUseCase = createRoomUseCase;
        this.listRoomsUseCase = listRoomsUseCase;
        this.findRoomByIdUseCase = findRoomByIdUseCase;
        this.updateRoomUseCase = updateRoomUseCase;
        this.deleteRoomUseCase = deleteRoomUseCase;
        this.roomWebMapper = roomWebMapper;
    }

    @Operation(summary = "Crear una sala")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sala creada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada no válidos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "409", description = "Ya existe una sala con ese nombre")})
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room room = createRoomUseCase.createRoom(request.name(), request.capacity());
        RoomResponse response = roomWebMapper.toResponse(room);
        return ResponseEntity.created(URI.create("/api/rooms/" + response.id())).body(response);
    }

    @Operation(summary = "Listar salas")
    @ApiResponse(responseCode = "200", description = "Lista de salas ordenadas por identificador")
    @GetMapping
    public List<RoomResponse> list() {
        return roomWebMapper.toResponseList(listRoomsUseCase.listRooms());
    }

    @Operation(summary = "Consultar una sala por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sala encontrada"),
        @ApiResponse(responseCode = "400", description = "El identificador no es válido"),
        @ApiResponse(responseCode = "404", description = "Sala no encontrada")})
    @GetMapping("/{id}")
    public RoomResponse findById(
            @Parameter(description = "Identificador de la sala", required = true, example = "1")
            @PathVariable Long id) {
        return roomWebMapper.toResponse(findRoomByIdUseCase.findById(id));
    }

    @Operation(summary = "Actualizar los datos editables de una sala")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sala actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada no válidos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "404", description = "Sala no encontrada"),
        @ApiResponse(responseCode = "409", description = "Ya existe una sala con ese nombre")})
    @PutMapping("/{id}")
    public RoomResponse update(
            @Parameter(description = "Identificador de la sala", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        Room room = updateRoomUseCase.updateRoom(id, request.name(), request.capacity());
        return roomWebMapper.toResponse(room);
    }

    @Operation(summary = "Eliminar una sala")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sala eliminada"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "404", description = "Sala no encontrada"),
        @ApiResponse(responseCode = "409", description = "La sala tiene reservas asociadas")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador de la sala", required = true, example = "1")
            @PathVariable Long id) {
        deleteRoomUseCase.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
