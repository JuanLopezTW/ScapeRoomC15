package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.infrastructure.events.SesionJugadorRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class SesionStompController {

    private final SesionJugadorRegistry sesiones;

    public SesionStompController(SesionJugadorRegistry sesiones) {
        this.sesiones = sesiones;
    }

    /** El cliente avisa a que jugador pertenece su sesion: SEND /app/partidas/{matchId}/jugadores/{userId}/sesion. */
    @MessageMapping("/partidas/{matchId}/jugadores/{userId}/sesion")
    public void registrarSesion(@DestinationVariable("matchId") String matchId,
                                @DestinationVariable("userId") String userId,
                                @Header("simpSessionId") String sessionId) {
        sesiones.registrar(sessionId, matchId, userId);
    }
}
