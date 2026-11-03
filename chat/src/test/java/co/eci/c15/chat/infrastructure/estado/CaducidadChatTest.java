package co.eci.c15.chat.infrastructure.estado;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CaducidadChatTest {

    private final CaducidadChat caducidad = new CaducidadChat(Duration.ofMinutes(5), Duration.ofMinutes(10));

    @Test
    void nadaPasaDeUnaHora() {
        assertEquals(Duration.ofHours(1), caducidad.deCanal("global"));
        assertEquals(Duration.ofMinutes(30), caducidad.deCanal("salas/s1"));
        assertEquals(Duration.ofMinutes(15), caducidad.deCanal("partidas/m1"));
        assertEquals(Duration.ofMinutes(15), caducidad.deCanal("partidas/m1/equipos/e1"));
        assertEquals(Duration.ofMinutes(15), caducidad.partida());
    }
}
