package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AbrirAcertijoConBloqueoUseCase;
import co.eci.c15.gameplay.application.AcertijoDto;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/acertijos")
public class AcertijoController {

    private final AbrirAcertijoConBloqueoUseCase useCase;

    public AcertijoController(AbrirAcertijoConBloqueoUseCase useCase) {
        this.useCase = useCase;
    }

    /** Abre y bloquea el acertijo para el userId. */
    @PostMapping("/{acertijoId}/bloqueo")
    public ResponseEntity<?> abrir(@PathVariable String acertijoId,
                                   @RequestBody Map<String, String> body) {
        try {
            AcertijoDto dto = useCase.ejecutar(acertijoId, body.get("userId"));
            return ResponseEntity.ok(dto);
        } catch (AcertijoBloqueadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (ComponenteSinAcertijoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Libera el bloqueo (cerrar sin resolver o resolver). */
    @DeleteMapping("/{acertijoId}/bloqueo")
    public ResponseEntity<Void> cerrar(@PathVariable String acertijoId,
                                       @RequestParam String userId) {
        useCase.cerrar(acertijoId, userId);
        return ResponseEntity.noContent().build();
    }

    /** Libera forzado por desconexión. */
    @DeleteMapping("/{acertijoId}/bloqueo/forzado")
    public ResponseEntity<Void> liberarForzado(@PathVariable String acertijoId) {
        useCase.desconectar(acertijoId);
        return ResponseEntity.noContent().build();
    }
}
