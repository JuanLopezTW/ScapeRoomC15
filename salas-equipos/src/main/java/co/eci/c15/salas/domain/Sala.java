package co.eci.c15.salas.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Sala {

    public enum Estado { DISPONIBLE, EN_PARTIDA }

    private final String id;
    private final String nombre;
    private final String anfitrionId;
    private Estado estado;
    private int numEquipos;
    private int jugadoresPorEquipo;
    private final Set<String> jugadores = new HashSet<>();

    private Sala(String id, String nombre, String anfitrionId) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = Estado.DISPONIBLE;
        this.numEquipos = 2;
        this.jugadoresPorEquipo = 4;
    }

    public static Sala crear(String nombre, String anfitrionId) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        Objects.requireNonNull(anfitrionId, "anfitrionId");
        return new Sala(UUID.randomUUID().toString(), nombre.trim(), anfitrionId);
    }

    public void configurar(String solicitanteId, int numEquipos, int jugadoresPorEquipo) {
        if (!anfitrionId.equals(solicitanteId)) throw new NoEsAnfitrionException(solicitanteId);
        if (numEquipos < 1) throw new ConfiguracionInvalidaException("El número de equipos debe ser al menos 1");
        if (jugadoresPorEquipo < 1) throw new ConfiguracionInvalidaException("Los jugadores por equipo deben ser al menos 1");
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
    }

    public void unirJugador(String userId) {
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        if (jugadores.size() >= numEquipos * jugadoresPorEquipo) throw new SalaLlenaException(id);
        jugadores.add(userId);
    }

    public void iniciarPartida() { this.estado = Estado.EN_PARTIDA; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getAnfitrionId() { return anfitrionId; }
    public Estado getEstado() { return estado; }
    public boolean isDisponible() { return estado == Estado.DISPONIBLE; }
    public int getNumEquipos() { return numEquipos; }
    public int getJugadoresPorEquipo() { return jugadoresPorEquipo; }
    public Set<String> getJugadores() { return Collections.unmodifiableSet(jugadores); }
    public int getCupoTotal() { return numEquipos * jugadoresPorEquipo; }
}
