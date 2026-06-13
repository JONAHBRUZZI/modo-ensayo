package com.modoensayo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerAndLogin_shouldReturnToken() throws Exception {
        String email = "test_" + System.currentTimeMillis() + "@gmail.com";
        String registerBody = """
            {
                "fullName": "Test User",
                "email": "%s",
                "password": "Test1234!",
                "phone": "+56912345678"
            }
            """.formatted(email);

        // Registro
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        // Login
        String loginBody = """
            {"email": "%s", "password": "Test1234!"}
            """.formatted(email);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerWithInvalidPassword_shouldReturn400() throws Exception {
        String body = """
            {
                "fullName": "Test User",
                "email": "bad@test.com",
                "password": "short",
                "phone": "+56912345678"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
