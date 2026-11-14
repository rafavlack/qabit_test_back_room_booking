package jetpackcodesolution.integration;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jetpackcodesolution.RoomBookingApplication;
import jetpackcodesolution.infrastructure.persistence.repository.RoomJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        roomJpaRepository.deleteAll();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    private String createRoom(String name, int capacity) throws Exception {
        return mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"capacity\":" + capacity + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void listRoomsReturnsEmptyListWhenNoRoomsExist() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listRoomsReturnsAllRoomsOrderedById() throws Exception {
        createRoom("Sala Uno", 10);
        createRoom("Sala Dos", 20);

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Sala Uno"))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[1].name").value("Sala Dos"))
                .andExpect(jsonPath("$[1].capacity").value(20));
    }

    @Test
    void findRoomByIdReturnsRoom() throws Exception {
        String created = createRoom("Sala Atlas", 12);
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/rooms/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Sala Atlas"))
                .andExpect(jsonPath("$.capacity").value(12))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void findRoomByIdReturnsNotFoundWhenRoomDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/rooms/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe una sala con el identificador 9999."));
    }

    @Test
    void findRoomByIdReturnsBadRequestForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/rooms/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro no válido"));
    }

    @Test
    void createRoomReturnsCreatedWithLocationHeader() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\",\"capacity\":8}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/rooms/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Sala Norte"))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createRoomTrimsSurroundingWhitespace() throws Exception {
        String response = createRoom("  Sala Neptuno  ", 5);

        org.assertj.core.api.Assertions.assertThat(
                        objectMapper.readTree(response).get("name").asText())
                .isEqualTo("Sala Neptuno");
    }

    @Test
    void createRoomWithDuplicateNameReturnsConflict() throws Exception {
        createRoom("Sala Duplicada", 10);

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Duplicada\",\"capacity\":20}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de sala"))
                .andExpect(jsonPath("$.detail").value("Ya existe una sala con ese nombre."));
    }

    @Test
    void createRoomWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\",\"capacity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validación fallida"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void createRoomWithMissingNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"capacity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void createRoomWithZeroCapacityReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Cero\",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("capacity"));
    }

    @Test
    void createRoomWithNegativeCapacityReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Negativa\",\"capacity\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("capacity"));
    }

    @Test
    void createRoomWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Sala Sin Token\",\"capacity\":5}"))
                .andExpect(status().isUnauthorized());
    }
}
