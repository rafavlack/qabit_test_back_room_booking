package jetpackcodesolution.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jetpackcodesolution.application.port.input.CreateBookingUseCase;
import jetpackcodesolution.domain.model.Booking;
import jetpackcodesolution.web.mapper.BookingWebMapper;
import jetpackcodesolution.web.request.CreateBookingRequest;
import jetpackcodesolution.web.response.BookingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final BookingWebMapper bookingWebMapper;

    public BookingController(
            CreateBookingUseCase createBookingUseCase,
            BookingWebMapper bookingWebMapper) {
        this.createBookingUseCase = createBookingUseCase;
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
}
