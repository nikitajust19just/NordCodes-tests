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
@Feature("Устойчивость автомата состояний сессий")
@DisplayName("Тесты переходов конечного автомата состояний")
public class StateMachineTests extends BaseTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос действия (ACTION) без предварительного выполнения LOGIN")
    void userCannotPerformActionWithoutLogin() {
        Allure.step("Попытка вызвать ACTION для токена, который не создавал сессию", () -> {
            String token = RandomData.getToken();
            WireMockSteps.stubSuccessResponse("/doAction");

            Response response = ApiSteps.sendRequest(token, Action.ACTION);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");
            softly.assertThat(body.getMessage()).contains("Token").contains("not found");

            WireMockSteps.verifyEndpointNeverCalled("/doAction");
        });
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторный запрос действия (ACTION) после успешного выполнения LOGOUT")
    void userCannotPerformActionAfterLogout() {
        String token = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");
        WireMockSteps.stubSuccessResponse("/doAction");

        Allure.step("Предусловие: Последовательный LOGIN и успешный LOGOUT для токена", () -> {
            ApiSteps.sendRequest(token, Action.LOGIN);
            ApiSteps.sendRequest(token, Action.LOGOUT);
        });

        WireMock.resetAllRequests();

        Allure.step("Попытка вызвать ACTION после того, как сессия была закрыта через LOGOUT", () -> {
            Response response = ApiSteps.sendRequest(token, Action.ACTION);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");

            WireMockSteps.verifyEndpointNeverCalled("/doAction");
        });
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный LOGIN для уже залогиненного токена приводит к конфликту сессий (409)")
    void duplicateLoginShouldBeRejectedWithConflict() {
        String token = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");

        Allure.step("Предусловие: Первичный успешный LOGIN токена в систему", () -> {
            ApiSteps.sendRequest(token, Action.LOGIN);
        });

        Allure.step("Повторный вызов LOGIN для этого же токена без предварительного выхода", () -> {
            Response response = ApiSteps.sendRequest(token, Action.LOGIN);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CONFLICT);
            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");
            softly.assertThat(body.getMessage()).contains("Token").contains("already exists");
        });
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный LOGOUT после того, как сессия уже была успешно закрыта")
    void duplicateLogoutShouldReturnForbidden() {
        String token = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");

        Allure.step("Предусловие: Успешный вход и последующее закрытие сессии (LOGOUT)", () -> {
            ApiSteps.sendRequest(token, Action.LOGIN);
            ApiSteps.sendRequest(token, Action.LOGOUT);
        });

        WireMock.resetAllRequests();

        Allure.step("Попытка отправить повторный запрос LOGOUT для уже неактивного токена", () -> {
            Response response = ApiSteps.sendRequest(token, Action.LOGOUT);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            WireMockSteps.verifyEndpointNeverCalled("/auth");
            WireMockSteps.verifyEndpointNeverCalled("/doAction");
        });
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторный LOGIN после успешного LOGOUT успешно пересоздает сессию")
    void loginAgainAfterLogoutShouldBeSuccess() {
        String token = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");

        Allure.step("Предусловие: Полный цикл входа и выхода из системы для токена", () -> {
            ApiSteps.sendRequest(token, Action.LOGIN);
            ApiSteps.sendRequest(token, Action.LOGOUT);
        });

        WireMock.resetAllRequests();

        Allure.step("Попытка заново выполнить LOGIN для этого же токена", () -> {
            Response response = ApiSteps.sendRequest(token, Action.LOGIN);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            WireMockSteps.verifyPostRequestSent("/auth", token);
        });
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Проверка изоляции сессий: логаут Токена А не сбрасывает сессию Токена Б")
    void twoTokensShouldBeIsolated() {
        String tokenA = RandomData.getToken();
        String tokenB = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");
        WireMockSteps.stubSuccessResponse("/doAction");

        Allure.step("Предусловие: Создание активных сессий (LOGIN) для двух независимых токенов", () -> {
            ApiSteps.sendRequest(tokenA, Action.LOGIN);
            ApiSteps.sendRequest(tokenB, Action.LOGIN);
        });

        Allure.step("Действие: Выполнение выхода из системы (LOGOUT) только для Токена А", () -> {
            ApiSteps.sendRequest(tokenA, Action.LOGOUT);
        });

        WireMock.resetAllRequests();

        Allure.step("Проверка: Токен Б сохранил сессию и беспрепятственно выполняет ACTION", () -> {
            Response responseB = ApiSteps.sendRequest(tokenB, Action.ACTION);

            softly.assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            AppResponse bodyB = responseB.as(AppResponse.class);
            softly.assertThat(bodyB.getResult()).isEqualTo("OK");

            WireMockSteps.verifyPostRequestSent("/doAction", tokenB);
            WireMockSteps.verifyEndpointNeverCalled("/auth");
        });
    }
}