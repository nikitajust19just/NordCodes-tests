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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Epic("Авторизация и Жизненный цикл токенов")
@Feature("Валидация входящих параметров и периметра")
@DisplayName("Тесты на валидацию контрактов API")
public class ValidationTests extends BaseTest {

    @DisplayName("Валидация граничных значений длины токена")
    @ParameterizedTest(name = "Валидация формата токена: {2} (длина: {1})")
    @Story("Валидация входящих параметров")
    @CsvSource({
            "SHORT_TOKEN, 31, Длина токена на 1 символ меньше допустимой",
            "LONG_TOKEN, 33, Длина токена на 1 символ больше допустимой",
            "EMPTY_TOKEN, 0, Передан абсолютно пустой токен"
    })
    void invalidTokenLengthShouldReturnBadRequest(String type, int length, String description) {
        Allure.step("Проверка сценария: " + description, () -> {
            String invalidToken = RandomData.getToken(length);

            Response response = ApiSteps.sendRequest(invalidToken, Action.LOGIN);
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

            AppResponse body = response.as(AppResponse.class);
            softly.assertThat(body.getResult()).isEqualTo("ERROR");
            softly.assertThat(body.getMessage()).contains("token: должно соответствовать");

            WireMockSteps.verifyEndpointNeverCalled("/auth");
        });
    }

    @Test
    @Story("Валидация входящих параметров")
    @DisplayName("Отправка запроса с неизвестным строковым action")
    void requestWithUnknownActionShouldReturnBadRequest() {
        Allure.step("Отправка запроса со случайным некорректным значением action", () -> {
            String token = RandomData.getToken();
            String invalidAction = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);
            WireMock.resetAllRequests();

            Response response = io.restassured.RestAssured.given()
                    .spec(api.specs.RequestSpecs.appSpec(Action.LOGIN))
                    .formParam("token", token)
                    .formParam("action", invalidAction)
                    .post("/endpoint");

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            WireMockSteps.verifyEndpointNeverCalled("/auth");
            WireMockSteps.verifyEndpointNeverCalled("/doAction");
        });
    }

    @Test
    @Story("Безопасность сетевого периметра API")
    @DisplayName("Отправка запроса LOGIN с неверным секретным ключом X-Api-Key")
    void requestWithInvalidApiKeyShouldBeRejected() {
        Allure.step("Попытка авторизации скомпрометированным или ошибочным API-ключом", () -> {
            String token = RandomData.getToken();

            Response response = ApiSteps.sendRequestWithCustomKey(token, Action.LOGIN, "INVALID_SECRET_ABC_123");
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);

            WireMockSteps.verifyEndpointNeverCalled("/auth");
        });
    }

    @Test
    @Story("Безопасность сетевого периметра API")
    @DisplayName("Отправка запроса полностью без заголовка X-Api-Key")
    void requestWithoutXApiKeyShouldBeRejected() {
        Allure.step("Попытка отправить запрос, когда заголовок X-Api-Key полностью отсутствует", () -> {
            String token = RandomData.getToken();
            WireMock.resetAllRequests();

            Response response = ApiSteps.sendRequestWithCustomKey(token, Action.LOGIN, null);

            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
            WireMockSteps.verifyEndpointNeverCalled("/auth");
        });
    }
}