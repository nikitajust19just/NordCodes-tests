package api.tests;

import api.config.Config;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class BaseTest {
    protected static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(Config.getWiremockPort()));
    protected SoftAssertions softly;

    private static boolean filtersInitialized = false;

    @BeforeAll
    static void startWireMock() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }

        WireMock.configureFor("localhost", Config.getWiremockPort());

        if (!filtersInitialized) {
            RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
            filtersInitialized = true;
        }
    }

    @BeforeEach
    void setUp() {
        WireMock.reset();
        softly = new SoftAssertions();
    }

    @AfterEach
    void assertAll() {
        softly.assertAll();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }
}