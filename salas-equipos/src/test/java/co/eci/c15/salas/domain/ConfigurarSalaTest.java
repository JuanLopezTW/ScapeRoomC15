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
    void ceroJugadoresPorEquipoEsInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 2, 0));
    }

    @Test
    void configuracionPreviaSeConservaAnteValorInvalido() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 3, 5);
        assertThrows(ConfiguracionInvalidaException.class, () -> sala.configurar("anfitrion-1", 0, 5));
        assertEquals(3, sala.getNumEquipos());
        assertEquals(5, sala.getJugadoresPorEquipo());
    }
}
