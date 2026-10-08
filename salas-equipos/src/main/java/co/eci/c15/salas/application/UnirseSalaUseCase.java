package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UnirseSalaUseCase {

    private final SalaRepository salas;
    private final ApplicationEventPublisher events;

    public UnirseSalaUseCase(SalaRepository salas, ApplicationEventPublisher events) {
        this.salas = salas;
        this.events = events;
    }

    public SalaDto ejecutar(String salaId, String userId) {
        Sala sala = salas.findById(salaId)
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada: " + salaId));
        sala.unirJugador(userId);
        salas.save(sala);
        events.publishEvent(new SalaActualizadaEvent(sala.getId()));
        return SalaDto.from(sala);
    }
}
