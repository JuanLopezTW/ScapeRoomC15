package co.eci.c15.gameplay.application;

import co.eci.c15.common.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.ProgresoEquipo;
import co.eci.c15.gameplay.domain.ProgresoRepository;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Progreso de puzzles en vivo (HU-33.5). Cada acertijo resuelto suma uno al equipo, sobre el
 * total de acertijos del mapa de la partida, y todos reciben la foto actualizada de los equipos.
 * Los cambios de una misma partida se aplican y avisan de a uno, para que nadie reciba un
 * estado viejo despues de uno nuevo.
 */
@Service
public class ProgresoPartidaUseCase {

    private final ProgresoRepository progresos;
    private final ObtenerMapaUseCase obtenerMapa;
    private final ProgresoNotifier notifier;
    private final Map<String, Object> bloqueos = new ConcurrentHashMap<>();

    public ProgresoPartidaUseCase(ProgresoRepository progresos, ObtenerMapaUseCase obtenerMapa, ProgresoNotifier notifier) {
        this.progresos = progresos;
        this.obtenerMapa = obtenerMapa;
        this.notifier = notifier;
    }

    /** Al iniciar la partida todos los equipos aparecen en 0 sobre el total del mapa. */
    public void iniciar(String matchId, Collection<String> equipoIds) {
        synchronized (bloqueo(matchId)) {
            int total = totalAcertijos(matchId);
            equipoIds.forEach(equipoId -> progresos.obtener(matchId, equipoId, total));
            notifier.notificar(foto(matchId));
        }
    }

    /** Suma el acertijo al equipo; si ya estaba contado no se vuelve a avisar. */
    public void acertijoResuelto(AcertijoResueltoEvent evento) {
        synchronized (bloqueo(evento.matchId())) {
            ProgresoEquipo progreso = progresos.obtener(evento.matchId(), evento.equipoId(), totalAcertijos(evento.matchId()));
            if (progreso.registrarResuelto(evento.componenteMapaId())) {
                notifier.notificar(foto(evento.matchId()));
            }
        }
    }

    public ProgresoPartidaDto consultar(String matchId) {
        return foto(matchId);
    }

    private ProgresoPartidaDto foto(String matchId) {
        return ProgresoPartidaDto.from(matchId, progresos.findByMatchId(matchId));
    }

    private int totalAcertijos(String matchId) {
        return (int) obtenerMapa.obtener(matchId).getComponentes().stream()
                .filter(c -> c.tipo() == TipoComponente.ACERTIJO).count();
    }

    private Object bloqueo(String matchId) {
        return bloqueos.computeIfAbsent(matchId, id -> new Object());
    }
}
