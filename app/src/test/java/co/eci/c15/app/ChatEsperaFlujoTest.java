package co.eci.c15.app;

import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Los chats contra las salas, los usuarios y los eventos reales (adaptadores incluidos). */
@SpringBootTest
@AutoConfigureMockMvc
class ChatEsperaFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private SalaRepository salas;

    @Autowired
    private ChatRepository historial;

    @Autowired
    private ApplicationEventPublisher eventos;

    @Test
    void losJugadoresDeLaSalaEscribenYLeenElHistorialEnOrden() throws Exception {
        String salaId = salaConJugadores("Chat orden", "ana1", "beto1");
        String ruta = "/api/chat/salas/" + salaId;

        enviar(ruta, "ana1", "  hola  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.canal").value("salas/" + salaId))
                .andExpect(jsonPath("$.userId").value("ana1"))
                .andExpect(jsonPath("$.texto").value("hola"))
                // ISO-8601: el front lo ordena y lo muestra como hora
                .andExpect(jsonPath("$.enviadoEn").isString());
        enviar(ruta, "beto1", "listo?").andExpect(status().isCreated());

        mvc.perform(get(ruta).param("userId", "beto1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].texto").value("hola"))
                .andExpect(jsonPath("$[1].texto").value("listo?"));
    }

    @Test
    void quienNoEstaEnLaSalaNoLeeNiEscribe() throws Exception {
        String salaId = salaConJugadores("Chat ajeno", "ana2", "beto2");
        String ruta = "/api/chat/salas/" + salaId;

        enviar(ruta, "intruso", "hola").andExpect(status().isForbidden());
        mvc.perform(get(ruta).param("userId", "intruso")).andExpect(status().isForbidden());
    }

    @Test
    void conLaPartidaIniciadaElChatDeLaSalaSeCierra() throws Exception {
        String salaId = salaConJugadores("Chat partida", "ana3", "beto3");
        String ruta = "/api/chat/salas/" + salaId;
        enviar(ruta, "ana3", "hola").andExpect(status().isCreated());

        Sala sala = salas.findById(salaId).orElseThrow();
        sala.iniciarPartida();
        salas.save(sala);

        enviar(ruta, "ana3", "sigo aqui").andExpect(status().isConflict());
        assertTrue(historial.historial("salas/" + salaId).isEmpty());
    }

    @Test
    void siElAnfitrionCierraLaSalaSeOlvidaSuHistorial() throws Exception {
        String salaId = salaConJugadores("Chat cerrado", "ana4", "beto4");
        String ruta = "/api/chat/salas/" + salaId;
        enviar(ruta, "beto4", "hola").andExpect(status().isCreated());

        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "ana4")).andExpect(status().isNoContent());

        assertTrue(historial.historial("salas/" + salaId).isEmpty());
        enviar(ruta, "beto4", "alguien?").andExpect(status().isConflict());
    }

    @Test
    void unMensajeVacioODemasiadoLargoSeRechaza() throws Exception {
        String salaId = salaConJugadores("Chat validacion", "ana5", "beto5");
        String ruta = "/api/chat/salas/" + salaId;

        enviar(ruta, "ana5", "   ").andExpect(status().isBadRequest());
        enviar(ruta, "ana5", "x".repeat(201)).andExpect(status().isBadRequest());
    }

    @Test
    void elGlobalEsParaLosUsuariosRegistrados() throws Exception {
        String userId = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"chat_global\"}"))
                .andReturn().getResponse().getContentAsString()).get("userId").asText();

        enviar("/api/chat/global", userId, "hola mundo").andExpect(status().isCreated());
        enviar("/api/chat/global", "no-registrado", "hola").andExpect(status().isBadRequest());

        mvc.perform(get("/api/chat/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.texto == 'hola mundo')]", hasSize(1)));
    }

    @Test
    void alIniciarLaPartidaSeAbrenSuChatYElDelEquipoYAlTerminarSeCierran() throws Exception {
        eventos.publishEvent(new PartidaIniciadaEvent("chat-m1",
                Map.of("chat-e1", List.of("ana6", "beto6"), "chat-e2", List.of("caro6", "dani6"))));
        String general = "/api/chat/partidas/chat-m1";
        String equipo = general + "/equipo";

        enviar(general, "ana6", "suerte").andExpect(status().isCreated());
        enviar(equipo, "ana6", "secreto").andExpect(status().isCreated());

        mvc.perform(get(general).param("userId", "caro6")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(equipo).param("userId", "beto6")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(equipo).param("userId", "caro6")).andExpect(jsonPath("$", hasSize(0)));

        eventos.publishEvent(new MatchFinishedEvent("chat-m1", "TIME_UP", null, Map.of("chat-e1", 0, "chat-e2", 0)));

        enviar(general, "ana6", "tarde").andExpect(status().isConflict());
        assertTrue(historial.historial("partidas/chat-m1").isEmpty());
    }

    private ResultActions enviar(String ruta, String userId, String texto) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userId", userId, "texto", texto))));
    }

    /** Sala creada por el primero (anfitrion) con todos dentro. */
    private String salaConJugadores(String nombre, String... usuarios) throws Exception {
        String salaId = json.readTree(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"" + usuarios[0] + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        for (String u : usuarios) {
            mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + u + "\"}"))
                    .andExpect(status().isOk());
        }
        return salaId;
    }
}
