package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisualizarAcertijoUseCaseTest {

    private AcertijoRepository acertijoRepo;
    private ElementoInteractivoRepository elementoRepo;
    private VisualizarAcertijoUseCase useCase;

    @BeforeEach
    void setUp() {
        acertijoRepo = mock(AcertijoRepository.class);
        elementoRepo = mock(ElementoInteractivoRepository.class);
        useCase = new VisualizarAcertijoUseCase(acertijoRepo, elementoRepo);
    }

    @Test
    void visualizaEnunciadoYElementos() {
        Acertijo a = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        List<ElementoInteractivo> elems = List.of(
                new ElementoInteractivo("e1", "a1", "PISTA", "Mira el cuadro"),
                new ElementoInteractivo("e2", "a1", "OBJETO", "Llave oxidada")
        );
        when(acertijoRepo.findById("a1")).thenReturn(Optional.of(a));
        when(elementoRepo.findByAcertijoId("a1")).thenReturn(elems);

        VisualizarAcertijoDto dto = useCase.ejecutar("a1");

        assertEquals("¿Cuánto es 2+2?", dto.enunciado());
        assertEquals("PENDIENTE", dto.estado());
        assertFalse(dto.resuelto());
        assertTrue(dto.puedeEnviarSolucion());
        assertEquals(2, dto.elementos().size());
    }

    @Test
    void acertijoResueltNoPermiteEnviarSolucion() {
        Acertijo a = new Acertijo("a1", "comp-1", "¿Cuánto es 2+2?");
        a.marcarResuelto();
        when(acertijoRepo.findById("a1")).thenReturn(Optional.of(a));
        when(elementoRepo.findByAcertijoId("a1")).thenReturn(List.of());

        VisualizarAcertijoDto dto = useCase.ejecutar("a1");

        assertTrue(dto.resuelto());
        assertFalse(dto.puedeEnviarSolucion());
        assertEquals("RESUELTO", dto.estado());
    }

    @Test
    void acertijoInexistenteLanzaExcepcion() {
        when(acertijoRepo.findById("no-existe")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar("no-existe"));
    }
}
