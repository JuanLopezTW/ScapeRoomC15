package co.eci.c15.salas.domain;

import java.util.Objects;
import java.util.UUID;

public final class Sala {

    public enum Estado { DISPONIBLE, EN_PARTIDA }

    private final String id;
    private final String nombre;
    private final String anfitrionId;
    private Estado estado;

    private Sala(String id, String nombre, String anfitrionId) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = Estado.DISPONIBLE;
    }

    public static Sala crear(String nombre, String anfitrionId) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        Objects.requireNonNull(anfitrionId, "anfitrionId");
        return new Sala(UUID.randomUUID().toString(), nombre.trim(), anfitrionId);
    }

    public void iniciarPartida() { this.estado = Estado.EN_PARTIDA; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getAnfitrionId() { return anfitrionId; }
    public Estado getEstado() { return estado; }
    public boolean isDisponible() { return estado == Estado.DISPONIBLE; }
}
