package co.eci.c15.salas.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Sala {

    public enum Estado { DISPONIBLE, EN_PARTIDA }

    public static final int MIN_EQUIPOS = 2;
    public static final int MIN_JUGADORES_POR_EQUIPO = 2;

    private final String id;
    private final String nombre;
    private final String anfitrionId;
    private Estado estado;
    private int numEquipos;
    private int jugadoresPorEquipo;
    private final Set<String> jugadores = new HashSet<>();

    private Sala(String id, String nombre, String anfitrionId, Estado estado,
                 int numEquipos, int jugadoresPorEquipo) {
        this.id = id;
        this.nombre = nombre;
        this.anfitrionId = anfitrionId;
        this.estado = estado;
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
    }

    public static Sala crear(String nombre, String anfitrionId) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la sala no puede estar vacío");
        if (anfitrionId == null || anfitrionId.isBlank()) throw new IllegalArgumentException("El anfitrionId es obligatorio");
        return new Sala(UUID.randomUUID().toString(), nombre.trim(), anfitrionId, Estado.DISPONIBLE, 2, 4);
    }

    /** Reconstruye una sala ya existente (desde persistencia) conservando su id y su estado. */
    public static Sala reconstituir(String id, String nombre, String anfitrionId, Estado estado,
                                    int numEquipos, int jugadoresPorEquipo, Collection<String> jugadores) {
        Sala sala = new Sala(Objects.requireNonNull(id), nombre, anfitrionId, estado, numEquipos, jugadoresPorEquipo);
        sala.jugadores.addAll(jugadores);
        return sala;
    }

    public void configurar(String solicitanteId, int numEquipos, int jugadoresPorEquipo) {
        if (!anfitrionId.equals(solicitanteId)) throw new NoEsAnfitrionException(solicitanteId);
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        if (numEquipos < MIN_EQUIPOS) throw new ConfiguracionInvalidaException("El número de equipos debe ser al menos " + MIN_EQUIPOS);
        if (jugadoresPorEquipo < MIN_JUGADORES_POR_EQUIPO) throw new ConfiguracionInvalidaException("Los jugadores por equipo deben ser al menos " + MIN_JUGADORES_POR_EQUIPO);
        if (numEquipos * jugadoresPorEquipo < jugadores.size()) {
            throw new ConfiguracionInvalidaException("El nuevo cupo (" + numEquipos * jugadoresPorEquipo
                    + ") es menor que los jugadores que ya están en la sala (" + jugadores.size() + ")");
        }
        this.numEquipos = numEquipos;
        this.jugadoresPorEquipo = jugadoresPorEquipo;
    }

    public void unirJugador(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
        if (estado == Estado.EN_PARTIDA) throw new PartidaYaIniciadaException(id);
        if (jugadores.contains(userId)) return;
        if (jugadores.size() >= numEquipos * jugadoresPorEquipo) throw new SalaLlenaException(id);
        jugadores.add(userId);
    }

    public boolean contieneJugador(String userId) { return jugadores.contains(userId); }

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