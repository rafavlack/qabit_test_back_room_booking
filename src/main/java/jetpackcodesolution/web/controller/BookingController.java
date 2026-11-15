package jetpackcodesolution.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jetpackcodesolution.application.port.input.CreateBookingUseCase;
import jetpackcodesolution.application.port.input.CancelBookingUseCase;
import jetpackcodesolution.application.port.input.FindBookingByIdUseCase;
import jetpackcodesolution.application.port.input.SearchBookingsUseCase;
import jetpackcodesolution.application.port.input.UpdateBookingUseCase;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.domain.model.BookingSearchResult;
import jetpackcodesolution.web.mapper.BookingWebMapper;
import jetpackcodesolution.web.request.CreateBookingRequest;
import jetpackcodesolution.web.response.BookingResponse;
import jetpackcodesolution.web.response.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final SearchBookingsUseCase searchBookingsUseCase;
    private final FindBookingByIdUseCase findBookingByIdUseCase;
    private final UpdateBookingUseCase updateBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final BookingWebMapper bookingWebMapper;

    public BookingController(
            CreateBookingUseCase createBookingUseCase,
            SearchBookingsUseCase searchBookingsUseCase,
            FindBookingByIdUseCase findBookingByIdUseCase,
            UpdateBookingUseCase updateBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            BookingWebMapper bookingWebMapper) {
        this.createBookingUseCase = createBookingUseCase;
        this.searchBookingsUseCase = searchBookingsUseCase;
        this.findBookingByIdUseCase = findBookingByIdUseCase;
        this.updateBookingUseCase = updateBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.bookingWebMapper = bookingWebMapper;
    }

    @Operation(summary = "Crear una reserva")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva creada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada no válidos o intervalo incorrecto"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "404", description = "Sala no encontrada"),
        @ApiResponse(responseCode = "409", description = "La reserva se solapa con otra de la misma sala")})
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        Booking booking = createBookingUseCase.createBooking(
                request.roomId(), request.responsible(), request.startTime(), request.endTime());
        BookingResponse response = bookingWebMapper.toResponse(booking);
        return ResponseEntity.created(URI.create("/api/bookings/" + response.id())).body(response);
    }

    @Operation(summary = "Listar reservas con filtros y paginación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de reservas ordenada por inicio e identificador"),
        @ApiResponse(responseCode = "400", description = "Filtros o parámetros de paginación no válidos")})
    @GetMapping
    public PageResponse<BookingResponse> list(
            @Parameter(description = "Identificador de la sala")
            @RequestParam(required = false) Long roomId,
            @Parameter(description = "Día a consultar en formato YYYY-MM-DD", example = "2026-12-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Incluir reservas canceladas")
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @Parameter(description = "Número de página, empezando en 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página, entre 1 y 100")
            @RequestParam(defaultValue = "20") int size) {
        BookingSearchResult result = searchBookingsUseCase.searchBookings(
                roomId, date, includeCancelled, page, size);
        return PageResponse.of(
                bookingWebMapper.toResponseList(result.bookings()),
                page, size, result.totalElements());
    }

    @Operation(summary = "Consultar una reserva por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
        @ApiResponse(responseCode = "400", description = "El identificador no es válido"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")})
    @GetMapping("/{id}")
    public BookingResponse findById(
            @Parameter(description = "Identificador de la reserva", required = true, example = "1")
            @PathVariable Long id) {
        return bookingWebMapper.toResponse(findBookingByIdUseCase.findById(id));
    }

    @Operation(summary = "Actualizar los datos editables de una reserva")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada no válidos o intervalo incorrecto"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "404", description = "Reserva o sala no encontrada"),
        @ApiResponse(responseCode = "409", description = "La reserva está cancelada o se solapa con otra")})
    @PutMapping("/{id}")
    public BookingResponse update(
            @Parameter(description = "Identificador de la reserva", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = updateBookingUseCase.updateBooking(
                id, request.roomId(), request.responsible(),
                request.startTime(), request.endTime());
        return bookingWebMapper.toResponse(booking);
    }

    @Operation(summary = "Cancelar una reserva")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reserva cancelada o ya estaba cancelada"),
        @ApiResponse(responseCode = "400", description = "El identificador no es válido"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Identificador de la reserva", required = true, example = "1")
            @PathVariable Long id) {
        cancelBookingUseCase.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
