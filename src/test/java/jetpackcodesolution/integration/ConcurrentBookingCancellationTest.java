package jetpackcodesolution.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
class ConcurrentBookingCancellationTest {

    private static final int ATTEMPTS = 8;
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
    private long bookingId;

    @BeforeEach
    void setUp() throws Exception {
        bookingJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();

        RoomEntity room = roomJpaRepository.save(
                new RoomEntity(null, "Sala Cancelación Concurrente", 6, CREATED_AT));
        bookingId = bookingJpaRepository.save(new BookingEntity(
                null, room, "luis", SLOT_START, SLOT_END, CREATED_AT, null)).getId();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    @Test
    void concurrentCancellationsAreAllNoContentAndKeepASingleInstant() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS);
        CountDownLatch ready = new CountDownLatch(ATTEMPTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < ATTEMPTS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(delete("/api/bookings/" + bookingId)
                                    .header("Authorization", "Bearer " + adminToken))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(204);
        } finally {
            executor.shutdownNow();
        }

        assertThat(bookingJpaRepository.count()).isEqualTo(1);
        BookingEntity cancelled = bookingJpaRepository.findById(bookingId).orElseThrow();
        assertThat(cancelled.getCancelledAt()).isNotNull();

        Instant firstCancellation = cancelled.getCancelledAt();

        mockMvc.perform(delete("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(bookingJpaRepository.findById(bookingId).orElseThrow().getCancelledAt())
                .isEqualTo(firstCancellation);
    }
}
