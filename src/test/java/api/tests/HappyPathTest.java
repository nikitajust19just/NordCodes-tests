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
@Feature("Успешные (Happy Path) сценарии взаимодействия")
@DisplayName("Набор позитивных интеграционных тестов")
public class HappyPathTest extends BaseTest {

    @Test
    @Story("Авторизация и выполнение основного действия")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Успешный сквозной проход: LOGIN и последующий ACTION")
    void userCanPerformFullHappyPathScenario() {
        String token = RandomData.getToken();

        WireMockSteps.stubSuccessResponse("/auth");
        WireMockSteps.stubSuccessResponse("/doAction");

        Allure.step("Шаг 1: Инициация LOGIN пользователем", () -> {
            Response response = ApiSteps.sendRequest(token, Action.LOGIN);
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("OK");
            softly.assertThat(body.getMessage()).as("Успешный ответ не должен содержать ошибок").isNull();

            WireMockSteps.verifyPostRequestSent("/auth", token);
        });

        Allure.step("Шаг 2: Выполнение бизнес-действия (ACTION) авторизованным токеном", () -> {
            Response response = ApiSteps.sendRequest(token, Action.ACTION);
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("OK");
            softly.assertThat(body.getMessage()).as("Успешный ответ не должен содержать ошибок").isNull();

            WireMockSteps.verifyPostRequestSent("/doAction", token);
        });

    }

    @Test
    @Story("Деавторизация токена (LOGOUT)")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Успешный отзыв токена (LOGOUT) и фиксация изоляции контракта")
    void userCanLogoutSuccessfully() {
        String token = RandomData.getToken();
        WireMockSteps.stubSuccessResponse("/auth");

        Allure.step("Предусловие: Выполнение успешного входа (LOGIN)", () -> {
            ApiSteps.sendRequest(token, Action.LOGIN);
        });
        WireMock.resetAllRequests();

        Allure.step("Шаг 1: Отправка запроса на выход (LOGOUT)", () -> {
            Response response = ApiSteps.sendRequest(token, Action.LOGOUT);
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("OK");
            softly.assertThat(body.getMessage()).as("Успешный ответ не должен содержать ошибок").isNull();

            WireMockSteps.verifyEndpointNeverCalled("/auth");
            WireMockSteps.verifyEndpointNeverCalled("/doAction");
        });
    }
}