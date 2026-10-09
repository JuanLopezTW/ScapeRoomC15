package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProgresoRepositoryEnMemoria implements ProgresoRepository {

    private final Map<String, Map<String, ProgresoEquipo>> porPartida = new ConcurrentHashMap<>();

    @Override
    public ProgresoEquipo obtener(String matchId, String equipoId, int total) {
        return porPartida.computeIfAbsent(matchId, id -> new ConcurrentHashMap<>())
                .computeIfAbsent(equipoId, id -> new ProgresoEquipo(matchId, equipoId, total));
    }

    @Override
    public List<ProgresoEquipo> findByMatchId(String matchId) {
        Map<String, ProgresoEquipo> partida = porPartida.get(matchId);
        if (partida == null) return List.of();
        return partida.values().stream().sorted(Comparator.comparing(ProgresoEquipo::getEquipoId)).toList();
    }
}
