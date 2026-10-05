package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SalaRepositoryAdapter implements SalaRepository {

    private final SalaJpaRepository jpa;

    public SalaRepositoryAdapter(SalaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Sala save(Sala sala) {
        jpa.save(new SalaJpa(sala.getId(), sala.getNombre(), sala.getAnfitrionId(), sala.getEstado()));
        return sala;
    }

    @Override
    public Optional<Sala> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Sala> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private Sala toDomain(SalaJpa e) {
        Sala sala = Sala.crear(e.getNombre(), e.getAnfitrionId());
        if (e.getEstado() == Sala.Estado.EN_PARTIDA) sala.iniciarPartida();
        return sala;
    }
}
