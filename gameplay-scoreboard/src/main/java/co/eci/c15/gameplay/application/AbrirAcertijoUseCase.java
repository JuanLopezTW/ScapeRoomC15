package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.AcertijoEnPartidaRepository;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteMapa;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.TipoComponente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abrir y cerrar acertijos del mapa (HU-27.5, 35.5, 66.5). Cada equipo tiene su propio estado
 * de cada acertijo; mientras un jugador lo tiene abierto, el resto de su equipo no puede abrirlo
 * y él no puede moverse. El bloqueo se libera al cerrar, si el jugador sale de la partida
 * (desconexión) o tras {@link #TTL_BLOQUEO} como respaldo.
 */
@Service
public class AbrirAcertijoUseCase {

    public static final Duration TTL_BLOQUEO = Duration.ofMinutes(5);

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;
    private final AcertijoRepository acertijos;
    private final ElementoInteractivoRepository elementos;
    private final AcertijoEnPartidaRepository estados;
    private final Clock clock;

    @Autowired
    public AbrirAcertijoUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa,
                                AcertijoRepository acertijos, ElementoInteractivoRepository elementos,
                                AcertijoEnPartidaRepository estados) {
        this(jugadores, obtenerMapa, acertijos, elementos, estados, Clock.systemUTC());
    }

    AbrirAcertijoUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa,
                         AcertijoRepository acertijos, ElementoInteractivoRepository elementos,
                         AcertijoEnPartidaRepository estados, Clock clock) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
        this.acertijos = acertijos;
        this.elementos = elementos;
        this.estados = estados;
        this.clock = clock;
    }

    public VisualizarAcertijoDto ejecutar(String matchId, String componenteMapaId, String userId) {
        JugadorEnMapa jugador = jugadores.find(matchId, userId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
        ComponenteMapa componente = obtenerMapa.obtener(matchId).getComponentes().stream()
                .filter(c -> c.id().equals(componenteMapaId) && c.tipo() == TipoComponente.ACERTIJO)
                .findFirst()
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        Acertijo acertijo = acertijos.findByComponenteMapaId(componenteMapaId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
        AcertijoEnPartida estado = estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId);

        if (!estado.isResuelto()) {
            AtomicReference<Optional<String>> desplazado = new AtomicReference<>(Optional.empty());
            jugador.abrirAcertijo(componente, () -> desplazado.set(
                    estado.bloquear(userId, clock.instant(), TTL_BLOQUEO,
                            poseedor -> jugadores.find(matchId, poseedor).isPresent())));
            // Si el bloqueo venció y se lo quitamos a otro jugador, él puede volver a moverse.
            desplazado.get()
                    .flatMap(anterior -> jugadores.find(matchId, anterior))
                    .ifPresent(anterior -> anterior.cerrarAcertijo(componenteMapaId));
        }
        return VisualizarAcertijoDto.from(acertijo, estado, elementos.findByAcertijoId(acertijo.getId()));
    }

    /** Cierra el acertijo: libera el bloqueo (si era suyo) y el jugador puede volver a moverse. */
    public void cerrar(String matchId, String componenteMapaId, String userId) {
        JugadorEnMapa jugador = jugadores.find(matchId, userId)
                .orElseThrow(() -> new JugadorNoRegistradoException(matchId, userId));
        estados.obtener(matchId, jugador.getEquipoId(), componenteMapaId).liberar(userId);
        jugador.cerrarAcertijo(componenteMapaId);
    }
}
