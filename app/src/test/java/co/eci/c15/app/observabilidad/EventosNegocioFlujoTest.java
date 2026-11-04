package co.eci.c15.app.observabilidad;

import co.eci.c15.app.integracion.SeguimientoEventos;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cada cosa que pasa en una partida deja un evento con nombre fijo y en orden, sin datos sensibles. */
@SpringBootTest
@AutoConfigureMockMvc
class EventosNegocioFlujoTest {

    private static final String SECRETO = "mi-clave-del-chat-no-debe-salir";

    @Autowired
    private SeguimientoEventos entreServicios;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private final Logger raiz = (Logger) LoggerFactory.getLogger("co.eci.c15");
    private final List<ILoggingEvent> eventos = new CopyOnWriteArrayList<>();
    private final ListAppender<ILoggingEvent> captura = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent evento) {
            evento.prepareForDeferredProcessing();
            eventos.add(evento);
        }
    };

    @BeforeEach
    void setUp() {
        captura.start();
        raiz.addAppender(captura);
    }

    @AfterEach
    void tearDown() {
        raiz.detachAppender(captura);
    }

    private static Optional<String> nombre(ILoggingEvent evento) {
        if (evento.getKeyValuePairs() == null) return Optional.empty();
        return evento.getKeyValuePairs().stream().filter(k -> "evento".equals(k.key)).map(k -> String.valueOf(k.value)).findFirst();
    }

    private List<String> nombres() {
        return eventos.stream().map(EventosNegocioFlujoTest::nombre).flatMap(Optional::stream)
                .filter(n -> !n.startsWith("http.")).toList();
    }

    private static void enOrden(List<String> todos, String... esperados) {
        int desde = 0;
        for (String e : esperados) {
            int i = todos.subList(desde, todos.size()).indexOf(e);
            assertTrue(i >= 0, "falta " + e + " despues de la posicion " + desde + " en " + todos);
            desde += i + 1;
        }
    }

    private void enviar(String ruta, String cuerpo) throws Exception {
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().is2xxSuccessful());
    }

    @Test
    void unaPartidaCompletaDejaSusEventosEnOrden() throws Exception {
        enviar("/api/perfil", "{\"userId\":\"ev-ana\",\"username\":\"ev_ana\"}");
        String salaId = json.readTree(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Eventos\",\"anfitrionId\":\"ev-ana\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        enviar("/api/chat/global", "{\"userId\":\"ev-ana\",\"texto\":\"" + SECRETO + "\"}");

        JsonNode equipos = json.readTree(mvc.perform(get("/api/salas/{id}/equipos", salaId))
                .andReturn().getResponse().getContentAsString());
        String[][] reparto = {{"ev-ana", "ev-beto"}, {"ev-caro", "ev-dani"}};
        for (int i = 0; i < 2; i++) {
            for (String u : reparto[i]) {
                enviar("/api/salas/" + salaId + "/jugadores", "{\"userId\":\"" + u + "\"}");
                enviar("/api/equipos/" + equipos.get(i).get("id").asText() + "/miembros", "{\"userId\":\"" + u + "\"}");
            }
        }
        for (String[] equipo : reparto) {
            for (String u : equipo) enviar("/api/salas/" + salaId + "/listo", "{\"userId\":\"" + u + "\"}");
        }
        await().atMost(Duration.ofSeconds(10)).until(() -> nombres().contains("partida.iniciada"));
        // Gameplay arma la partida despues del commit y en otro hilo
        await().atMost(Duration.ofSeconds(10)).until(entreServicios::entregados);

        mvc.perform(post("/dev/partidas/{m}/reto/inicio", salaId)).andExpect(status().isOk());
        mvc.perform(delete("/api/partidas/{m}/jugadores/{u}", salaId, "ev-dani")).andExpect(status().isNoContent());

        enOrden(nombres(), "perfil.registrado", "sala.creada", "chat.mensaje_enviado",
                "sala.jugador_unido", "equipo.jugador_unido", "jugador.listo", "partida.cuenta_regresiva",
                "partida.iniciada", "reto.iniciado", "jugador.salio_partida");
        assertTrue(nombres().contains("cronometro.iniciado"), nombres().toString());

        // El evento de inicio dice de que partida es y cuantos jugadores tiene
        ILoggingEvent iniciada = eventos.stream().filter(e -> nombre(e).filter("partida.iniciada"::equals).isPresent()).findFirst().orElseThrow();
        assertTrue(iniciada.getFormattedMessage().contains("matchId=" + salaId), iniciada.getFormattedMessage());
        assertTrue(iniciada.getFormattedMessage().contains("jugadores=4"), iniciada.getFormattedMessage());

        // El texto del chat no aparece en ninguna linea
        assertTrue(eventos.stream().noneMatch(e -> e.getFormattedMessage().contains(SECRETO)
                || (e.getKeyValuePairs() != null && e.getKeyValuePairs().stream().anyMatch(k -> String.valueOf(k.value).contains(SECRETO)))));
    }

    @Test
    void cerrarLaSalaDejaEventoConSuMotivo() throws Exception {
        String salaId = json.readTree(mvc.perform(post("/api/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cierre\",\"anfitrionId\":\"cl-ana\"}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(delete("/api/salas/{s}/jugadores/{u}", salaId, "cl-ana")).andExpect(status().isNoContent());

        ILoggingEvent cerrada = eventos.stream().filter(e -> nombre(e).filter("sala.cerrada"::equals).isPresent()).findFirst().orElseThrow();
        assertTrue(cerrada.getFormattedMessage().contains("motivo=anfitrion_salio"), cerrada.getFormattedMessage());
    }
}
