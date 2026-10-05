package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.EquipoDto;
import co.eci.c15.salas.application.UnirseEquipoUseCase;
import co.eci.c15.salas.domain.EquipoLlenoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final UnirseEquipoUseCase unirse;

    public EquipoController(UnirseEquipoUseCase unirse) {
        this.unirse = unirse;
    }

    @PostMapping("/{equipoId}/miembros")
    public ResponseEntity<?> unirse(@PathVariable String equipoId, @RequestBody Map<String, String> body) {
        try {
            EquipoDto dto = unirse.ejecutar(equipoId, body.get("userId"));
            return ResponseEntity.ok(dto);
        } catch (EquipoLlenoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
