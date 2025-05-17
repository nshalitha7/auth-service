package com.nipuna.chargepoint.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.DriverIdentifier;
import com.nipuna.chargepoint.authservice.kafka.AuthorizationProducer;
import com.nipuna.chargepoint.authservice.service.ResponseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizationProducer producer;

    @MockBean
    private ResponseManager responseManager;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthorizationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AuthorizationRequest();
        validRequest.setStationUuid(UUID.randomUUID().toString());
        DriverIdentifier driver = new DriverIdentifier();
        driver.setId("id12345678901234567890");
        validRequest.setDriverIdentifier(driver);

        // Reset mocks to avoid test bleed-through
        Mockito.reset(producer, responseManager);
    }

    @Test
    void shouldReturnInvalidForShortLength() throws Exception {
        validRequest.getDriverIdentifier().setId("short");

        mockMvc.perform(post("/api/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationStatus").value("Invalid"));

        verifyNoInteractions(producer);
    }

    @Test
    void shouldTimeoutWhenNoKafkaResponse() throws Exception {
        doAnswer(invocation -> {
            CompletableFuture<String> futureArg = invocation.getArgument(1);
            futureArg.completeExceptionally(new RuntimeException("timeout"));
            return null;
        }).when(responseManager).register(anyString(), any());

        mockMvc.perform(post("/api/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldReturnAcceptedStatus() throws Exception {
        doAnswer(invocation -> {
            CompletableFuture<String> futureArg = invocation.getArgument(1);
            futureArg.complete("Accepted");
            return null;
        }).when(responseManager).register(anyString(), any());

        mockMvc.perform(post("/api/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationStatus").value("Accepted"));
    }

    @Test
    void shouldReturnRejectedStatus() throws Exception {
        doAnswer(invocation -> {
            CompletableFuture<String> futureArg = invocation.getArgument(1);
            futureArg.complete("Rejected");
            return null;
        }).when(responseManager).register(anyString(), any());

        mockMvc.perform(post("/api/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationStatus").value("Rejected"));
    }

    @Test
    void shouldReturnUnknownStatus() throws Exception {
        doAnswer(invocation -> {
            CompletableFuture<String> futureArg = invocation.getArgument(1);
            futureArg.complete("Unknown");
            return null;
        }).when(responseManager).register(anyString(), any());

        mockMvc.perform(post("/api/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationStatus").value("Unknown"));
    }
}
