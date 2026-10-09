package co.eci.c15.app;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.domain.ChallengeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reto Heroe vs Verdugo (HU-44.5) contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class RetoHeroeVerdugoFlujoTest {

    private static final int CLICKS_PER_PLAYER = 60;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private HeroVillainChallengeService challenges;

    @Test
    void retoCompletoConClicksConcurrentesYMapaCongelado() throws Exception {
        Map<String, String> teamOf = new HashMap<>();
        String matchId = partidaIniciada(teamOf);

        mvc.perform(post("/dev/matches/{id}/challenge/start", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post("/dev/matches/{id}/challenge/start", matchId))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("congelado")));

        Map<String, String> roleOf = new HashMap<>();
        for (String team : new HashMap<>(teamOf).values().stream().distinct().toList()) {
            JsonNode roles = leer(mvc.perform(get("/api/partidas/{m}/equipos/{e}/roles", matchId, team))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("roles");
            roles.fields().forEachRemaining(e -> roleOf.put(e.getKey(), e.getValue().asText()));
        }
        assertEquals(4, roleOf.size());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (String player : roleOf.keySet()) {
            for (int t = 0; t < 2; t++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    for (int i = 0; i < CLICKS_PER_PLAYER / 2; i++) {
                        mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"userId\":\"" + player + "\"}"))
                                .andExpect(status().isNoContent());
                    }
                    return null;
                }));
            }
        }
        go.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        Map<String, Long> expected = new HashMap<>();
        teamOf.values().forEach(team -> expected.put(team, 0L));
        roleOf.forEach((player, role) -> {
            String team = teamOf.get(player);
            if (role.equals("HEROE")) expected.merge(team, (long) CLICKS_PER_PLAYER, Long::sum);
            else expected.keySet().stream().filter(t -> !t.equals(team))
                    .forEach(rival -> expected.merge(rival, (long) -CLICKS_PER_PLAYER, Long::sum));
        });

        ChallengeResult result = challenges.finish(matchId).orElseThrow();
        assertEquals(expected, result.scores());

        mvc.perform(post("/api/partidas/{m}/reto/clicks", matchId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"ana\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/partidas/{m}/jugadores/{u}/movimiento", matchId, "ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"x\":1,\"y\":1}"))
                .andExpect(content().string(not(containsString("congelado"))));
    }

    @Test
    void sinPartidaNoHayReto() throws Exception {
        mvc.perform(post("/dev/matches/{id}/challenge/start", "no-existe"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/partidas/{m}/reto", "no-existe"))
                .andExpect(status().isNotFound());
    }

    /** Arma una sala con dos equipos de dos, los marca listos y espera a que la partida arranque. */
    private String partidaIniciada(Map<String, String> teamOf) throws Exception {
        String salaId = leer(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sala reto\",\"anfitrionId\":\"ana\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        JsonNode equipos = leer(mvc.perform(get("/api/salas/{id}/equipos", salaId))
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

    private JsonNode leer(String body) throws Exception {
        return json.readTree(body);
    }
}
