package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.ChatEsperaService;
import co.eci.c15.chat.application.ChatNotifier;
import co.eci.c15.chat.application.SalaEsperaConsulta;
import co.eci.c15.chat.infrastructure.persistence.ChatSalaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatSalaControllerTest {

    private MockMvc mvc;
    private final AtomicBoolean enEspera = new AtomicBoolean(true);

    @BeforeEach
    void setUp() {
        SalaEsperaConsulta salas = new SalaEsperaConsulta() {
            @Override
            public boolean enEspera(String salaId) {
                return "s1".equals(salaId) && enEspera.get();
            }

            @Override
            public boolean contieneJugador(String salaId, String userId) {
                return "ana".equals(userId) || "beto".equals(userId);
            }
        };
        ChatNotifier notifier = m -> { };
        mvc = MockMvcBuilders.standaloneSetup(
                new ChatSalaController(new ChatEsperaService(new ChatSalaEnMemoria(), salas, notifier))).build();
    }

    private static String cuerpo(String userId, String texto) {
        return "{\"userId\":\"" + userId + "\",\"texto\":\"" + texto + "\"}";
    }

    @Test
    void enviarDevuelve201ConElMensaje() throws Exception {
        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("ana", "hola")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.salaId").value("s1"))
                .andExpect(jsonPath("$.userId").value("ana"))
                .andExpect(jsonPath("$.texto").value("hola"))
                .andExpect(jsonPath("$.enviadoEn").isNotEmpty());
    }

    @Test
    void elHistorialTraeLosMensajesDeLaSala() throws Exception {
        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("ana", "hola")));
        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("beto", "que mas")));

        mvc.perform(get("/api/salas/s1/chat").param("userId", "beto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].texto").value("hola"))
                .andExpect(jsonPath("$[1].userId").value("beto"));
    }

    @Test
    void mensajeVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("ana", "  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void historialSinUserIdDevuelve400() throws Exception {
        mvc.perform(get("/api/salas/s1/chat")).andExpect(status().isBadRequest());
    }

    @Test
    void jugadorAjenoALaSalaDevuelve403() throws Exception {
        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("intruso", "hola")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/salas/s1/chat").param("userId", "intruso"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fueraDeLaSalaDeEsperaDevuelve409() throws Exception {
        enEspera.set(false);

        mvc.perform(post("/api/salas/s1/chat").contentType(MediaType.APPLICATION_JSON).content(cuerpo("ana", "hola")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(get("/api/salas/s1/chat").param("userId", "ana"))
                .andExpect(status().isConflict());
    }
}
