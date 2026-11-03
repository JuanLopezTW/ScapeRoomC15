package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.AsignacionRoles;
import co.eci.c15.gameplay.domain.Rol;
import co.eci.c15.gameplay.domain.RolesRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class RolesRedis implements RolesRepository {

    private final EstadoPartidaRedis estado;

    public RolesRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "roles");
    }

    /** Devuelve la misma asignacion si quedo guardada (quien la propuso es quien avisa los roles). */
    @Override
    public AsignacionRoles saveIfAbsent(AsignacionRoles asignacion) {
        if (estado.guardarSiNoExiste(clave(asignacion.matchId()), asignacion.equipoId(), asignacion)) return asignacion;
        return findByEquipo(asignacion.matchId(), asignacion.equipoId()).orElse(asignacion);
    }

    @Override
    public Optional<AsignacionRoles> findByEquipo(String matchId, String equipoId) {
        return estado.leer(clave(matchId), equipoId, AsignacionRoles.class);
    }

    @Override
    public Optional<Rol> findRol(String matchId, String userId) {
        return estado.todos(clave(matchId), AsignacionRoles.class).stream()
                .map(a -> a.roles().get(userId))
                .filter(Objects::nonNull)
                .findFirst();
    }
}
