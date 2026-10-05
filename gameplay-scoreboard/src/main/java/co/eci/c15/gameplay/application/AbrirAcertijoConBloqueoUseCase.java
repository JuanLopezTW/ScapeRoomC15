package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import org.springframework.stereotype.Service;

@Service
public class AbrirAcertijoConBloqueoUseCase {

    private final AcertijoRepository acertijos;
    private final AcertijoLockService locks;

    public AbrirAcertijoConBloqueoUseCase(AcertijoRepository acertijos, AcertijoLockService locks) {
        this.acertijos = acertijos;
        this.locks = locks;
    }

    public AcertijoDto ejecutar(String acertijoId, String userId) {
        var acertijo = acertijos.findById(acertijoId)
                .orElseThrow(() -> new ComponenteSinAcertijoException(acertijoId));

        if (!locks.bloquear(acertijoId, userId)) {
            throw new AcertijoBloqueadoException(acertijoId);
        }

        return AcertijoDto.from(acertijo);
    }

    public void cerrar(String acertijoId, String userId) {
        locks.liberar(acertijoId, userId);
    }

    public void desconectar(String acertijoId) {
        locks.liberarForzado(acertijoId);
    }
}
