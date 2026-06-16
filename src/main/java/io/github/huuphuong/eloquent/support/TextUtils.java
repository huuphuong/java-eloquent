package io.github.huuphuong.eloquent.support;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

