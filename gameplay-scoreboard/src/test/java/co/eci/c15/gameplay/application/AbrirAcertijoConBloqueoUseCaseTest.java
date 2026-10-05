package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbrirAcertijoConBloqueoUseCaseTest {

    private AcertijoRepository repo;
    private AcertijoLockService locks;
    private AbrirAcertijoConBloqueoUseCase useCase;
    private Acertijo acertijo;

    @BeforeEach
    void setUp() {
        repo = mock(AcertijoRepository.class);
        locks = new AcertijoLockService(); // real, no mock
        useCase = new AbrirAcertijoConBloqueoUseCase(repo, locks);
        acertijo = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        when(repo.findById("a1")).thenReturn(Optional.of(acertijo));
    }

    @Test
    void primerJugadorAbreYBloquea() {
        AcertijoDto dto = useCase.ejecutar("a1", "user-1");
        assertEquals("a1", dto.id());
        assertEquals("user-1", locks.propietario("a1").orElseThrow());
    }

    @Test
    void segundoJugadorRechazo() {
        useCase.ejecutar("a1", "user-1");
        assertThrows(AcertijoBloqueadoException.class, () -> useCase.ejecutar("a1", "user-2"));
    }

    @Test
    void cerrarLiberaParaOtro() {
        useCase.ejecutar("a1", "user-1");
        useCase.cerrar("a1", "user-1");
        assertDoesNotThrow(() -> useCase.ejecutar("a1", "user-2"));
    }

    @Test
    void desconexionLiberaForzado() {
        useCase.ejecutar("a1", "user-1");
        useCase.desconectar("a1");
        assertDoesNotThrow(() -> useCase.ejecutar("a1", "user-2"));
    }
}
