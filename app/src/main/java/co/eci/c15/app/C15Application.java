package co.eci.c15.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada unico del monolito modular EscapeRoomC15.
 * Al escanear co.eci.c15, Spring levanta los @Configuration/@Component
 * de TODOS los modulos (auth, salas, gameplay, chat, estadisticas).
 *
 * Cuando se migre a microservicios, cada modulo tendra su propia
 * clase equivalente a esta, en su propio proyecto.
 */
@SpringBootApplication(scanBasePackages = "co.eci.c15")
public class C15Application {
    public static void main(String[] args) {
        SpringApplication.run(C15Application.class, args);
    }
}
