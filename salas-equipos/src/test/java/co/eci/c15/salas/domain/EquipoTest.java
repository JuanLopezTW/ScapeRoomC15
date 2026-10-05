package co.eci.c15.salas.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipoTest {

    @Test
    void miembroSeUneExitosamente() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        assertTrue(equipo.getMiembros().contains("user-1"));
        assertFalse(equipo.isFull());
    }

    @Test
    void equipoLlenoLanzaExcepcion() {
        Equipo equipo = Equipo.crear("sala-1", 1, 2);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-2");
        assertTrue(equipo.isFull());
        assertThrows(EquipoLlenoException.class, () -> equipo.unirMiembro("user-3"));
    }

    @Test
    void unirseDoVecesNoAumentaContador() {
        Equipo equipo = Equipo.crear("sala-1", 1, 3);
        equipo.unirMiembro("user-1");
        equipo.unirMiembro("user-1");
        assertEquals(1, equipo.getMiembros().size());
    }
}
