package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrearSalaUseCaseTest {

    private SalaRepository repo;
    private ApplicationEventPublisher events;
    private CrearSalaUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new CrearSalaUseCase(repo, events);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creaYPublicaEvento() {
        SalaDto dto = useCase.ejecutar("Sala 1", "user-1");
        assertEquals("Sala 1", dto.nombre());
        assertEquals("DISPONIBLE", dto.estado());
        verify(events).publishEvent(any(SalaCreadaEvent.class));
    }

    @Test
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar("", "user-1"));
        verifyNoInteractions(events);
    }
}
