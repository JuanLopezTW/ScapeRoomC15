package co.eci.c15.salas.application;

import co.eci.c15.salas.domain.Equipo;
import co.eci.c15.salas.domain.EquipoRepository;
import co.eci.c15.salas.domain.Sala;
import co.eci.c15.salas.domain.SalaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Un jugador deja la sala mientras sigue en el lobby (hoy: al cerrar la pagina). Si es el
 * anfitrion, la sala se cierra: se borran la sala y sus equipos y se avisa a los demas, para que
 * no queden lobbys fantasma. Con la partida ya iniciada no hace nada (no hay reconexion).
 */
@Service
public class SalirDeSalaUseCase {

    public enum Resultado { SALIO, SALA_CERRADA, NADA }

    private final SalaRepository salas;
    private final EquipoRepository equipos;
    private final ArranquePartidaNotifier notifier;
    private final ApplicationEventPublisher events;

    public SalirDeSalaUseCase(SalaRepository salas, EquipoRepository equipos,
                              ArranquePartidaNotifier notifier, ApplicationEventPublisher events) {
        this.salas = salas;
        this.equipos = equipos;
        this.notifier = notifier;
        this.events = events;
    }

    @Transactional
    public Resultado ejecutar(String salaId, String userId) {
        Optional<Sala> encontrada = salas.findById(salaId);
        if (encontrada.isEmpty()) return Resultado.NADA;
        Sala sala = encontrada.get();
        if (!sala.isDisponible()) return Resultado.NADA;

        if (sala.esAnfitrion(userId)) {
            equipos.findBySalaId(salaId).stream().map(Equipo::getId).forEach(equipos::delete);
            salas.delete(salaId);
            notifier.cerrada(salaId);
            events.publishEvent(new SalaActualizadaEvent(salaId));
            return Resultado.SALA_CERRADA;
        }

        if (!sala.contieneJugador(userId)) return Resultado.NADA;
        equipos.findBySalaId(salaId).stream()
                .filter(e -> e.contieneMiembro(userId))
                .forEach(e -> {
                    e.quitarMiembro(userId);
                    equipos.save(e);
                });
        sala.retirarJugador(userId);
        salas.save(sala);
        events.publishEvent(new SalaActualizadaEvent(salaId));
        return Resultado.SALIO;
    }
}
