package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.ChatEsperaService;
import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

/** Chat de la sala de espera: GET historial y POST mensaje; los mensajes nuevos llegan por STOMP. */
@RestController
@RequestMapping("/api/salas/{salaId}/chat")
public class ChatSalaController {

    private final ChatEsperaService chat;

    public ChatSalaController(ChatEsperaService chat) {
        this.chat = chat;
    }

    @GetMapping
    public ResponseEntity<?> historial(@PathVariable("salaId") String salaId,
                                       @RequestParam(value = "userId", required = false) String userId) {
        return responder(HttpStatus.OK, () -> chat.historial(salaId, userId));
    }

    @PostMapping
    public ResponseEntity<?> enviar(@PathVariable("salaId") String salaId, @RequestBody Map<String, String> body) {
        return responder(HttpStatus.CREATED, () -> chat.enviar(salaId, body.get("userId"), body.get("texto")));
    }

    private static ResponseEntity<?> responder(HttpStatus ok, Supplier<Object> accion) {
        try {
            return ResponseEntity.status(ok).body(accion.get());
        } catch (ChatNoDisponibleException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (JugadorFueraDeSalaException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<?> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
