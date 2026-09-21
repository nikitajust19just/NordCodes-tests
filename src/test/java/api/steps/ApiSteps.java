package api.steps;

import api.models.Action;
import api.specs.RequestSpecs;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public final class ApiSteps {

    private ApiSteps() {
    }

    @Step("[API] Отправка запроса {action} с токеном {token}")
    public static Response sendRequest(String token, Action action) {
        return given()
                .spec(RequestSpecs.appSpec(action))
                .formParam("token", token)
                .formParam("action", action.name())
                .when()
                .post("/endpoint");
    }

    @Step("[API] Отправка запроса {action} с кастомным API-ключом")
    public static Response sendRequestWithCustomKey(String token, Action action, String apiKey) {
        return given()
                .spec(RequestSpecs.appSpecWithCustomKey(action, apiKey))
                .formParam("token", token)
                .formParam("action", action.name())
                .when()
                .post("/endpoint");
    }
}
