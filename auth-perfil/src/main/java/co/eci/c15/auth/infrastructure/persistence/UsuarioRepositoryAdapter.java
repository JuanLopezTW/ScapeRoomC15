package co.eci.c15.auth.infrastructure.persistence;

import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public Usuario save(Usuario usuario) {
        jpa.save(new UsuarioJpa(usuario.getId(), usuario.getUsername()));
        return usuario;
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return jpa.findByUsername(username)
                .map(e -> Usuario.crear(e.getUsername()));
    }
}
