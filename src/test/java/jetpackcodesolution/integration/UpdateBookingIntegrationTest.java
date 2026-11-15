package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.temporal.ChronoUnit;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
class UpdateBookingIntegrationTest {

    private static final Instant SLOT_START =
            Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    private static final Instant SLOT_END = SLOT_START.plus(2, ChronoUnit.HOURS);
    private static final Instant CREATED_AT = Instant.parse("2025-12-01T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private BookingJpaRepository bookingJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private RoomEntity roomA;
    private RoomEntity roomB;

    @BeforeEach
    void setUp() throws Exception {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        roomA = roomJpaRepository.save(new RoomEntity(null, "Sala Update A", 8, CREATED_AT));
        roomB = roomJpaRepository.save(new RoomEntity(null, "Sala Update B", 4, CREATED_AT));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    private BookingEntity createBooking(
            RoomEntity room, String responsible, Instant start, Instant end) {
        return bookingJpaRepository.save(new BookingEntity(
                null, room, responsible, start, end, CREATED_AT, null));
    }

    private String body(Long roomId, String responsible, Instant start, Instant end) {
        return String.format(
                "{\"roomId\":%d,\"responsible\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                roomId, responsible, start, end);
    }

    @Test
    void updateReturnsUpdatedBookingAndPreservesCreationTime() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomB.getId(), "  ana  ",
                                SLOT_START.plus(1, ChronoUnit.DAYS),
                                SLOT_END.plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.roomId").value(roomB.getId()))
                .andExpect(jsonPath("$.responsible").value("ana"))
                .andExpect(jsonPath("$.startTime")
                        .value(SLOT_START.plus(1, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$.endTime")
                        .value(SLOT_END.plus(1, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));

        BookingEntity updated = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(updated.getRoom().getId()).isEqualTo(roomB.getId());
        assertThat(updated.getResponsible()).isEqualTo("ana");
        assertThat(updated.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void updateKeepsItsOwnSlot() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "luis", SLOT_START, SLOT_END)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsConflictWhenItOverlapsAnotherBookingInTheSameRoom() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);
        createBooking(roomA, "eva",
                SLOT_START.plus(2, ChronoUnit.HOURS), SLOT_END.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "luis",
                                SLOT_START.plus(2, ChronoUnit.HOURS),
                                SLOT_END.plus(2, ChronoUnit.HOURS))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de reserva"))
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe una reserva que se solapa en esa sala."));

        BookingEntity unchanged = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getStartTime()).isEqualTo(SLOT_START);
        assertThat(unchanged.getEndTime()).isEqualTo(SLOT_END);
    }

    @Test
    void updateReturnsConflictWhenTheTargetRoomSlotIsTaken() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);
        createBooking(roomB, "eva", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomB.getId(), "luis", SLOT_START, SLOT_END)))
                .andExpect(status().isConflict());

        BookingEntity unchanged = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getRoom().getId()).isEqualTo(roomA.getId());
    }

    @Test
    void updateReturnsNotFoundWhenBookingDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/bookings/9999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("No existe una reserva con el identificador 9999."));
    }

    @Test
    void updateReturnsNotFoundWhenRoomDoesNotExist() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(9999L, "luis", SLOT_START, SLOT_END)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("No existe una sala con el identificador 9999."));
    }

    @Test
    void updateReturnsBadRequestWhenIntervalIsInvalid() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "luis", SLOT_START, SLOT_START)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Reserva no válida"))
                .andExpect(jsonPath("$.detail")
                        .value("La fecha de fin debe ser posterior a la fecha de inicio."));
    }

    @Test
    void updateReturnsBadRequestWhenStartIsNotInTheFuture() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "luis", past, past.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La reserva debe comenzar posterior al momento actual."));
    }

    @Test
    void updateValidatesIntervalBeforeCheckingTheBooking() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        mockMvc.perform(put("/api/bookings/9999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "ana", past, past.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Reserva no válida"));
    }

    @Test
    void updateReturnsConflictWhenBookingIsCancelled() throws Exception {
        BookingEntity cancelled = bookingJpaRepository.save(new BookingEntity(
                null, roomA, "luis", SLOT_START, SLOT_END, CREATED_AT, Instant.now()));

        mockMvc.perform(put("/api/bookings/" + cancelled.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de reserva"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede modificar la reserva " + cancelled.getId()
                                + " porque está cancelada."));

        BookingEntity unchanged =
                bookingJpaRepository.findById(cancelled.getId()).orElseThrow();
        assertThat(unchanged.getResponsible()).isEqualTo("luis");
        assertThat(unchanged.getCancelledAt()).isNotNull();
    }

    @Test
    void updateWithoutTokenReturnsUnauthorized() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .contentType(APPLICATION_JSON)
                        .content(body(roomB.getId(), "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isUnauthorized());

        BookingEntity unchanged = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getRoom().getId()).isEqualTo(roomA.getId());
        assertThat(unchanged.getResponsible()).isEqualTo("luis");
    }

    @Test
    void updateReturnsBadRequestWhenResponsibleIsMissing() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roomId\":" + roomA.getId()
                                + ",\"startTime\":\"" + SLOT_START
                                + "\",\"endTime\":\"" + SLOT_END + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validación fallida"))
                .andExpect(jsonPath("$.errors[0].field").value("responsible"));
    }

    @Test
    void updateReturnsBadRequestWhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(put("/api/bookings/abc")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro no válido"));
    }

    @Test
    void updatedBookingIsVisibleThroughTheReadEndpoints() throws Exception {
        BookingEntity booking = createBooking(roomA, "luis", SLOT_START, SLOT_END);

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomA.getId(), "ana actualizada",
                                SLOT_START.plus(1, ChronoUnit.DAYS),
                                SLOT_END.plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsible").value("ana actualizada"));
    }
}
