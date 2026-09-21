package api.specs;

import api.config.Config;
import api.models.Action;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class RequestSpecs {
    private RequestSpecs() {
    }

    private static AllureRestAssured createAllureFilter(String actionName) {
        return new AllureRestAssured()
                .setRequestAttachmentName("Запрос: " + actionName)
                .setResponseAttachmentName("Ответ: " + actionName);
    }

    public static RequestSpecification appSpec(Action action) {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .addHeader("X-Api-Key", Config.getApiKey())
                .addFilter(createAllureFilter(action.name()))
                .build();
    }

    public static RequestSpecification appSpecWithCustomKey(Action action, String customKey) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .addFilter(createAllureFilter(action.name() + " (Кастомный API Key)"));

        if (customKey != null) {
            builder.addHeader("X-Api-Key", customKey);
        }
        return builder.build();
    }
}
