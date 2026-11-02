package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.CanalChat;
import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import co.eci.c15.chat.domain.MensajeChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los cuatro chats del juego, todos efimeros:
 * <ul>
 *   <li>global (menu): cualquier usuario; los mensajes caducan a la hora.</li>
 *   <li>sala: los jugadores de la sala, mientras no inicie la partida.</li>
 *   <li>partida: todos los jugadores de la partida.</li>
 *   <li>equipo: solo los de su equipo, que lo decide el back a partir del userId.</li>
 * </ul>
 */
@Service
public class ChatService {

    static final Duration CADUCIDAD_GLOBAL = Duration.ofHours(1);

    private final ChatRepository mensajes;
    private final SalaEsperaConsulta salas;
    private final UsuarioConsulta usuarios;
    private final ChatNotifier notifier;
    private final Clock clock;
    private final LimiteMensajes limite = new LimiteMensajes();
    /** matchId -> equipoId -> jugadores, tal como llegan en PartidaIniciadaEvent. */
    private final Map<String, Map<String, List<String>>> partidas = new ConcurrentHashMap<>();

    @Autowired
    public ChatService(ChatRepository mensajes, SalaEsperaConsulta salas, UsuarioConsulta usuarios, ChatNotifier notifier) {
        this(mensajes, salas, usuarios, notifier, Clock.systemUTC());
    }

    ChatService(ChatRepository mensajes, SalaEsperaConsulta salas, UsuarioConsulta usuarios, ChatNotifier notifier, Clock clock) {
        this.mensajes = mensajes;
        this.salas = salas;
        this.usuarios = usuarios;
        this.notifier = notifier;
        this.clock = clock;
    }

    public MensajeChatDto enviarGlobal(String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.GLOBAL.clave(), userId, texto, clock.instant());
        if (!usuarios.existe(userId)) throw new IllegalArgumentException("El usuario no existe");
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialGlobal() {
        Instant limiteCaducidad = clock.instant().minus(CADUCIDAD_GLOBAL);
        return mensajes.historial(CanalChat.GLOBAL.clave()).stream()
                .filter(m -> m.enviadoEn().isAfter(limiteCaducidad))
                .map(MensajeChatDto::from)
                .toList();
    }

    public MensajeChatDto enviarSala(String salaId, String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.sala(salaId).clave(), userId, texto, clock.instant());
        validarSala(salaId, userId);
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialSala(String salaId, String userId) {
        exigirUserId(userId);
        validarSala(salaId, userId);
        return historial(CanalChat.sala(salaId));
    }

    public MensajeChatDto enviarPartida(String matchId, String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.partida(matchId).clave(), userId, texto, clock.instant());
        equipoDe(matchId, userId);
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialPartida(String matchId, String userId) {
        exigirUserId(userId);
        equipoDe(matchId, userId);
        return historial(CanalChat.partida(matchId));
    }

    public MensajeChatDto enviarEquipo(String matchId, String userId, String texto) {
        exigirUserId(userId);
        CanalChat canal = CanalChat.equipo(matchId, equipoDe(matchId, userId));
        return publicar(MensajeChat.crear(canal.clave(), userId, texto, clock.instant()));
    }

    public List<MensajeChatDto> historialEquipo(String matchId, String userId) {
        exigirUserId(userId);
        return historial(CanalChat.equipo(matchId, equipoDe(matchId, userId)));
    }

    /** La sala salio de la espera: su chat se cierra y se olvida. */
    public void cerrarSala(String salaId) {
        mensajes.borrar(CanalChat.sala(salaId).clave());
    }

    public void partidaIniciada(String matchId, Map<String, List<String>> equipos) {
        cerrarSala(matchId);
        partidas.put(matchId, equipos);
    }

    public void partidaTerminada(String matchId) {
        Map<String, List<String>> equipos = partidas.remove(matchId);
        mensajes.borrar(CanalChat.partida(matchId).clave());
        if (equipos != null) {
            equipos.keySet().forEach(equipoId -> mensajes.borrar(CanalChat.equipo(matchId, equipoId).clave()));
        }
    }

    private MensajeChatDto publicar(MensajeChat mensaje) {
        limite.registrar(mensaje.userId(), mensaje.enviadoEn());
        mensajes.guardar(mensaje);
        MensajeChatDto dto = MensajeChatDto.from(mensaje);
        notifier.mensajeNuevo(dto);
        return dto;
    }

    private List<MensajeChatDto> historial(CanalChat canal) {
        return mensajes.historial(canal.clave()).stream().map(MensajeChatDto::from).toList();
    }

    private void validarSala(String salaId, String userId) {
        if (!salas.enEspera(salaId)) {
            cerrarSala(salaId);
            throw new ChatNoDisponibleException(salaId);
        }
        if (!salas.contieneJugador(salaId, userId)) throw new JugadorFueraDeSalaException(salaId, userId);
    }

    private String equipoDe(String matchId, String userId) {
        Map<String, List<String>> equipos = partidas.get(matchId);
        if (equipos == null) throw new ChatNoDisponibleException(matchId);
        return equipos.entrySet().stream()
                .filter(e -> e.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new JugadorFueraDeSalaException(matchId, userId));
    }

    private static void exigirUserId(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
    }
}
