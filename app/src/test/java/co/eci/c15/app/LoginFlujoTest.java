package co.eci.c15.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Login contra la app ensamblada (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class LoginFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginExitosoYUsernameRepetido() throws Exception {
        String body = "{\"username\":\"jugador_flujo\"}";

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jugador_flujo"))
                .andExpect(jsonPath("$.userId").isNotEmpty());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void usernameInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"ab\"}"))
                .andExpect(status().isBadRequest());
    }
}
