package co.eci.c15.auth.domain;

import java.util.Optional;

public interface UsuarioRepository {
    boolean existsByUsername(String username);
    Usuario save(Usuario usuario);
    Optional<Usuario> findByUsername(String username);
}
