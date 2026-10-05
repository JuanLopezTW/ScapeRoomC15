package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.ConfigurarSalaUseCase;
import co.eci.c15.salas.application.CrearSalaUseCase;
import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.SalaDto;
import co.eci.c15.salas.application.UnirseSalaUseCase;
import co.eci.c15.salas.domain.ConfiguracionInvalidaException;
import co.eci.c15.salas.domain.NoEsAnfitrionException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.SalaLlenaException;
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
    private final ConfigurarSalaUseCase configurar;
    private final UnirseSalaUseCase unirse;

    public SalaController(CrearSalaUseCase crear, ListarSalasUseCase listar,
                          ConfigurarSalaUseCase configurar, UnirseSalaUseCase unirse) {
        this.crear = crear;
        this.listar = listar;
        this.configurar = configurar;
        this.unirse = unirse;
    }

    @GetMapping
    public List<SalaDto> catalogo() {
        return listar.ejecutar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(crear.ejecutar(body.get("nombre"), body.get("anfitrionId")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{salaId}/configuracion")
    public ResponseEntity<?> configurar(@PathVariable String salaId, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(configurar.ejecutar(salaId, (String) body.get("solicitanteId"),
                    (int) body.get("numEquipos"), (int) body.get("jugadoresPorEquipo")));
        } catch (NoEsAnfitrionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (ConfiguracionInvalidaException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{salaId}/jugadores")
    public ResponseEntity<?> unirse(@PathVariable String salaId, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(unirse.ejecutar(salaId, body.get("userId")));
        } catch (SalaLlenaException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (PartidaYaIniciadaException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
