package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(SalaRepositoryAdapter.class)
class SalaRepositoryAdapterTest {

    @Autowired
    private SalaRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void conservaIdConfiguracionYJugadores() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.configurar("anfitrion-1", 3, 5);
        sala.unirJugador("user-1");
        sala.unirJugador("user-2");
        adapter.save(sala);
        em.flush();
        em.clear();

        Sala leida = adapter.findById(sala.getId()).orElseThrow();
        assertEquals(sala.getId(), leida.getId());
        assertEquals("Sala 1", leida.getNombre());
        assertEquals("anfitrion-1", leida.getAnfitrionId());
        assertEquals(3, leida.getNumEquipos());
        assertEquals(5, leida.getJugadoresPorEquipo());
        assertEquals(Set.of("user-1", "user-2"), leida.getJugadores());
    }

    @Test
    void guardarDosVecesActualizaLaMismaFila() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        adapter.save(sala);

        Sala leida = adapter.findById(sala.getId()).orElseThrow();
        leida.unirJugador("user-1");
        adapter.save(leida);

        assertEquals(1, adapter.findAll().size());
        assertEquals(Set.of("user-1"), adapter.findById(sala.getId()).orElseThrow().getJugadores());
    }

    @Test
    void conservaEstadoEnPartida() {
        Sala sala = Sala.crear("Sala 1", "anfitrion-1");
        sala.iniciarPartida();
        adapter.save(sala);

        assertEquals(Sala.Estado.EN_PARTIDA, adapter.findById(sala.getId()).orElseThrow().getEstado());
    }
}