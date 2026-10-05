package co.eci.c15.salas.infrastructure.web;

import co.eci.c15.salas.application.ConfigurarSalaUseCase;
import co.eci.c15.salas.application.CrearSalaUseCase;
import co.eci.c15.salas.application.ListarSalasUseCase;
import co.eci.c15.salas.application.SalaDto;
import co.eci.c15.salas.domain.ConfiguracionInvalidaException;
import co.eci.c15.salas.domain.NoEsAnfitrionException;
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

    public SalaController(CrearSalaUseCase crear, ListarSalasUseCase listar, ConfigurarSalaUseCase configurar) {
        this.crear = crear;
        this.listar = listar;
        this.configurar = configurar;
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

    @PatchMapping("/{salaId}/configuracion")
    public ResponseEntity<?> configurar(@PathVariable String salaId,
                                        @RequestBody Map<String, Object> body) {
        try {
            String solicitanteId = (String) body.get("solicitanteId");
            int numEquipos = (int) body.get("numEquipos");
            int jugadoresPorEquipo = (int) body.get("jugadoresPorEquipo");
            SalaDto sala = configurar.ejecutar(salaId, solicitanteId, numEquipos, jugadoresPorEquipo);
            return ResponseEntity.ok(sala);
        } catch (NoEsAnfitrionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (ConfiguracionInvalidaException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
