package co.eci.c15.auth.application;

import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsernameInvalidoException;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IniciarSesionUseCaseTest {

    private UsuarioRepository repo;
    private IniciarSesionUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        useCase = new IniciarSesionUseCase(repo);
    }

    @Test
    void ingresoExitoso() {
        when(repo.existsByUsername("jugador1")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SesionDto sesion = useCase.ejecutar("jugador1");

        assertEquals("jugador1", sesion.username());
        assertNotNull(sesion.userId());
        verify(repo).save(any(Usuario.class));
    }

    @Test
    void usernameVacioLanzaExcepcion() {
        assertThrows(UsernameInvalidoException.class, () -> useCase.ejecutar(""));
        verifyNoInteractions(repo);
    }

    @Test
    void usernameEnUsoLanzaExcepcion() {
        when(repo.existsByUsername("jugador1")).thenReturn(true);
        assertThrows(UsernameEnUsoException.class, () -> useCase.ejecutar("jugador1"));
        verify(repo, never()).save(any());
    }
}
