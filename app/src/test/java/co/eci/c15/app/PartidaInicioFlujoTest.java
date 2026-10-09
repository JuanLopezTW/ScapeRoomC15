package co.eci.c15.app;

import co.eci.c15.salas.application.CuentaRegresivaPartida;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Arranque automatico de la partida (HU-64.4) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class PartidaInicioFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private CuentaRegresivaPartida cuentaRegresiva;

    @Test
    void conDosEquiposListosLaPartidaArrancaSolaYArrancaElCronometro() throws Exception {
        String salaId = salaConDosEquipos("Sala inicio");
        for (String u : new String[]{"ana", "beto", "caro", "dani"}) marcarListo(salaId, u);
        assertTrue(cuentaRegresiva.enCurso(salaId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isConflict()));

        mvc.perform(get("/api/matches/{id}/timer", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingSeconds", greaterThan(1700)));
        mvc.perform(get("/api/partidas/{id}/jugadores/{u}/rol", salaId, "ana"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ana\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void siAlguienSeDesmarcaDuranteLaCuentaRegresivaNoArranca() throws Exception {
        String salaId = salaConDosEquipos("Sala cancelada");
        for (String u : new String[]{"ana", "beto", "caro", "dani"}) marcarListo(salaId, u);
        assertTrue(cuentaRegresiva.enCurso(salaId));

        mvc.perform(delete("/api/salas/{id}/listo", salaId).param("userId", "dani"))
                .andExpect(status().isOk());
        assertFalse(cuentaRegresiva.enCurso(salaId));

        Thread.sleep(1500);
        mvc.perform(get("/api/salas/{id}", salaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        mvc.perform(get("/api/matches/{id}/timer", salaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void unJugadorSinEquipoImpideElArranque() throws Exception {
        String salaId = salaConDosEquipos("Sala con suelto");
        unirseASala(salaId, "eli");
        for (String u : new String[]{"ana", "beto", "caro", "dani"}) marcarListo(salaId, u);

        assertFalse(cuentaRegresiva.enCurso(salaId));
    }

    /** Sala con dos equipos: ana y beto en el 1, caro y dani en el 2. */
    private String salaConDosEquipos(String nombre) throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"ana\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        JsonNode equipos = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString());
        String[][] reparto = {{"ana", "beto"}, {"caro", "dani"}};
        for (int i = 0; i < reparto.length; i++) {
            for (String u : reparto[i]) {
                unirseASala(salaId, u);
                mvc.perform(post("/api/equipos/{id}/miembros", equipos.get(i).get("id").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + u + "\"}"))
                        .andExpect(status().isOk());
            }
        }
        return salaId;
    }

    private void unirseASala(String salaId, String userId) throws Exception {
        mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private void marcarListo(String salaId, String userId) throws Exception {
        mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
