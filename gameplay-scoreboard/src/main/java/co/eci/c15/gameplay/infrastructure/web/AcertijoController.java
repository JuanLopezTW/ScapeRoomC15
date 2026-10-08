package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AbrirAcertijoUseCase;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoLejosException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.OtroAcertijoAbiertoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/acertijos/{componenteMapaId}/bloqueo")
public class AcertijoController {

    private final AbrirAcertijoUseCase useCase;

    public AcertijoController(AbrirAcertijoUseCase useCase) {
        this.useCase = useCase;
    }

    /** Abre el acertijo (Enter junto al punto brillante) y lo bloquea para el resto del equipo. */
    @PostMapping
    public ResponseEntity<?> abrir(@PathVariable("matchId") String matchId,
                                   @PathVariable("componenteMapaId") String componenteMapaId,
                                   @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El userId es obligatorio"));
        }
        try {
            return ResponseEntity.ok(useCase.ejecutar(matchId, componenteMapaId, userId));
        } catch (JugadorNoRegistradoException | ComponenteSinAcertijoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AcertijoBloqueadoException | OtroAcertijoAbiertoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (AcertijoLejosException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Cierra el acertijo y libera el bloqueo. */
    @DeleteMapping
    public ResponseEntity<?> cerrar(@PathVariable("matchId") String matchId,
                                    @PathVariable("componenteMapaId") String componenteMapaId,
                                    @RequestParam("userId") String userId) {
        try {
            useCase.cerrar(matchId, componenteMapaId, userId);
            return ResponseEntity.noContent().build();
        } catch (JugadorNoRegistradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
