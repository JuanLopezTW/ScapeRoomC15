package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.CuentaRegresivaPartida;
import co.eci.c15.salas.application.SalaActualizadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Cada cambio en la sala revisa si ya puede arrancar la partida (HU-64.4). */
@Component
public class ArranqueAutomaticoListener {

    private final CuentaRegresivaPartida cuentaRegresiva;

    public ArranqueAutomaticoListener(CuentaRegresivaPartida cuentaRegresiva) {
        this.cuentaRegresiva = cuentaRegresiva;
    }

    @EventListener
    public void onSalaActualizada(SalaActualizadaEvent event) {
        cuentaRegresiva.evaluar(event.salaId());
    }
}
