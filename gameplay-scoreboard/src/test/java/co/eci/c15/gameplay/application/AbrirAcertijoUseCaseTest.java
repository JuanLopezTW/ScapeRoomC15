package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbrirAcertijoUseCaseTest {

    private AcertijoRepository repo;
    private AbrirAcertijoUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(AcertijoRepository.class);
        useCase = new AbrirAcertijoUseCase(repo);
    }

    @Test
    void retornaAcertijoDelComponente() {
        Acertijo a = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        when(repo.findByComponenteMapaId("comp-1")).thenReturn(Optional.of(a));

        AcertijoDto dto = useCase.ejecutar("comp-1");

        assertEquals("a1", dto.id());
        assertEquals("¿Cuánto es 2+2?", dto.enunciado());
        assertEquals("PENDIENTE", dto.estado());
    }

    @Test
    void componenteSinAcertijoLanzaExcepcion() {
        when(repo.findByComponenteMapaId("comp-vacio")).thenReturn(Optional.empty());
        assertThrows(ComponenteSinAcertijoException.class, () -> useCase.ejecutar("comp-vacio"));
    }
}
