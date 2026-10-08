package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.JugadorNoEnSalaException;
import co.eci.c15.salas.domain.PartidaYaIniciadaException;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnirseEquipoUseCase {

    private final EquipoRepository equipos;
    private final SalaRepository salas;

    public UnirseEquipoUseCase(EquipoRepository equipos, SalaRepository salas) {
        this.equipos = equipos;
        this.salas = salas;
    }

    /**
     * Une al jugador al equipo. El jugador debe estar en la sala del equipo.
     * Si ya estaba en otro equipo de la misma sala, se cambia de equipo.
     */
    @Transactional
    public EquipoDto ejecutar(String equipoId, String userId) {
        Equipo equipo = equipos.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + equipoId));
        Sala sala = salas.findById(equipo.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada: " + equipo.getSalaId()));
        if (!sala.isDisponible()) throw new PartidaYaIniciadaException(sala.getId());
        if (!sala.contieneJugador(userId)) throw new JugadorNoEnSalaException(userId, sala.getId());
        if (equipo.contieneMiembro(userId)) return EquipoDto.from(equipo);

        equipo.unirMiembro(userId);
        for (Equipo anterior : equipos.findBySalaId(sala.getId())) {
            if (!anterior.getId().equals(equipo.getId()) && anterior.contieneMiembro(userId)) {
                anterior.quitarMiembro(userId);
                equipos.save(anterior);
            }
        }
        equipos.save(equipo);
        return EquipoDto.from(equipo);
    }
}