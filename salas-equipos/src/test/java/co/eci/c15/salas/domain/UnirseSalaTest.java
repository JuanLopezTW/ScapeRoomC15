package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnirseSalaTest {

    @Test
    void jugadorSeUneExitosamente() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.unirJugador("user-1");
        assertTrue(sala.getJugadores().contains("user-1"));
    }

    @Test
    void salaLlenaLanzaExcepcion() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 1, 2); // cupo = 2
        sala.unirJugador("user-1");
        sala.unirJugador("user-2");
        assertThrows(SalaLlenaException.class, () -> sala.unirJugador("user-3"));
    }

    @Test
    void partidaIniciadaNoPermiteUnirse() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        assertThrows(PartidaYaIniciadaException.class, () -> sala.unirJugador("user-1"));
    }

    @Test
    void unirseDoVecesNoAumentaContador() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.unirJugador("user-1");
        sala.unirJugador("user-1");
        assertEquals(1, sala.getJugadores().size());
    }
}
