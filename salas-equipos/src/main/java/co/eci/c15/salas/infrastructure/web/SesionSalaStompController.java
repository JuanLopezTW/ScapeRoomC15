package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.infrastructure.events.SesionSalaRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SesionSalaStompController {

    private final SesionSalaRegistry sesiones;

    public SesionSalaStompController(SesionSalaRegistry sesiones) {
        this.sesiones = sesiones;
    }

    /** El lobby avisa en que sala esta esta conexion; el jugador sale del token: SEND /app/salas/{salaId}/sesion. */
    @MessageMapping("/salas/{salaId}/sesion")
    public void registrarSesion(@DestinationVariable("salaId") String salaId,
                                Principal yo,
                                @Header("simpSessionId") String sessionId) {
        sesiones.registrar(sessionId, salaId, yo.getName());
    }
}
