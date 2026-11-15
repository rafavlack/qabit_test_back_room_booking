package jetpackcodesolution.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateBookingRequest(
        @NotNull(message = "La sala es obligatoria")
        @Positive(message = "El identificador de la sala debe ser positivo")
        Long roomId,
        @NotBlank(message = "El responsable es obligatorio")
        @Size(max = 100, message = "El responsable no puede superar los 100 caracteres")
        String responsible,
        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant startTime,
        @NotNull(message = "La fecha de fin es obligatoria")
        Instant endTime) {
}
