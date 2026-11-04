package co.eci.c15.app.integracion;

import co.eci.c15.common.events.EventosEntreServicios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Los eventos entre servicios, dentro del monolito: se entregan solo si la transaccion de quien los publica
 * confirma, y en un hilo propio, asi las carreras que traera el broker aparecen desde ya. Un solo hilo
 * mantiene el orden (el fin de una partida nunca llega antes que su inicio). Al cortar, esta clase se
 * cambia por la que publique en Kafka.
 */
@Component
public class EventosEntreServiciosAsincronos implements EventosEntreServicios, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(EventosEntreServiciosAsincronos.class);

    private final ApplicationEventPublisher eventos;
    private final ThreadPoolTaskExecutor hilo = new ThreadPoolTaskExecutor();

    public EventosEntreServiciosAsincronos(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
        hilo.setCorePoolSize(1);
        hilo.setMaxPoolSize(1);
        hilo.setThreadNamePrefix("eventos-");
        // Quien escucha queda en la misma traza (y con el mismo userId/matchId en los logs) que quien publico
        hilo.setTaskDecorator(new ContextPropagatingTaskDecorator());
        hilo.setWaitForTasksToCompleteOnShutdown(true);
        hilo.initialize();
    }

    @Override
    public void publicar(Object evento) {
        Runnable entregar = () -> {
            try {
                eventos.publishEvent(evento);
            } catch (RuntimeException e) {
                // Un listener que falla no debe frenar a los demas eventos de la cola
                log.error("Fallo al entregar {}", evento.getClass().getSimpleName(), e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    hilo.execute(entregar);
                }
            });
        } else {
            hilo.execute(entregar);
        }
    }

    /** Si ya entrego todo lo publicado (las pruebas lo usan para esperar a los demas modulos). */
    public boolean entregados() {
        ThreadPoolExecutor cola = hilo.getThreadPoolExecutor();
        return cola.getCompletedTaskCount() == cola.getTaskCount();
    }

    @Override
    public void destroy() {
        hilo.shutdown();
    }
}
