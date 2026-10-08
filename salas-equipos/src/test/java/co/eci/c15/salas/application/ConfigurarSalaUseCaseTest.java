package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.ConfiguracionInvalidaException;
import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.NoEsAnfitrionException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigurarSalaUseCaseTest {

    private SalaRepository repo;
    private EquiposEnMemoria equipos;
    private ApplicationEventPublisher events;
    private ConfigurarSalaUseCase useCase;
    private Sala sala;

    @BeforeEach
    void setUp() {
        repo = mock(SalaRepository.class);
        equipos = new EquiposEnMemoria();
        events = mock(ApplicationEventPublisher.class);
        useCase = new ConfigurarSalaUseCase(repo, equipos, events);
        sala = Sala.crear("Sala 1", "anfitrion-1");
        equipos.save(Equipo.crear(sala.getId(), 1, 4));
        equipos.save(Equipo.crear(sala.getId(), 2, 4));
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
    void masEquiposCreaLosQueFaltanConElNuevoCupo() {
        useCase.ejecutar(sala.getId(), "anfitrion-1", 3, 5);
        List<Equipo> resultado = equipos.findBySalaId(sala.getId());
        assertEquals(List.of(1, 2, 3), resultado.stream().map(Equipo::getNumero).toList());
        assertTrue(resultado.stream().allMatch(e -> e.getCupoMaximo() == 5));
    }

    @Test
    void menosEquiposEliminaLosSobrantesVacios() {
        useCase.ejecutar(sala.getId(), "anfitrion-1", 3, 4);
        useCase.ejecutar(sala.getId(), "anfitrion-1", 2, 4);
        assertEquals(List.of(1, 2), equipos.findBySalaId(sala.getId()).stream().map(Equipo::getNumero).toList());
    }

    @Test
    void noSeQuitaUnEquipoConJugadores() {
        useCase.ejecutar(sala.getId(), "anfitrion-1", 3, 4);
        sala.unirJugador("user-1");
        equipos.findBySalaId(sala.getId()).get(2).unirMiembro("user-1");

        assertThrows(ConfiguracionInvalidaException.class,
                () -> useCase.ejecutar(sala.getId(), "anfitrion-1", 2, 4));
        assertEquals(3, equipos.findBySalaId(sala.getId()).size());
    }

    @Test
    void cupoNoBajaDeLosMiembrosDeUnEquipo() {
        for (int i = 1; i <= 3; i++) {
            sala.unirJugador("user-" + i);
            equipos.findBySalaId(sala.getId()).get(0).unirMiembro("user-" + i);
        }
        reset(repo);
        when(repo.findById(sala.getId())).thenReturn(Optional.of(sala));

        assertThrows(ConfiguracionInvalidaException.class,
                () -> useCase.ejecutar(sala.getId(), "anfitrion-1", 2, 2));
        verify(repo, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void noAnfitrionLanzaExcepcion() {
        assertThrows(NoEsAnfitrionException.class,
                () -> useCase.ejecutar(sala.getId(), "otro-user", 3, 5));
        verify(repo, never()).save(any());
        assertEquals(2, equipos.findBySalaId(sala.getId()).size());
    }

    @Test
    void salaInexistenteLanzaExcepcion() {
        when(repo.findById("no-existe")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.ejecutar("no-existe", "anfitrion-1", 2, 4));
    }
}