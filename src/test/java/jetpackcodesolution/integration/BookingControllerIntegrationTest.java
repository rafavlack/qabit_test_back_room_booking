package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class BookingControllerIntegrationTest {

    private static final Instant SLOT_START =
            Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    private static final Instant SLOT_END = SLOT_START.plus(2, ChronoUnit.HOURS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private BookingJpaRepository bookingJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private long roomId;

    @BeforeEach
    void setUp() throws Exception {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        roomId = createRoom("Sala Reservas", 8);
    }

    private long createRoom(String name, int capacity) throws Exception {
        String response = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"capacity\":" + capacity + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String body(long room, String responsible, Instant start, Instant end) {
        return String.format(
                "{\"roomId\":%d,\"responsible\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                room, responsible, start, end);
    }

    private String createBooking(long room, String responsible, Instant start, Instant end)
            throws Exception {
        String response = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(room, responsible, start, end)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void createCancelledBooking(long room, Instant start, Instant end) {
        RoomEntity roomEntity = roomJpaRepository.findById(room).orElseThrow();
        bookingJpaRepository.save(new BookingEntity(
                null, roomEntity, "ana", start, end, Instant.now(), Instant.now()));
    }

    @Test
    void createBookingReturnsCreatedWithLocationAndTrimmedResponsible() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "  ana  ", SLOT_START, SLOT_END)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/bookings/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.responsible").value("ana"))
                .andExpect(jsonPath("$.startTime").value(SLOT_START.toString()))
                .andExpect(jsonPath("$.endTime").value(SLOT_END.toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));

        assertThat(bookingJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void createBookingReturnsConflictWhenOverlapping() throws Exception {
        createBooking(roomId, "ana", SLOT_START, SLOT_END);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "luis", SLOT_START.plus(30, ChronoUnit.MINUTES), SLOT_END)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de reserva"))
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe una reserva que se solapa en esa sala."));

        assertThat(bookingJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void createBookingAllowsConsecutiveInterval() throws Exception {
        createBooking(roomId, "ana", SLOT_START, SLOT_END);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "luis", SLOT_END, SLOT_END.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isCreated());

        assertThat(bookingJpaRepository.count()).isEqualTo(2);
    }

    @Test
    void createBookingAllowsSameSlotInAnotherRoom() throws Exception {
        long otherRoomId = createRoom("Sala Alterna", 4);
        createBooking(roomId, "ana", SLOT_START, SLOT_END);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(otherRoomId, "luis", SLOT_START, SLOT_END)))
                .andExpect(status().isCreated());
    }

    @Test
    void createBookingIgnoresCancelledBookingsWhenCheckingOverlaps() throws Exception {
        createCancelledBooking(roomId, SLOT_START, SLOT_END);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isCreated());
    }

    @Test
    void createBookingReturnsBadRequestWhenEndIsNotAfterStart() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "ana", SLOT_START, SLOT_START)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Reserva no válida"))
                .andExpect(jsonPath("$.detail")
                        .value("La fecha de fin debe ser posterior a la fecha de inicio."));
    }

    @Test
    void createBookingReturnsBadRequestWhenStartIsNotInTheFuture() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "ana", past, past.plus(1, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Reserva no válida"))
                .andExpect(jsonPath("$.detail").value("La reserva debe comenzar posterior al momento actual."));
    }

    @Test
    void createBookingReturnsNotFoundWhenRoomDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(9999L, "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("No existe una sala con el identificador 9999."));
    }

    @Test
    void invalidIntervalIsRejectedBeforeLookingUpTheRoom() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(9999L, "ana", SLOT_START, SLOT_START.minus(1, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Reserva no válida"));
    }

    @Test
    void createBookingReturnsBadRequestWhenResponsibleIsMissing() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roomId\":" + roomId
                                + ",\"startTime\":\"" + SLOT_START
                                + "\",\"endTime\":\"" + SLOT_END + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validación fallida"))
                .andExpect(jsonPath("$.errors[0].field").value("responsible"));
    }

    @Test
    void createBookingReturnsBadRequestWhenRoomIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"responsible\":\"ana\",\"startTime\":\"" + SLOT_START
                                + "\",\"endTime\":\"" + SLOT_END + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));
    }

    @Test
    void createBookingReturnsBadRequestWhenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(body(-1L, "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));
    }

    @Test
    void createBookingReturnsBadRequestWhenTimesAreMissing() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roomId\":" + roomId + ",\"responsible\":\"ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void createBookingWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(APPLICATION_JSON)
                        .content(body(roomId, "ana", SLOT_START, SLOT_END)))
                .andExpect(status().isUnauthorized());

        assertThat(bookingJpaRepository.count()).isZero();
    }
}
