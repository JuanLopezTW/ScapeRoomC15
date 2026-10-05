package co.eci.c15.gameplay.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AcertijoTest {

    @Test
    void acertijoCreadoEnEstadoPendiente() {
        Acertijo a = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        assertEquals(Acertijo.Estado.PENDIENTE, a.getEstado());
        assertFalse(a.isResuelto());
    }

    @Test
    void marcarResueltoCambiaEstado() {
        Acertijo a = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        a.marcarResuelto();
        assertTrue(a.isResuelto());
        assertEquals(Acertijo.Estado.RESUELTO, a.getEstado());
    }
}
