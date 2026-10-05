package co.eci.c15.salas.domain;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository {
    Equipo save(Equipo equipo);
    Optional<Equipo> findById(String id);
    List<Equipo> findBySalaId(String salaId);
}
