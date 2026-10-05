package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.Acertijo;
import jakarta.persistence.*;

@Entity
@Table(name = "acertijos")
class AcertijoJpa {

    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String componenteMapaId;
    @Column(nullable = false, length = 2000)
    private String enunciado;
    @Enumerated(EnumType.STRING)
    private Acertijo.Estado estado;

    protected AcertijoJpa() {}

    AcertijoJpa(String id, String componenteMapaId, String enunciado, Acertijo.Estado estado) {
        this.id = id;
        this.componenteMapaId = componenteMapaId;
        this.enunciado = enunciado;
        this.estado = estado;
    }

    String getId() { return id; }
    String getComponenteMapaId() { return componenteMapaId; }
    String getEnunciado() { return enunciado; }
    Acertijo.Estado getEstado() { return estado; }
}
