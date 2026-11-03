package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.application.EquiposDePartida;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Los equipos de cada partida en curso, para que cualquier instancia sepa quien puede hablar en que canal. */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class EquiposDePartidaRedis implements EquiposDePartida {

    static final String PREFIJO = "c15:chat:equipos:";
    private static final TypeReference<Map<String, List<String>>> TIPO = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final CaducidadChat caducidad;

    public EquiposDePartidaRedis(StringRedisTemplate redis, ObjectMapper json, CaducidadChat caducidad) {
        this.redis = redis;
        this.json = json;
        this.caducidad = caducidad;
    }

    @Override
    public void guardar(String matchId, Map<String, List<String>> equipos) {
        try {
            redis.opsForValue().set(PREFIJO + matchId, json.writeValueAsString(equipos), caducidad.partida());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron guardar los equipos de " + matchId, e);
        }
    }

    @Override
    public Optional<Map<String, List<String>>> de(String matchId) {
        String guardado = redis.opsForValue().get(PREFIJO + matchId);
        if (guardado == null) return Optional.empty();
        try {
            return Optional.of(json.readValue(guardado, TIPO));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Equipos corruptos en Redis para " + matchId, e);
        }
    }

    @Override
    public void borrar(String matchId) {
        redis.delete(PREFIJO + matchId);
    }
}
