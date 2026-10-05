package co.eci.c15.gameplay.domain;

import java.util.Optional;

public interface AcertijoRepository {
    Optional<Acertijo> findById(String id);
    Optional<Acertijo> findByComponenteMapaId(String componenteMapaId);
    Acertijo save(Acertijo acertijo);
}
