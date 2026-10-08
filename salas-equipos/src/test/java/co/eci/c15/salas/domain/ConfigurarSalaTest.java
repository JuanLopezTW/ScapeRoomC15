package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurarSalaTest {

    @Test
    void anfitrionPuedeCambiarConfiguracion() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 3, 5);
        assertEquals(3, sala.getNumEquipos());
        assertEquals(5, sala.getJugadoresPorEquipo());
    }

    @Test
    void noAnfitrionNoPuedeCambiarConfiguracion() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(NoEsAnfitrionException.class, () -> sala.configurar("otro-user", 3, 5));
    }

    @Test
    void ceroEquiposEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 0, 4));
    }

    @Test
    void unSoloEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 1, 4));
    }

    @Test
    void ceroJugadoresPorEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 0));
    }

    @Test
    void unJugadorPorEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 1));
    }

    @Test
    void configuracionPreviaSeConservaAnteValorInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 3, 5);
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 0, 5));
        assertEquals(3, sala.getNumEquipos());
        assertEquals(5, sala.getJugadoresPorEquipo());
    }

    @Test
    void cupoNoPuedeQuedarPorDebajoDeLosJugadoresActuales() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        for (int i = 1; i <= 5; i++) sala.unirJugador("user-" + i);
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 2));
        assertEquals(4, sala.getJugadoresPorEquipo());
    }

    @Test
    void noSePuedeConfigurarConPartidaIniciada() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> sala.configurar("anfitrion-1", 2, 3));
    }
}