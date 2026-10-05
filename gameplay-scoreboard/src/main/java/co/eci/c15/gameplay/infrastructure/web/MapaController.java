package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AbrirAcertijoUseCase;
import co.eci.c15.gameplay.application.AcertijoDto;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mapa")
public class MapaController {

    private final AbrirAcertijoUseCase abrirAcertijo;

    public MapaController(AbrirAcertijoUseCase abrirAcertijo) {
        this.abrirAcertijo = abrirAcertijo;
    }

    @GetMapping("/componentes/{componenteMapaId}/acertijo")
    public ResponseEntity<?> abrirAcertijo(@PathVariable String componenteMapaId) {
        try {
            AcertijoDto dto = abrirAcertijo.ejecutar(componenteMapaId);
            return ResponseEntity.ok(dto);
        } catch (ComponenteSinAcertijoException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
