package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.infrastructure.events.SesionJugadorRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SesionStompController {

    private final SesionJugadorRegistry sesiones;

    public SesionStompController(SesionJugadorRegistry sesiones) {
        this.sesiones = sesiones;
    }

    /** El cliente avisa en que partida esta esta conexion; el jugador sale del token: SEND /app/partidas/{matchId}/sesion. */
    @MessageMapping("/partidas/{matchId}/sesion")
    public void registrarSesion(@DestinationVariable("matchId") String matchId,
                                Principal yo,
                                @Header("simpSessionId") String sessionId) {
        sesiones.registrar(sessionId, matchId, yo.getName());
    }
}
