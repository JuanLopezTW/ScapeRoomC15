package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoLlenoException;
import co.eci.c15.salas.domain.EquipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UnirseEquipoUseCaseTest {

    private EquipoRepository repo;
    private UnirseEquipoUseCase useCase;
    private Equipo equipo;

    @BeforeEach
    void setUp() {
        repo = mock(EquipoRepository.class);
        useCase = new UnirseEquipoUseCase(repo);
        equipo = Equipo.crear("sala-1", 1, 3);
        when(repo.findById(equipo.getId())).thenReturn(Optional.of(equipo));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unionExitosa() {
        EquipoDto dto = useCase.ejecutar(equipo.getId(), "user-1");
        assertTrue(dto.miembros().contains("user-1"));
        verify(repo).save(equipo);
    }

    @Test
    void equipoLlenoLanzaExcepcion() {
        equipo.unirMiembro("u1");
        equipo.unirMiembro("u2");
        equipo.unirMiembro("u3");
        assertThrows(EquipoLlenoException.class, () -> useCase.ejecutar(equipo.getId(), "u4"));
        verify(repo, never()).save(any());
    }

    @Test
    void equipoInexistenteLanzaExcepcion() {
        when(repo.findById("no-existe")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar("no-existe", "user-1"));
    }
}
