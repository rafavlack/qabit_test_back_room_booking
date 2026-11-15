package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class CancelBookingIntegrationTest {

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
    private RoomEntity room;

    @BeforeEach
    void setUp() throws Exception {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        room = roomJpaRepository.save(new RoomEntity(null, "Sala Cancelación", 6, CREATED_AT));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    private BookingEntity createBooking(Instant start, Instant end) {
        return bookingJpaRepository.save(new BookingEntity(
                null, room, "luis", start, end, CREATED_AT, null));
    }

    private BookingEntity createCancelledBooking(Instant start, Instant end, Instant cancelledAt) {
        return bookingJpaRepository.save(new BookingEntity(
                null, room, "luis", start, end, CREATED_AT, cancelledAt));
    }

    private String bookingBody(Long roomId, String responsible, Instant start, Instant end) {
        return String.format(
                "{\"roomId\":%d,\"responsible\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                roomId, responsible, start, end);
    }

    @Test
    void deleteCancelsTheBookingLogicallyWithoutDeletingTheRow() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);

        mockMvc.perform(delete("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        BookingEntity cancelled = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getRoom().getId()).isEqualTo(room.getId());
        assertThat(cancelled.getResponsible()).isEqualTo("luis");
        assertThat(cancelled.getStartTime()).isEqualTo(SLOT_START);
        assertThat(cancelled.getEndTime()).isEqualTo(SLOT_END);
        assertThat(cancelled.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(bookingJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteIsIdempotentAndKeepsTheFirstCancellationInstant() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);
        String url = "/api/bookings/" + booking.getId();

        mockMvc.perform(delete(url).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Instant firstCancellation =
                bookingJpaRepository.findById(booking.getId()).orElseThrow().getCancelledAt();

        mockMvc.perform(delete(url).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        BookingEntity afterSecondDelete =
                bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(afterSecondDelete.getCancelledAt()).isEqualTo(firstCancellation);
    }

    @Test
    void cancelledBookingIsStillVisibleThroughFindById() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);

        mockMvc.perform(delete("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bookings/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
    }

    @Test
    void deleteReturnsNotFoundWhenBookingDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/bookings/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("No existe una reserva con el identificador 9999."));
    }

    @Test
    void deleteWithoutTokenReturnsUnauthorizedAndKeepsTheBookingActive() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);

        mockMvc.perform(delete("/api/bookings/" + booking.getId()))
                .andExpect(status().isUnauthorized());

        BookingEntity unchanged = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getCancelledAt()).isNull();
    }

    @Test
    void deleteReturnsBadRequestWhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/bookings/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro no válido"))
                .andExpect(jsonPath("$.detail")
                        .value("El parámetro 'id' no tiene un valor válido."));
    }

    @Test
    void cancelledBookingFreesItsSlotForANewBooking() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(bookingBody(room.getId(), "eva", SLOT_START, SLOT_END)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(bookingBody(room.getId(), "eva", SLOT_START, SLOT_END)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsible").value("eva"))
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
    }

    @Test
    void updateReturnsConflictWhenTheBookingWasCancelled() throws Exception {
        BookingEntity booking = createBooking(SLOT_START, SLOT_END);

        mockMvc.perform(delete("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(bookingBody(
                                room.getId(), "ana",
                                SLOT_START.plus(1, ChronoUnit.DAYS),
                                SLOT_END.plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de reserva"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede modificar la reserva " + booking.getId()
                                + " porque está cancelada."));

        BookingEntity unchanged = bookingJpaRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getResponsible()).isEqualTo("luis");
        assertThat(unchanged.getStartTime()).isEqualTo(SLOT_START);
        assertThat(unchanged.getCancelledAt()).isNotNull();
    }

    @Test
    void listExcludesCancelledBookingsByDefaultAndIncludesThemOnDemand() throws Exception {
        BookingEntity active = createBooking(SLOT_START, SLOT_END);
        BookingEntity cancelled = createCancelledBooking(
                SLOT_START.plus(3, ChronoUnit.HOURS), SLOT_END.plus(3, ChronoUnit.HOURS),
                Instant.parse("2025-12-20T00:00:00Z"));

        mockMvc.perform(delete("/api/bookings/" + cancelled.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(active.getId()));

        mockMvc.perform(get("/api/bookings").param("includeCancelled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[1].id").value(cancelled.getId()))
                .andExpect(jsonPath("$.content[1].cancelledAt")
                        .value("2025-12-20T00:00:00Z"));
    }
}
