package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ClickAcertijoUseCase;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoNoAbiertoException;
import co.eci.c15.gameplay.domain.AcertijoYaResueltoException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoNoValidoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/acertijos/{componenteMapaId}/clicks")
public class AcertijoClicksController {

    private final ClickAcertijoUseCase useCase;

    public AcertijoClicksController(ClickAcertijoUseCase useCase) {
        this.useCase = useCase;
    }

    /** Click sobre un elemento del acertijo que el jugador tiene abierto. */
    @PostMapping
    public ResponseEntity<?> click(@PathVariable("matchId") String matchId,
                                   @PathVariable("componenteMapaId") String componenteMapaId,
                                   @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) return error(HttpStatus.BAD_REQUEST, "El userId es obligatorio");
        return responder(() -> useCase.click(matchId, componenteMapaId, userId, body.get("elementoId"), body.get("valor")));
    }

    /** Estado actual de lo ingresado por el equipo. */
    @GetMapping
    public ResponseEntity<?> consultar(@PathVariable("matchId") String matchId,
                                       @PathVariable("componenteMapaId") String componenteMapaId,
                                       @RequestParam("userId") String userId) {
        return responder(() -> useCase.consultar(matchId, componenteMapaId, userId));
    }

    /** Borra lo ingresado para empezar de nuevo. */
    @DeleteMapping
    public ResponseEntity<?> reiniciar(@PathVariable("matchId") String matchId,
                                       @PathVariable("componenteMapaId") String componenteMapaId,
                                       @RequestParam("userId") String userId) {
        return responder(() -> useCase.reiniciar(matchId, componenteMapaId, userId));
    }

    private ResponseEntity<?> responder(Supplier<Object> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (JugadorNoRegistradoException | ComponenteSinAcertijoException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AcertijoBloqueadoException | AcertijoNoAbiertoException | AcertijoYaResueltoException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (ElementoNoValidoException | IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<?> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
