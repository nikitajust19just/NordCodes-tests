package api.config;

public final class Config {
    private Config() {
    }

    public static String getBaseUrl() {
        String envUrl = System.getenv("BASE_URL");
        return System.getProperty("baseUrl", envUrl != null ? envUrl : "http://localhost:8080/");
    }

    public static String getApiKey() {
        String envKey = System.getenv("API_KEY");
        return System.getProperty("apiKey", envKey != null ? envKey : "qazWSXedc");
    }

    public static int getWiremockPort() {
        String envPort = System.getenv("WIREMOCK_PORT");
        String port = System.getProperty("wiremockPort", envPort != null ? envPort : "8888");
        return Integer.parseInt(port);
    }
}