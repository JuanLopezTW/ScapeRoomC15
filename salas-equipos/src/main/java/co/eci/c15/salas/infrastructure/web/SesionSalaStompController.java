package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.infrastructure.events.SesionSalaRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class SesionSalaStompController {

    private final SesionSalaRegistry sesiones;

    public SesionSalaStompController(SesionSalaRegistry sesiones) {
        this.sesiones = sesiones;
    }

    /** El lobby avisa a que jugador y sala pertenece su sesion: SEND /app/salas/{salaId}/jugadores/{userId}/sesion. */
    @MessageMapping("/salas/{salaId}/jugadores/{userId}/sesion")
    public void registrarSesion(@DestinationVariable("salaId") String salaId,
                                @DestinationVariable("userId") String userId,
                                @Header("simpSessionId") String sessionId) {
        sesiones.registrar(sessionId, salaId, userId);
    }
}
