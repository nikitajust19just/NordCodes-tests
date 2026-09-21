package api.tests;

import api.generators.RandomData;
import api.models.Action;
import api.models.AppResponse;
import api.steps.ApiSteps;
import api.steps.WireMockSteps;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Авторизация и Жизненный цикл токенов")
@Feature("Известные дефекты системы")
@DisplayName("Набор тестов на известные баги бэкенда")
public class KnownBugsTests extends BaseTest {

    @Test
    @Issue("BUG-501")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-501: Падение бэкенда в 500 при сетевом сбое внешнего сервиса авторизации /auth")
    void userCannotLoginIfExternalServiceFails_KnownBug() {
        Allure.step("Симуляция падения внешнего сервиса авторизации (код 401) и проверка реакции приложения", () -> {
            String token = RandomData.getToken();
            WireMockSteps.stubErrorResponse("/auth", HttpStatus.SC_UNAUTHORIZED, "Invalid token status");

            WireMock.resetAllRequests();

            Response response = ApiSteps.sendRequest(token, Action.LOGIN);

            softly.assertThat(response.getStatusCode())
                    .as("КРИТИЧЕСКИЙ БАГ: Бэкенд возвращает 500 ошибку вместо корректной обработки")
                    .isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");
            softly.assertThat(body.getMessage())
                    .as("Сообщение об ошибке в теле ответа должно быть заполнено")
                    .isNotNull();

            WireMockSteps.verifyPostRequestSent("/auth", token);
        });
    }

    @Test
    @Issue("BUG-502")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-502: Падение валидации при использовании разрешенных по ТЗ символов (G-Z) в токене")
    void tokenWithNonHexAlphabetChars_KnownBug() {
        Allure.step("Отправка LOGIN с токеном, содержащим разрешенные по ТЗ буквы вне HEX-диапазона (например, Z)", () -> {
            String tzToken = RandomData.getTzToken(32);
            WireMock.resetAllRequests();

            Response response = ApiSteps.sendRequest(tzToken, Action.LOGIN);

            softly.assertThat(response.getStatusCode())
                    .as("КРИТИЧЕСКИЙ БАГ ВАЛИДАЦИИ: Бэкенд отклоняет буквы G-Z, хотя они разрешены в ТЗ")
                    .isEqualTo(HttpStatus.SC_BAD_REQUEST);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");
            softly.assertThat(body.getMessage()).contains("token: должно соответствовать");

            WireMockSteps.verifyEndpointNeverCalled("/auth");
        });
    }

    @Test
    @Issue("BUG-503")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-503: Падение бэкенда в 500 при сетевом сбое внешнего сервиса /doAction")
    void externalServiceFailureDuringAction_KnownBug() {
        Allure.step("Симуляция падения внешнего сервиса бизнес-логики (код 500) и проверка реакции приложения", () -> {
            String token = RandomData.getToken();
            WireMockSteps.stubSuccessResponse("/auth");
            WireMockSteps.stubErrorResponse("/doAction", 500, "Downstream crash");

            ApiSteps.sendRequest(token, Action.LOGIN);
            WireMock.resetAllRequests();

            Response response = ApiSteps.sendRequest(token, Action.ACTION);

            softly.assertThat(response.getStatusCode())
                    .as("КРИТИЧЕСКИЙ БАГ: Бэкенд возвращает 500 при падении сервиса /doAction")
                    .isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");

            WireMockSteps.verifyPostRequestSent("/doAction", token);
        });
    }
}