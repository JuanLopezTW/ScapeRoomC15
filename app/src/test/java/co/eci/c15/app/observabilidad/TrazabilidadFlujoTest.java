package co.eci.c15.app.observabilidad;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con el tracing real encendido: cada peticion tiene traceId, su contexto en el log y lo hereda lo programado. */
@SpringBootTest(properties = "management.tracing.sampling.probability=1.0")
@AutoConfigureMockMvc
@AutoConfigureObservability(metrics = false)
class TrazabilidadFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RegistroPeticionesFilter registro;

    @Autowired
    private ObservationRegistry observaciones;

    @Autowired
    private Tracer tracer;

    @Autowired
    private StompTrazaInterceptor traza;

    @Autowired
    private ApplicationEventPublisher publicador;

    private final MessageChannel canal = mock(MessageChannel.class);

    @Autowired
    @Qualifier("salasScheduler")
    private TaskScheduler salas;

    @Autowired
    @Qualifier("gameplayScheduler")
    private TaskScheduler gameplay;

    private final Logger logger = (Logger) LoggerFactory.getLogger(RegistroPeticionesFilter.class);
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();

    @BeforeEach
    void setUp() {
        eventos.start();
        logger.addAppender(eventos);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(eventos);
        MDC.clear();
    }

    @Test
    void laRespuestaTraeElTraceIdYElLogLoLlevaConElJugadorYLaSala() throws Exception {
        String traza = mvc.perform(get("/api/salas/no-existe").param("userId", "auth0|ana"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getHeader(RegistroPeticionesFilter.CABECERA_TRAZA);

        assertNotNull(traza);
        assertTrue(traza.matches("[0-9a-f]{32}"), traza);
        ILoggingEvent evento = eventos.list.get(eventos.list.size() - 1);
        assertEquals(traza, evento.getMDCPropertyMap().get("traceId"));
        assertEquals("auth0|ana", evento.getMDCPropertyMap().get(ContextoLog.USER_ID));
        assertEquals("no-existe", evento.getMDCPropertyMap().get(ContextoLog.SALA_ID));
        assertTrue(evento.getFormattedMessage().contains("/api/salas/{salaId} -> 404"), evento.getFormattedMessage());
    }

    @Test
    void unaPeticionSinTokenTambienQuedaRegistradaYSinJugador() throws Exception {
        MockMvc sinToken = MockMvcBuilders.webAppContextSetup(contexto).addFilters(registro).apply(springSecurity()).build();

        sinToken.perform(get("/api/salas")).andExpect(status().isUnauthorized());

        ILoggingEvent evento = eventos.list.get(eventos.list.size() - 1);
        assertTrue(evento.getFormattedMessage().startsWith("GET /api/salas -> 401"), evento.getFormattedMessage());
        assertNull(evento.getMDCPropertyMap().get(ContextoLog.USER_ID));
    }

    private void heredaElContexto(TaskScheduler scheduler) throws Exception {
        CompletableFuture<String[]> dentro = new CompletableFuture<>();
        String[] afuera = new String[1];

        Observation.createNotStarted("prueba", observaciones).observe(() -> {
            afuera[0] = tracer.currentSpan().context().traceId();
            MDC.put(ContextoLog.MATCH_ID, "m-1");
            scheduler.schedule(() -> dentro.complete(new String[]{MDC.get("traceId"), MDC.get(ContextoLog.MATCH_ID)}),
                    Instant.now());
            MDC.remove(ContextoLog.MATCH_ID);
        });

        String[] enTarea = dentro.get(5, TimeUnit.SECONDS);
        assertEquals(afuera[0], enTarea[0]);
        assertEquals("m-1", enTarea[1]);
    }

    @Test
    void elHeaderDeTrazaSePuedeLeerDesdeOtroDominio() throws Exception {
        mvc.perform(get("/api/salas"))
                .andExpect(header().string("Access-Control-Expose-Headers", RegistroPeticionesFilter.CABECERA_TRAZA));
    }

    private static Message<byte[]> frame(StompCommand comando, String destino, String usuario) {
        StompHeaderAccessor stomp = StompHeaderAccessor.create(comando);
        stomp.setDestination(destino);
        if (usuario != null) stomp.setUser((Principal) () -> usuario);
        stomp.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], stomp.getMessageHeaders());
    }

    @Test
    void cadaFrameStompTieneTrazaYContextoYSeLimpiaAlTerminar() {
        Message<byte[]> suscripcion = frame(StompCommand.SUBSCRIBE, "/topic/partidas/m-7/reto", "auth0|ana");

        traza.preSend(suscripcion, canal);

        assertNotNull(tracer.currentSpan());
        String enFrame = tracer.currentSpan().context().traceId();
        assertEquals("auth0|ana", MDC.get(ContextoLog.USER_ID));
        assertEquals("m-7", MDC.get(ContextoLog.MATCH_ID));
        assertEquals(enFrame, MDC.get("traceId"));

        traza.afterSendCompletion(suscripcion, canal, true, null);

        assertNull(tracer.currentSpan());
        assertNull(MDC.get(ContextoLog.USER_ID));
        assertNull(MDC.get(ContextoLog.MATCH_ID));

        Message<byte[]> otro = frame(StompCommand.SEND, "/app/salas/s-1/sesion", "auth0|ana");
        traza.preSend(otro, canal);
        assertNotEquals(enFrame, tracer.currentSpan().context().traceId());
        assertEquals("s-1", MDC.get(ContextoLog.SALA_ID));
        traza.afterSendCompletion(otro, canal, true, new IllegalStateException("rechazado"));
        assertNull(tracer.currentSpan());
    }

    @Test
    void losLatidosYCompletarSinFrameNoAbrenNada() {
        Message<byte[]> latido = MessageBuilder.withPayload(new byte[0]).build();

        assertSame(latido, traza.preSend(latido, canal));
        assertNull(tracer.currentSpan());
        assertDoesNotThrow(() -> traza.afterSendCompletion(latido, canal, true, null));
    }

    @Test
    void unaDesconexionTieneTrazaYElJugadorParaSusListeners() {
        Logger delMulticaster = (Logger) LoggerFactory.getLogger(DesconexionConTrazaMulticaster.class);
        ListAppender<ILoggingEvent> desconexiones = new ListAppender<>();
        desconexiones.start();
        delMulticaster.addAppender(desconexiones);
        try {
            StompHeaderAccessor frame = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            frame.setSessionId("sesion-traza");
            publicador.publishEvent(new SessionDisconnectEvent("prueba",
                    MessageBuilder.createMessage(new byte[0], frame.getMessageHeaders()),
                    "sesion-traza", CloseStatus.GOING_AWAY, () -> "auth0|ana"));
        } finally {
            delMulticaster.detachAppender(desconexiones);
        }

        ILoggingEvent evento = desconexiones.list.get(0);
        assertTrue(evento.getFormattedMessage().startsWith("stomp.desconectado"), evento.getFormattedMessage());
        assertTrue(evento.getMDCPropertyMap().get("traceId").matches("[0-9a-f]{32}"));
        assertEquals("auth0|ana", evento.getMDCPropertyMap().get(ContextoLog.USER_ID));
        assertNull(MDC.get(ContextoLog.USER_ID));
    }

    @Test
    void lasTareasProgramadasDeSalasYGameplayHeredanTraceIdYContexto() throws Exception {
        heredaElContexto(salas);
        heredaElContexto(gameplay);
    }
}
