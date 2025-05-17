package com.nipuna.chargepoint.authservice.integration;

import com.nipuna.chargepoint.authservice.dto.AuthorizationRequest;
import com.nipuna.chargepoint.authservice.dto.AuthorizationResponse;
import com.nipuna.chargepoint.authservice.dto.DriverIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"auth-requests", "auth-responses"})
class AuthorizationFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/authorize";
    }

    private AuthorizationRequest createValidRequest() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setStationUuid(UUID.randomUUID().toString());

        DriverIdentifier identifierDto = new DriverIdentifier();
        identifierDto.setId("id12345678901234567890"); // valid ID (length >= 20)
        request.setDriverIdentifier(identifierDto);

        return request;
    }

    @Test
    void shouldReturnValidResponseStatus() {
        AuthorizationRequest request = createValidRequest();
        AuthorizationResponse response = restTemplate.postForObject(getBaseUrl(), request, AuthorizationResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAuthorizationStatus()).isIn("Accepted", "Rejected", "Unknown", "Invalid");
    }

    @Test
    void shouldReturnInvalidForShortDriverId() {
        AuthorizationRequest request = createValidRequest();
        request.getDriverIdentifier().setId("short");

        AuthorizationResponse response = restTemplate.postForObject(getBaseUrl(), request, AuthorizationResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAuthorizationStatus()).isEqualTo("Invalid");
    }

    @Test
    void shouldReturnBadRequestForMissingStationUuid() {
        AuthorizationRequest request = new AuthorizationRequest(); // no stationUuid
        DriverIdentifier identifierDto = new DriverIdentifier();
        identifierDto.setId("id12345678901234567890");
        request.setDriverIdentifier(identifierDto);

        Throwable thrown = catchThrowable(() ->
                restTemplate.postForObject(getBaseUrl(), request, AuthorizationResponse.class));

        assertThat(thrown)
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void shouldReturnBadRequestForMissingDriverId() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setStationUuid(UUID.randomUUID().toString());
        request.setDriverIdentifier(new DriverIdentifier()); // missing ID

        Throwable thrown = catchThrowable(() ->
                restTemplate.postForObject(getBaseUrl(), request, AuthorizationResponse.class));

        assertThat(thrown)
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }
}
