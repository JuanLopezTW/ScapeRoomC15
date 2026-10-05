package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import org.springframework.stereotype.Service;

@Service
public class UnirseEquipoUseCase {

    private final EquipoRepository equipos;

    public UnirseEquipoUseCase(EquipoRepository equipos) {
        this.equipos = equipos;
    }

    public EquipoDto ejecutar(String equipoId, String userId) {
        Equipo equipo = equipos.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado: " + equipoId));
        equipo.unirMiembro(userId);
        equipos.save(equipo);
        return EquipoDto.from(equipo);
    }
}
