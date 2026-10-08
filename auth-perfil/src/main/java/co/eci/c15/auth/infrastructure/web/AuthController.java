package co.eci.c15.auth.infrastructure.web;

import co.eci.c15.auth.application.IniciarSesionUseCase;
import co.eci.c15.auth.application.SesionDto;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsernameInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesion;

    public AuthController(IniciarSesionUseCase iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            SesionDto sesion = iniciarSesion.ejecutar(body.get("username"));
            return ResponseEntity.ok(sesion);
        } catch (UsernameInvalidoException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UsernameEnUsoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
