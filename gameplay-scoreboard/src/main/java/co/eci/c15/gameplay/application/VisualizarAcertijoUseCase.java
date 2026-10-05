package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import org.springframework.stereotype.Service;

@Service
public class VisualizarAcertijoUseCase {

    private final AcertijoRepository acertijos;
    private final ElementoInteractivoRepository elementos;

    public VisualizarAcertijoUseCase(AcertijoRepository acertijos, ElementoInteractivoRepository elementos) {
        this.acertijos = acertijos;
        this.elementos = elementos;
    }

    public VisualizarAcertijoDto ejecutar(String acertijoId) {
        Acertijo acertijo = acertijos.findById(acertijoId)
                .orElseThrow(() -> new IllegalArgumentException("Acertijo no encontrado: " + acertijoId));
        return VisualizarAcertijoDto.from(acertijo, elementos.findByAcertijoId(acertijoId));
    }
}
