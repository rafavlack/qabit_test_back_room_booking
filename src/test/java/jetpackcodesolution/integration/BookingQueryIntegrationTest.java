package jetpackcodesolution.integration;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jetpackcodesolution.RoomBookingApplication;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import jetpackcodesolution.infrastructure.persistence.entity.RoomEntity;
import jetpackcodesolution.infrastructure.persistence.repository.BookingJpaRepository;
import jetpackcodesolution.infrastructure.persistence.repository.RoomJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
class BookingQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private BookingJpaRepository bookingJpaRepository;

    private RoomEntity roomA;
    private RoomEntity roomB;

    @BeforeEach
    void setUp() {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        roomA = roomJpaRepository.save(new RoomEntity(null, "Sala Consulta A", 8, createdAt));
        roomB = roomJpaRepository.save(new RoomEntity(null, "Sala Consulta B", 4, createdAt));
    }

    private BookingEntity booking(
            RoomEntity room, String responsible, String start, String end, Instant cancelledAt) {
        return bookingJpaRepository.save(new BookingEntity(
                null,
                room,
                responsible,
                Instant.parse(start),
                Instant.parse(end),
                Instant.parse("2026-01-01T00:00:00Z"),
                cancelledAt));
    }

    private BookingEntity booking(RoomEntity room, String responsible, String start, String end) {
        return booking(room, responsible, start, end, null);
    }

    @Test
    void listReturnsPageOrderedByStartAndIdWithoutToken() throws Exception {
        BookingEntity first = booking(roomB, "ana", "2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z");
        BookingEntity second = booking(roomA, "luis", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        BookingEntity third = booking(roomA, "eva", "2026-03-10T09:00:00Z", "2026-03-10T11:00:00Z");

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].responsible").value("luis"))
                .andExpect(jsonPath("$.content[1].id").value(third.getId()))
                .andExpect(jsonPath("$.content[2].id").value(first.getId()))
                .andExpect(jsonPath("$.content[0].roomId").value(roomA.getId()))
                .andExpect(jsonPath("$.content[0].cancelledAt").value(nullValue()));
    }

    @Test
    void listPaginatesWithRequestedSize() throws Exception {
        booking(roomA, "ana", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomA, "luis", "2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z");
        booking(roomA, "eva", "2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z");

        mockMvc.perform(get("/api/bookings").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].responsible").value("ana"))
                .andExpect(jsonPath("$.content[1].responsible").value("luis"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/bookings").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].responsible").value("eva"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void listFiltersByRoom() throws Exception {
        booking(roomA, "ana", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomB, "luis", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomA, "eva", "2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z");

        mockMvc.perform(get("/api/bookings").param("roomId", roomB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].responsible").value("luis"))
                .andExpect(jsonPath("$.content[0].roomId").value(roomB.getId()));
    }

    @Test
    void listIncludesBookingThatCrossesMidnightOnBothDays() throws Exception {
        booking(roomA, "nocturno", "2026-03-10T23:00:00Z", "2026-03-11T01:00:00Z");
        booking(roomA, "madrugada", "2026-03-11T00:00:00Z", "2026-03-11T01:00:00Z");
        booking(roomA, "hasta-medianoche", "2026-03-10T22:00:00Z", "2026-03-11T00:00:00Z");

        mockMvc.perform(get("/api/bookings").param("date", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].responsible").value("hasta-medianoche"))
                .andExpect(jsonPath("$.content[1].responsible").value("nocturno"));

        mockMvc.perform(get("/api/bookings").param("date", "2026-03-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].responsible").value("nocturno"))
                .andExpect(jsonPath("$.content[1].responsible").value("madrugada"));

        mockMvc.perform(get("/api/bookings").param("date", "2026-03-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listExcludesCancelledBookingsByDefaultAndIncludesThemOnDemand() throws Exception {
        booking(roomA, "activa", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomA, "cancelada", "2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z",
                Instant.parse("2026-03-01T00:00:00Z"));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].responsible").value("activa"));

        mockMvc.perform(get("/api/bookings").param("includeCancelled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].responsible").value("activa"))
                .andExpect(jsonPath("$.content[1].responsible").value("cancelada"))
                .andExpect(jsonPath("$.content[1].cancelledAt").isNotEmpty());
    }

    @Test
    void listCombinesRoomAndDateFilters() throws Exception {
        booking(roomA, "ana", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomB, "luis", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");
        booking(roomA, "eva", "2026-03-11T09:00:00Z", "2026-03-11T10:00:00Z");

        mockMvc.perform(get("/api/bookings")
                        .param("roomId", roomA.getId().toString())
                        .param("date", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].responsible").value("ana"));
    }

    @Test
    void listIgnoresClientSort() throws Exception {
        booking(roomA, "ana", "2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z");
        booking(roomA, "luis", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");

        mockMvc.perform(get("/api/bookings").param("sort", "responsible,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].responsible").value("luis"))
                .andExpect(jsonPath("$.content[1].responsible").value("ana"));
    }

    @Test
    void listRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/bookings").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetros de paginación no válidos"))
                .andExpect(jsonPath("$.detail").value("El parámetro 'page' no puede ser negativo."));

        mockMvc.perform(get("/api/bookings").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro 'size' debe estar entre 1 y 100."));

        mockMvc.perform(get("/api/bookings").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro 'size' debe estar entre 1 y 100."));
    }

    @Test
    void listRejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/bookings").param("date", "2026-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro no válido"));

        mockMvc.perform(get("/api/bookings").param("roomId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro 'roomId' no tiene un valor válido."));
    }

    @Test
    void findByIdReturnsBookingWithoutToken() throws Exception {
        BookingEntity booking = booking(
                roomA, "ana", "2026-03-10T09:00:00Z", "2026-03-10T10:00:00Z");

        mockMvc.perform(get("/api/bookings/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.roomId").value(roomA.getId()))
                .andExpect(jsonPath("$.responsible").value("ana"))
                .andExpect(jsonPath("$.startTime").value("2026-03-10T09:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-03-10T10:00:00Z"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
    }

    @Test
    void findByIdReturnsNotFoundWhenBookingDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/bookings/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("No existe una reserva con el identificador 9999."));
    }

    @Test
    void findByIdReturnsBadRequestWhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/bookings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro no válido"))
                .andExpect(jsonPath("$.detail")
                        .value("El parámetro 'id' no tiene un valor válido."));
    }
}
