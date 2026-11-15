package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import jetpackcodesolution.RoomBookingApplication;
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
class ConcurrentBookingTest {

    private static final int THREADS = 8;
    private static final Instant SLOT_START =
            Instant.now().plus(14, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    private static final Instant SLOT_END = SLOT_START.plus(1, ChronoUnit.HOURS);

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
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        String roomResponse = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Concurrida\",\"capacity\":10}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        roomId = objectMapper.readTree(roomResponse).get("id").asLong();
    }

    @Test
    void onlyOneOfEightConcurrentRequestsForTheSameSlotSucceeds() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return postBooking();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
            assertThat(statuses).filteredOn(status -> status == 409).hasSize(THREADS - 1);
            assertThat(bookingJpaRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int postBooking() throws Exception {
        return mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roomId\":" + roomId
                                + ",\"responsible\":\"ana\""
                                + ",\"startTime\":\"" + SLOT_START + "\""
                                + ",\"endTime\":\"" + SLOT_END + "\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
