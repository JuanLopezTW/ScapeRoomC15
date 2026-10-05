package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.CrearSalaUseCase;
import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.SalaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final CrearSalaUseCase crear;
    private final ListarSalasUseCase listar;

    public SalaController(CrearSalaUseCase crear, ListarSalasUseCase listar) {
        this.crear = crear;
        this.listar = listar;
    }

    @GetMapping
    public List<SalaDto> catalogo() {
        return listar.ejecutar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        try {
            SalaDto sala = crear.ejecutar(body.get("nombre"), body.get("anfitrionId"));
            return ResponseEntity.status(HttpStatus.CREATED).body(sala);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
