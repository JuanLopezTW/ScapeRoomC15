package co.eci.c15.auth.application;

import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class IniciarSesionUseCase {

    private final UsuarioRepository usuarios;

    public IniciarSesionUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Crea la sesión del usuario. Si el username no existe lo registra; si ya existe y está activo, lanza excepción. */
    public SesionDto ejecutar(String username) {
        // validación de formato lanzada desde el dominio
        Usuario.validarUsername(username);

        if (usuarios.existsByUsername(username)) {
            throw new UsernameEnUsoException(username);
        }

        Usuario usuario = usuarios.save(Usuario.crear(username));
        return new SesionDto(usuario.getId(), usuario.getUsername());
    }
}
