package co.eci.c15.salas.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cuenta regresiva antes de arrancar la partida (HU-64.4). Cuando la sala queda lista se
 * programa el inicio; si deja de estarlo antes de que termine (alguien se desmarca, entra un
 * jugador nuevo, etc.) se cancela.
 */
@Service
public class CuentaRegresivaPartida {

    private static final Logger log = LoggerFactory.getLogger(CuentaRegresivaPartida.class);

    private final IniciarPartidaUseCase iniciarPartida;
    private final TaskScheduler scheduler;
    private final ArranquePartidaNotifier notifier;
    private final Duration espera;
    /** Cuentas en curso por sala. Protegido por {@code this}. */
    private final Map<String, ScheduledFuture<?>> pendientes = new HashMap<>();

    public CuentaRegresivaPartida(IniciarPartidaUseCase iniciarPartida,
                                  @Qualifier("salasScheduler") TaskScheduler scheduler,
                                  ArranquePartidaNotifier notifier,
                                  @Value("${c15.partida.cuenta-regresiva:5s}") Duration espera) {
        this.iniciarPartida = iniciarPartida;
        this.scheduler = scheduler;
        this.notifier = notifier;
        this.espera = espera;
    }

    public synchronized void evaluar(String salaId) {
        boolean lista = iniciarPartida.listaParaIniciar(salaId);
        ScheduledFuture<?> enCurso = pendientes.get(salaId);
        if (lista && enCurso == null) {
            AtomicReference<ScheduledFuture<?>> propia = new AtomicReference<>();
            ScheduledFuture<?> tarea = scheduler.schedule(() -> alTerminar(salaId, propia),
                    scheduler.getClock().instant().plus(espera));
            propia.set(tarea);
            pendientes.put(salaId, tarea);
            notifier.cuentaRegresiva(salaId, espera.toSeconds());
        } else if (!lista && enCurso != null) {
            enCurso.cancel(false);
            pendientes.remove(salaId);
            notifier.cancelada(salaId);
        }
    }

    public synchronized boolean enCurso(String salaId) {
        return pendientes.containsKey(salaId);
    }

    void alTerminar(String salaId, AtomicReference<ScheduledFuture<?>> propia) {
        synchronized (this) {
            if (!pendientes.remove(salaId, propia.get())) return;
        }
        try {
            iniciarPartida.iniciar(salaId).ifPresentOrElse(
                    matchId -> notifier.iniciada(salaId, matchId),
                    () -> notifier.cancelada(salaId));
        } catch (RuntimeException e) {
            log.error("No se pudo iniciar la partida de la sala {}", salaId, e);
            notifier.cancelada(salaId);
        }
    }
}
