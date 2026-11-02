package co.eci.c15.app.observabilidad;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una linea por peticion (ruta, estado, duracion) con su traceId y contexto, sin tocar los controladores.
 * Corre antes de la seguridad para registrar tambien los rechazados. Responde el traceId en X-Trace-Id.
 */
@Component
public class RegistroPeticionesFilter extends OncePerRequestFilter implements Ordered {

    static final String CABECERA_TRAZA = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(RegistroPeticionesFilter.class);
    private static final Pattern RECURSO = Pattern.compile("^/(?:api/(?:chat/)?|dev/)(salas|partidas)/([A-Za-z0-9-]{1,64})(?:/.*)?$");
    /** Llegan decenas por segundo por jugador: registrarlas una a una solo seria ruido. */
    private static final Pattern FRECUENTES = Pattern.compile("^/api/partidas/[^/]+/(?:reto/clicks|acertijos/[^/]+/clicks)$");
    private static final int MAX_RUTA = 120;

    private final ObjectProvider<Tracer> tracer;

    public RegistroPeticionesFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    public int getOrder() {
        return SecurityProperties.DEFAULT_FILTER_ORDER - 1;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/ws") || uri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        ponerRecursoEnContexto(request.getRequestURI());
        escribirTraza(response);
        try {
            chain.doFilter(request, response);
            registrar(request, response.getStatus(), inicio, null);
        } catch (IOException | ServletException | RuntimeException e) {
            registrar(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, inicio, e);
            throw e;
        } finally {
            MDC.remove(ContextoLog.USER_ID);
            MDC.remove(ContextoLog.SALA_ID);
            MDC.remove(ContextoLog.MATCH_ID);
        }
    }

    private static void ponerRecursoEnContexto(String uri) {
        Matcher recurso = RECURSO.matcher(uri);
        if (recurso.matches()) {
            MDC.put("salas".equals(recurso.group(1)) ? ContextoLog.SALA_ID : ContextoLog.MATCH_ID, recurso.group(2));
        }
    }

    private void escribirTraza(HttpServletResponse response) {
        Tracer actual = tracer.getIfAvailable();
        Span span = actual != null ? actual.currentSpan() : null;
        if (span != null) response.setHeader(CABECERA_TRAZA, span.context().traceId());
    }

    private static void registrar(HttpServletRequest request, int estado, long inicio, Throwable error) {
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        String metodo = request.getMethod();
        String ruta = rutaDe(request);
        Level nivel = nivelDe(estado, request.getRequestURI());
        log.atLevel(nivel)
                .addKeyValue("evento", "http.peticion")
                .addKeyValue("http.metodo", metodo)
                .addKeyValue("http.ruta", ruta)
                .addKeyValue("http.estado", estado)
                .addKeyValue("duracion.ms", ms)
                .setCause(error)
                .log("{} {} -> {} en {} ms", metodo, ruta, estado, ms);
    }

    private static Level nivelDe(int estado, String uri) {
        if (estado >= 500) return Level.ERROR;
        if (estado == 401 || estado == 403) return Level.WARN;
        return FRECUENTES.matcher(uri).matches() ? Level.DEBUG : Level.INFO;
    }

    /** La plantilla (/api/salas/{salaId}) si la hay; si no, la URI limpia, que viene del cliente. */
    private static String rutaDe(HttpServletRequest request) {
        Object plantilla = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (plantilla != null) return plantilla.toString();
        String limpia = request.getRequestURI().replaceAll("[^A-Za-z0-9/._%-]", "_");
        return limpia.length() > MAX_RUTA ? limpia.substring(0, MAX_RUTA) : limpia;
    }
}
