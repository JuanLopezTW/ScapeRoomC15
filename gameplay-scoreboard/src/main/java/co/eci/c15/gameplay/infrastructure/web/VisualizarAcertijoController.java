package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.VisualizarAcertijoDto;
import co.eci.c15.gameplay.application.VisualizarAcertijoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/acertijos")
public class VisualizarAcertijoController {

    private final VisualizarAcertijoUseCase useCase;

    public VisualizarAcertijoController(VisualizarAcertijoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{acertijoId}")
    public ResponseEntity<?> visualizar(@PathVariable String acertijoId) {
        try {
            VisualizarAcertijoDto dto = useCase.ejecutar(acertijoId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
