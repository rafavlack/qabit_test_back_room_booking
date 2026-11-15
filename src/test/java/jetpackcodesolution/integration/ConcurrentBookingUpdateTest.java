package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
class ConcurrentBookingUpdateTest {

    private static final int BOOKINGS = 8;
    private static final Instant SLOT_START =
            Instant.now().plus(14, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    private static final Instant SLOT_END = SLOT_START.plus(1, ChronoUnit.HOURS);
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
    private long targetRoomId;
    private final List<Long> bookingIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        targetRoomId = roomJpaRepository.save(
                new RoomEntity(null, "Sala Destino", 10, CREATED_AT)).getId();

        bookingIds.clear();
        for (int i = 0; i < BOOKINGS; i++) {
            RoomEntity source = roomJpaRepository.save(
                    new RoomEntity(null, "Sala Origen " + i, 4, CREATED_AT));
            BookingEntity booking = bookingJpaRepository.save(new BookingEntity(
                    null, source, "origen-" + i,
                    SLOT_START.plus(i, ChronoUnit.DAYS),
                    SLOT_END.plus(i, ChronoUnit.DAYS),
                    CREATED_AT, null));
            bookingIds.add(booking.getId());
        }
    }

    @Test
    void onlyOneOfEightConcurrentMovesIntoTheSameSlotSucceeds() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(BOOKINGS);
        CountDownLatch ready = new CountDownLatch(BOOKINGS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < BOOKINGS; i++) {
                long bookingId = bookingIds.get(i);
                int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return putBooking(bookingId, index);
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
            assertThat(statuses).filteredOn(status -> status == 409).hasSize(BOOKINGS - 1);
            assertThat(bookingsInTargetSlot()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int putBooking(long bookingId, int index) throws Exception {
        return mockMvc.perform(put("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roomId\":" + targetRoomId
                                + ",\"responsible\":\"mover-" + index + "\""
                                + ",\"startTime\":\"" + SLOT_START + "\""
                                + ",\"endTime\":\"" + SLOT_END + "\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private long bookingsInTargetSlot() {
        return bookingJpaRepository.findAll().stream()
                .filter(booking -> booking.getRoom().getId() == targetRoomId)
                .filter(booking -> booking.getStartTime().equals(SLOT_START))
                .count();
    }
}
