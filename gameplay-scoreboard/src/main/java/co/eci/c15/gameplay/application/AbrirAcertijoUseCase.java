package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import org.springframework.stereotype.Service;

@Service
public class AbrirAcertijoUseCase {

    private final AcertijoRepository acertijos;

    public AbrirAcertijoUseCase(AcertijoRepository acertijos) {
        this.acertijos = acertijos;
    }

    public AcertijoDto ejecutar(String componenteMapaId) {
        return acertijos.findByComponenteMapaId(componenteMapaId)
                .map(AcertijoDto::from)
                .orElseThrow(() -> new ComponenteSinAcertijoException(componenteMapaId));
    }
}
