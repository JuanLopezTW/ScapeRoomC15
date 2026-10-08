package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearSalaUseCase {

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ApplicationEventPublisher events;

    public CrearSalaUseCase(SalaRepository salas, EquipoRepository equipos, ApplicationEventPublisher events) {
        this.salas = salas;
        this.equipos = equipos;
        this.events = events;
    }

    /** Crea la sala con la configuración por defecto y sus equipos vacíos. */
    @Transactional
    public SalaDto ejecutar(String nombre, String anfitrionId) {
        Sala sala = salas.save(Sala.crear(nombre, anfitrionId));
        for (int numero = 1; numero <= sala.getNumEquipos(); numero++) {
            equipos.save(Equipo.crear(sala.getId(), numero, sala.getJugadoresPorEquipo()));
        }
        events.publishEvent(new SalaCreadaEvent(sala.getId(), sala.getNombre()));
        return SalaDto.from(sala);
    }
}