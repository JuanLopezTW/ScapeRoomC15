package co.eci.c15.app;

import co.eci.c15.common.events.AcertijoResueltoEvent;
import co.eci.c15.common.events.TimeUpEvent;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fin de la partida y ganador (HU-67.6) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class GanarPartidaFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private HeroVillainChallengeService challenges;

    @Test
    void resolverTodosLosAcertijosTerminaLaPartidaYGanaElDeMasPuntaje() throws Exception {
        Map<String, String> teamOf = new LinkedHashMap<>();
        String matchId = partidaIniciada("Sala ganar", teamOf);
        String equipoA = teamOf.get("ana");
        String equipoB = teamOf.get("caro");

        mvc.perform(get("/api/partidas/{m}/resultado", matchId)).andExpect(status().isNotFound());

        // El equipo B gana el reto: solo hace click su heroe
        mvc.perform(post("/dev/matches/{m}/challenge/start", matchId)).andExpect(status().isOk());
        String heroeB = heroeDe(matchId, equipoB);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + heroeB + "\"}"))
                    .andExpect(status().isNoContent());
        }
        challenges.finish(matchId);

        // B resuelve 1 acertijo y A los resuelve todos (el acertijo-1 llega repetido: no cuenta doble)
        events.publishEvent(new AcertijoResueltoEvent(matchId, equipoB, "acertijo-1", "caro"));
        for (String acertijo : new String[]{"acertijo-1", "acertijo-1", "acertijo-2", "acertijo-3"}) {
            events.publishEvent(new AcertijoResueltoEvent(matchId, equipoA, acertijo, "ana"));
        }

        mvc.perform(get("/api/partidas/{m}/resultado", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("ALL_PUZZLES_SOLVED"))
                .andExpect(jsonPath("$.winnerTeamId").value(equipoA))
                .andExpect(jsonPath("$.standings[0].teamId").value(equipoA))
                .andExpect(jsonPath("$.standings[0].puzzlesSolved").value(3))
                .andExpect(jsonPath("$.standings[0].score").value(3))
                .andExpect(jsonPath("$.standings[1].challengesWon").value(1))
                .andExpect(jsonPath("$.standings[1].score").value(2));

        mvc.perform(get("/api/matches/{m}/timer", matchId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("termino")));
    }

    @Test
    void siSeAcabaElTiempoIgualadosEsEmpate() throws Exception {
        Map<String, String> teamOf = new LinkedHashMap<>();
        String matchId = partidaIniciada("Sala empate", teamOf);

        events.publishEvent(new AcertijoResueltoEvent(matchId, teamOf.get("ana"), "acertijo-1", "ana"));
        events.publishEvent(new AcertijoResueltoEvent(matchId, teamOf.get("caro"), "acertijo-2", "caro"));
        events.publishEvent(new TimeUpEvent(matchId, Instant.now()));

        mvc.perform(get("/api/partidas/{m}/resultado", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("TIME_UP"))
                .andExpect(jsonPath("$.draw").value(true))
                .andExpect(jsonPath("$.winnerTeamId").doesNotExist())
                .andExpect(jsonPath("$.tiedTeamIds", hasSize(2)));
    }

    private String heroeDe(String matchId, String equipoId) throws Exception {
        JsonNode roles = json.readTree(mvc.perform(get("/api/partidas/{m}/equipos/{e}/roles", matchId, equipoId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("roles");
        var it = roles.fields();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue().asText().equals("HEROE")) return e.getKey();
        }
        throw new AssertionError("El equipo " + equipoId + " no tiene heroe");
    }

    /** Sala con dos equipos (ana y beto, caro y dani), todos listos; espera a que la partida arranque. */
    private String partidaIniciada(String nombre, Map<String, String> teamOf) throws Exception {
        String salaId = json.readTree(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"anfitrionId\":\"ana\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        JsonNode equipos = json.readTree(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString());
        String[][] reparto = {{"ana", "beto"}, {"caro", "dani"}};
        for (int i = 0; i < reparto.length; i++) {
            String equipoId = equipos.get(i).get("id").asText();
            for (String u : reparto[i]) {
                mvc.perform(post("/api/salas/{id}/jugadores", salaId).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + u + "\"}"))
                        .andExpect(status().isOk());
                mvc.perform(post("/api/equipos/{id}/miembros", equipoId).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":\"" + u + "\"}"))
                        .andExpect(status().isOk());
                teamOf.put(u, equipoId);
            }
        }
        for (String u : teamOf.keySet()) {
            mvc.perform(post("/api/salas/{id}/listo", salaId).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + u + "\"}"))
                    .andExpect(status().isOk());
        }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(get("/api/salas/{id}", salaId)).andExpect(status().isConflict()));
        return salaId;
    }
}
