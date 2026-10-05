package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ConfigurarSalaUseCase {

    private final SalaRepository salas;
    private final ApplicationEventPublisher events;

    public ConfigurarSalaUseCase(SalaRepository salas, ApplicationEventPublisher events) {
        this.salas = salas;
        this.events = events;
    }

    public SalaDto ejecutar(String salaId, String solicitanteId, int numEquipos, int jugadoresPorEquipo) {
        Sala sala = salas.findById(salaId)
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada: " + salaId));
        sala.configurar(solicitanteId, numEquipos, jugadoresPorEquipo);
        salas.save(sala);
        events.publishEvent(new SalaActualizadaEvent(sala.getId()));
        return SalaDto.from(sala);
    }
}
