package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.NoEsAnfitrionException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigurarSalaUseCaseTest {

    private SalaRepository repo;
    private ApplicationEventPublisher events;
    private ConfigurarSalaUseCase useCase;
    private Sala sala;

    @BeforeEach
    void setUp() {
        repo = mock(SalaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        useCase = new ConfigurarSalaUseCase(repo, events);
        sala = Sala.crear("Sala 1", "anfitrion-1");
        when(repo.findById(sala.getId())).thenReturn(Optional.of(sala));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void anfitrionConfiguraYPublicaEvento() {
        SalaDto dto = useCase.ejecutar(sala.getId(), "anfitrion-1", 3, 5);
        assertEquals(3, dto.numEquipos());
        assertEquals(5, dto.jugadoresPorEquipo());
        verify(events).publishEvent(any(SalaActualizadaEvent.class));
    }

    @Test
    void noAnfitrionLanzaExcepcion() {
        assertThrows(NoEsAnfitrionException.class,
                () -> useCase.ejecutar(sala.getId(), "otro-user", 3, 5));
        verify(repo, never()).save(any());
    }

    @Test
    void salaInexistenteLanzaExcepcion() {
        when(repo.findById("no-existe")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.ejecutar("no-existe", "anfitrion-1", 2, 4));
    }
}
