package co.eci.c15.salas.infrastructure.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SesionSalaRegistryTest {

    private final PresenciaSala presencia = mock(PresenciaSala.class);
    private final SesionSalaRegistry sesiones = new SesionSalaRegistry(presencia);

    @Test
    void registrarYRetirarAvisanALaPresenciaCompartida() {
        sesiones.registrar("sesion-1", "s1", "ana");

        assertEquals(new SesionSalaRegistry.Sesion("s1", "ana"), sesiones.retirar("sesion-1").orElseThrow());
        verify(presencia).entrar("sesion-1", "s1", "ana");
        verify(presencia).salir("sesion-1", "s1", "ana");
    }

    @Test
    void siLaMismaConexionPasaAOtraSalaSaleDeLaAnterior() {
        sesiones.registrar("sesion-1", "s1", "ana");
        sesiones.registrar("sesion-1", "s2", "ana");

        verify(presencia).salir("sesion-1", "s1", "ana");
        verify(presencia).entrar("sesion-1", "s2", "ana");
    }

    @Test
    void retirarUnaSesionDesconocidaNoHaceNada() {
        assertTrue(sesiones.retirar("nunca").isEmpty());
        verifyNoInteractions(presencia);
    }

    @Test
    void siSigueConectadoLoDiceLaPresencia() {
        when(presencia.conectado("s1", "ana")).thenReturn(true);

        assertTrue(sesiones.tieneOtraSesion("s1", "ana"));
        assertFalse(sesiones.tieneOtraSesion("s1", "beto"));
    }
}
