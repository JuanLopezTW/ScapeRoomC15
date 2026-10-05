package co.eci.c15.salas.infrastructure.persistence;

import co.eci.c15.salas.domain.Sala;
import jakarta.persistence.*;

@Entity
@Table(name = "salas")
class SalaJpa {

    @Id
    private String id;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String anfitrionId;
    @Enumerated(EnumType.STRING)
    private Sala.Estado estado;

    protected SalaJpa() {}

    SalaJpa(String id, String nombre, String anfitrionId, Sala.Estado estado) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = estado;
    }

    String getId() { return id; }
    String getNombre() { return nombre; }
    String getAnfitrionId() { return anfitrionId; }
    Sala.Estado getEstado() { return estado; }
}
