package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalirDeSalaUseCase;
import co.eci.c15.salas.infrastructure.web.SesionSalaStompController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DesconexionSalaTest {

    private SalirDeSalaUseCase salir;
    private SesionSalaStompController controller;
    private DesconexionSalaListener listener;

    @BeforeEach
    void setUp() {
        SesionSalaRegistry sesiones = new SesionSalaRegistry();
        salir = mock(SalirDeSalaUseCase.class);
        controller = new SesionSalaStompController(sesiones);
        listener = new DesconexionSalaListener(sesiones, salir);
    }

    private static SessionDisconnectEvent evento(String sessionId) {
        Message<byte[]> mensaje = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent("fuente", mensaje, sessionId, CloseStatus.GOING_AWAY);
    }

    @Test
    void alDesconectarseSaleDeLaSala() {
        controller.registrarSesion("s1", "ana", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));

        verify(salir).ejecutar("s1", "ana");
    }

    @Test
    void sesionNoRegistradaSeIgnora() {
        listener.onDisconnect(evento("desconocida"));

        verifyNoInteractions(salir);
    }

    @Test
    void laSesionSoloSeProcesaUnaVez() {
        controller.registrarSesion("s1", "ana", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));
        listener.onDisconnect(evento("sesion-1"));

        verify(salir, times(1)).ejecutar(anyString(), anyString());
    }

    @Test
    void conOtraPestañaAbiertaEnLaSalaNoSale() {
        controller.registrarSesion("s1", "ana", "sesion-1");
        controller.registrarSesion("s1", "ana", "sesion-2");

        listener.onDisconnect(evento("sesion-1"));
        verifyNoInteractions(salir);

        listener.onDisconnect(evento("sesion-2"));
        verify(salir).ejecutar("s1", "ana");
    }
}
