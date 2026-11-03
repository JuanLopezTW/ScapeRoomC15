package co.eci.c15.gameplay.infrastructure.estado;

import co.eci.c15.gameplay.domain.InventarioEquipo;
import co.eci.c15.gameplay.domain.InventarioRepository;
import co.eci.c15.gameplay.domain.ObjetoInventario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class InventarioRedis implements InventarioRepository {

    record Guardado(String matchId, String equipoId, List<ObjetoInventario> objetos) {
        static Guardado de(InventarioEquipo i) {
            return new Guardado(i.getMatchId(), i.getEquipoId(), i.getObjetos());
        }

        InventarioEquipo restaurar() {
            return InventarioEquipo.restaurar(matchId, equipoId, objetos == null ? List.of() : objetos);
        }
    }

    private final EstadoPartidaRedis estado;

    public InventarioRedis(EstadoPartidaRedis estado) {
        this.estado = estado;
    }

    private static String clave(String matchId) {
        return EstadoPartidaRedis.clave(matchId, "inventarios");
    }

    /** Un inventario vacio no se guarda: se crea al vuelo hasta que el equipo recolecte algo. */
    @Override
    public InventarioEquipo obtener(String matchId, String equipoId) {
        return estado.leer(clave(matchId), equipoId, Guardado.class).map(Guardado::restaurar)
                .orElseGet(() -> new InventarioEquipo(matchId, equipoId));
    }

    @Override
    public void guardar(InventarioEquipo inventario) {
        estado.guardar(clave(inventario.getMatchId()), inventario.getEquipoId(), Guardado.de(inventario));
    }
}
