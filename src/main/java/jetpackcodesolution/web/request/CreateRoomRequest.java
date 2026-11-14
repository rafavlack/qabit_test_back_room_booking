package jetpackcodesolution.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "El nombre de la sala es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,
        @Positive(message = "La capacidad debe ser mayor que cero")
        int capacity) {
}
