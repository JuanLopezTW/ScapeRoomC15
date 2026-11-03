package co.eci.c15.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EventoNegocioTest {

    private final Logger log = (Logger) LoggerFactory.getLogger(EventoNegocioTest.class);
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();

    @BeforeEach
    void setUp() {
        eventos.start();
        log.addAppender(eventos);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(eventos);
        MDC.clear();
    }

    private Map<String, Object> campos() {
        return eventos.list.get(0).getKeyValuePairs().stream().collect(Collectors.toMap(k -> k.key, k -> k.value));
    }

    @Test
    void registraElNombreYLosCamposComoDatosYEnElMensaje() {
        EventoNegocio.de(log, "sala.creada").con("salaId", "s1").con("equipos", 2).info();

        ILoggingEvent evento = eventos.list.get(0);
        assertEquals(Level.INFO, evento.getLevel());
        assertEquals("sala.creada salaId=s1 equipos=2", evento.getFormattedMessage());
        assertEquals(Map.of("evento", "sala.creada", "salaId", "s1", "equipos", 2), campos());
    }

    @Test
    void puedeSerUnaAdvertencia() {
        EventoNegocio.de(log, "chat.limite_excedido").warn();

        assertEquals(Level.WARN, eventos.list.get(0).getLevel());
    }

    @Test
    void noRepiteLoQueYaEstaEnElContextoConElMismoValor() {
        MDC.put("userId", "auth0|ana");

        EventoNegocio.de(log, "sala.jugador_unido").con("userId", "auth0|ana").info();

        assertFalse(campos().containsKey("userId"));
        assertTrue(eventos.list.get(0).getFormattedMessage().contains("userId=auth0|ana"));
    }

    @Test
    void siElContextoTieneOtroValorUsaOtraClaveParaNoDuplicarla() {
        MDC.put("userId", "auth0|ana");

        EventoNegocio.de(log, "jugador.salio_partida").con("userId", "auth0|beto").info();

        assertEquals("auth0|beto", campos().get("evento.userId"));
        assertFalse(campos().containsKey("userId"));
    }

    @Test
    void limpiaSaltosDeLineaYRecortaValoresLargos() {
        EventoNegocio.de(log, "x").con("a", "uno\ndos\r").con("b", "y".repeat(500)).con("c", null).info();

        assertEquals("uno_dos_", campos().get("a"));
        assertEquals(200, campos().get("b").toString().length());
        assertEquals("null", campos().get("c"));
        assertFalse(eventos.list.get(0).getFormattedMessage().contains("\n"));
    }
}
