package mega.privacy.android.app.utils;

public class TextUtil {

    public static boolean isTextEmpty(String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }
}
