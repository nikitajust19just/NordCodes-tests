package api.generators;

import org.apache.commons.lang3.RandomStringUtils;

public final class RandomData {
    private RandomData() {}

    public static String getToken() {
        return getToken(32);
    }

    public static String getToken(int length) {
        return RandomStringUtils.random(length, "0123456789ABCDEF");
    }

    public static String getTzToken(int length) {
        return RandomStringUtils.random(length, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }
}
