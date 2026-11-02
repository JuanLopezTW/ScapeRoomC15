package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalirDeSalaUseCase;
import co.eci.c15.salas.infrastructure.web.SesionSalaStompController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DesconexionSalaTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final Duration GRACIA = Duration.ofSeconds(20);

    private SalirDeSalaUseCase salir;
    private TaskScheduler scheduler;
    private SesionSalaStompController controller;
    private DesconexionSalaListener listener;

    @BeforeEach
    void setUp() {
        SesionSalaRegistry sesiones = new SesionSalaRegistry();
        salir = mock(SalirDeSalaUseCase.class);
        scheduler = mock(TaskScheduler.class);
        when(scheduler.getClock()).thenReturn(Clock.fixed(AHORA, ZoneOffset.UTC));
        controller = new SesionSalaStompController(sesiones);
        listener = new DesconexionSalaListener(sesiones, salir, scheduler, GRACIA);
    }

    private static SessionDisconnectEvent evento(String sessionId) {
        Message<byte[]> mensaje = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent("fuente", mensaje, sessionId, CloseStatus.GOING_AWAY);
    }

    /** Se cumple el tiempo de gracia de la ultima desconexion. */
    private void pasaLaGracia(int programadas) {
        ArgumentCaptor<Runnable> tareas = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(programadas)).schedule(tareas.capture(), eq(AHORA.plus(GRACIA)));
        tareas.getValue().run();
    }

    @Test
    void alDesconectarseNoSaleDeUnaSinoCuandoPasaLaGracia() {
        controller.registrarSesion("s1", "ana", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));
        verifyNoInteractions(salir);

        pasaLaGracia(1);
        verify(salir).ejecutar("s1", "ana");
    }

    @Test
    void siVuelveDentroDeLaGraciaNoSale() {
        controller.registrarSesion("s1", "ana", "sesion-1");
        listener.onDisconnect(evento("sesion-1"));

        controller.registrarSesion("s1", "ana", "sesion-nueva");
        pasaLaGracia(1);

        verifyNoInteractions(salir);
    }

    @Test
    void sesionNoRegistradaSeIgnora() {
        listener.onDisconnect(evento("desconocida"));

        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verifyNoInteractions(salir);
    }

    @Test
    void laSesionSoloSeProcesaUnaVez() {
        controller.registrarSesion("s1", "ana", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));
        listener.onDisconnect(evento("sesion-1"));

        pasaLaGracia(1);
        verify(salir, times(1)).ejecutar(anyString(), anyString());
    }

    @Test
    void conOtraPestañaAbiertaEnLaSalaNoSale() {
        controller.registrarSesion("s1", "ana", "sesion-1");
        controller.registrarSesion("s1", "ana", "sesion-2");

        listener.onDisconnect(evento("sesion-1"));
        pasaLaGracia(1);
        verifyNoInteractions(salir);

        listener.onDisconnect(evento("sesion-2"));
        pasaLaGracia(2);
        verify(salir).ejecutar("s1", "ana");
    }
}
