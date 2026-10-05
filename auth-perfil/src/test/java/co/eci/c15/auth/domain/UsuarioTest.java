package co.eci.c15.auth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void creaUsuarioValido() {
        Usuario u = Usuario.crear("jugador1");
        assertEquals("jugador1", u.getUsername());
        assertNotNull(u.getId());
    }

    @Test
    void rechazaUsernameVacio() {
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear(""));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear("   "));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear(null));
    }

    @Test
    void rechazaUsernameInvalido() {
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear("ab")); // muy corto
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear("nombre con espacios"));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.crear("nombre@invalido"));
    }

    @Test
    void aceptaUsernameConGuionBajo() {
        assertDoesNotThrow(() -> Usuario.crear("jugador_1"));
    }
}
