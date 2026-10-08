package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import co.eci.c15.gameplay.application.PosicionJugadorDto;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.infrastructure.persistence.JugadorEnMapaRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.web.SesionStompController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DesconexionStompTest {

    private JugadorEnMapaRepositoryEnMemoria jugadores;
    private List<PosicionJugadorDto> avisos;
    private SesionJugadorRegistry sesiones;
    private SesionStompController controller;
    private DesconexionStompListener listener;

    @BeforeEach
    void setUp() {
        jugadores = new JugadorEnMapaRepositoryEnMemoria();
        jugadores.saveIfAbsent(new JugadorEnMapa("p1", "e1", "u1", new Posicion(2, 3)));
        avisos = new ArrayList<>();
        sesiones = new SesionJugadorRegistry();
        controller = new SesionStompController(sesiones);
        listener = new DesconexionStompListener(sesiones,
                new DesconectarJugadorUseCase(jugadores, (m, e, dto) -> avisos.add(dto)));
    }

    private static SessionDisconnectEvent evento(String sessionId) {
        Message<byte[]> mensaje = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent("fuente", mensaje, sessionId, CloseStatus.GOING_AWAY);
    }

    @Test
    void desconectarLaSesionSacaAlJugadorYAvisa() {
        controller.registrarSesion("p1", "u1", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));

        assertTrue(jugadores.find("p1", "u1").isEmpty());
        assertEquals(List.of(new PosicionJugadorDto("u1", 2, 3, false)), avisos);
    }

    @Test
    void sesionNoRegistradaSeIgnora() {
        listener.onDisconnect(evento("desconocida"));

        assertTrue(jugadores.find("p1", "u1").isPresent());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void laSesionSoloSeProcesaUnaVez() {
        controller.registrarSesion("p1", "u1", "sesion-1");

        listener.onDisconnect(evento("sesion-1"));
        listener.onDisconnect(evento("sesion-1"));

        assertEquals(1, avisos.size());
    }
}
