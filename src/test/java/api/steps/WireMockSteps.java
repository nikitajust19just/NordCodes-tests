package api.steps;

import io.qameta.allure.Step;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public final class WireMockSteps {

    private WireMockSteps() {
    }

    @Step("[WireMock] Настройка успешного ответа (200 OK) для эндпоинта {endpoint}")
    public static void stubSuccessResponse(String endpoint) {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\": \"success\"}")));
    }

    @Step("[WireMock] Настройка ответа с ошибкой {statusCode} для эндпоинта {endpoint}")
    public static void stubErrorResponse(String endpoint, int statusCode, String reason) {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\": \"ERROR\", \"message\": \"" + reason + "\"}")));
    }

    @Step("[WireMock] Верификация: Ровно 1 POST-запрос ушел на {endpoint} с токеном в формате form-urlencoded")
    public static void verifyPostRequestSent(String endpoint, String token) {
        verify(1, postRequestedFor(urlEqualTo(endpoint))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("token=" + token)));
    }

    @Step("[WireMock] Верификация: Эндпоинт {endpoint} гарантированно НЕ вызывался")
    public static void verifyEndpointNeverCalled(String endpoint) {
        verify(0, postRequestedFor(urlEqualTo(endpoint)));
    }
}