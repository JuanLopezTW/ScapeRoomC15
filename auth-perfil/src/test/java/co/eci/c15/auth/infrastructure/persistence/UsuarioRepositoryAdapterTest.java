package co.eci.c15.auth.infrastructure.persistence;

import co.eci.c15.auth.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(UsuarioRepositoryAdapter.class)
class UsuarioRepositoryAdapterTest {

    @Autowired
    private UsuarioRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByUsernameConservaElIdGuardado() {
        Usuario usuario = adapter.save(Usuario.crear("jugador1"));
        em.flush();
        em.clear();

        Usuario leido = adapter.findByUsername("jugador1").orElseThrow();
        assertEquals(usuario.getId(), leido.getId());
        assertEquals("jugador1", leido.getUsername());
    }

    @Test
    void existsByUsername() {
        adapter.save(Usuario.crear("jugador1"));
        assertTrue(adapter.existsByUsername("jugador1"));
        assertFalse(adapter.existsByUsername("otro"));
        assertTrue(adapter.findByUsername("otro").isEmpty());
    }
}
