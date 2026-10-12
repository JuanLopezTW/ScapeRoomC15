package co.eci.c15.app;

import co.eci.c15.salas.infrastructure.events.DesconexionSalaListener;
import co.eci.c15.salas.infrastructure.web.SesionSalaStompController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cerrar la pagina en el lobby saca al jugador; si es el anfitrion, la sala se cierra. */
@SpringBootTest
@AutoConfigureMockMvc
class SalirDeSalaFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private SesionSalaStompController sesiones;

    @Autowired
    private DesconexionSalaListener desconexion;

    @Test
    void unJugadorQueCierraLaPaginaSaleDeLaSalaYDeSuEquipo() throws Exception {
        String salaId = salaConJugadores("Lobby jugador", "ana", "beto");
        String equipoId = primerEquipo(salaId);
        unirseAEquipo(equipoId, "beto");
        sesiones.registrarSesion(salaId, "beto", "sesion-beto");

        desconectar("sesion-beto");

        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", not(hasItem("beto"))))
                .andExpect(jsonPath("$.jugadores", hasSize(1)));
        mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andExpect(jsonPath("$[0].miembros", hasSize(0)));
    }

    @Test
    void siElAnfitrionCierraLaPaginaLaSalaDesaparece() throws Exception {
        String salaId = salaConJugadores("Lobby anfitrion", "ana", "beto");
        sesiones.registrarSesion(salaId, "ana", "sesion-ana");

        desconectar("sesion-ana");

        mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/salas"))
                .andExpect(jsonPath("$[?(@.id == '" + salaId + "')]", hasSize(0)));
    }

    @Test
    void elBotonVolverSacaAlJugadorYSiEsElAnfitrionCierraLaSala() throws Exception {
        String salaId = salaConJugadores("Lobby volver", "ana", "beto");

        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "beto")).andExpect(status().isNoContent());
        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadores", not(hasItem("beto"))));

        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "ana")).andExpect(status().isNoContent());
        mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isNotFound());

        // Repetirlo no falla: no hay nada que hacer
        mvc.perform(delete("/api/salas/{id}/jugadores/{u}", salaId, "ana")).andExpect(status().isNoContent());
    }

    private void desconectar(String sessionId) {
        desconexion.onDisconnect(new SessionDisconnectEvent("test",
                MessageBuilder.withPayload(new byte[0]).build(), sessionId, CloseStatus.GOING_AWAY));
    }

    /** Sala creada por el primero (anfitrion) con todos dentro. */
    private String salaConJugadores(String nombre, String... usuarios) throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
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

    private String primerEquipo(String salaId) throws Exception {
        return leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private void unirseAEquipo(String equipoId, String userId) throws Exception {
        mvc.perform(post("/api/equipos/{id}/miembros", equipoId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
