package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CrearSalaUseCase {

    private final SalaRepository salas;
    private final ApplicationEventPublisher events;

    public CrearSalaUseCase(SalaRepository salas, ApplicationEventPublisher events) {
        this.salas = salas;
        this.events = events;
    }

    public SalaDto ejecutar(String nombre, String anfitrionId) {
        Sala sala = salas.save(Sala.crear(nombre, anfitrionId));
        events.publishEvent(new SalaCreadaEvent(sala.getId(), sala.getNombre()));
        return SalaDto.from(sala);
    }
}
