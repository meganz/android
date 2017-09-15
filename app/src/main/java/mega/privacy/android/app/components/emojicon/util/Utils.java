package mega.privacy.android.app.components.emojicon.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicInteger;

public final class Utils {
    private static int sKeyboardHeight;

    private Utils() {
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static int getSoftKeyboardHeight(Context context) {
        return 200;
    }

    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        return height;
    }

    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        return width;
    }

    public static boolean isKeyboardShowing(Context context) {
        return context.getResources().getConfiguration().keyboardHidden == Configuration.KEYBOARDHIDDEN_NO;
    }

    public static void setKeyboardHeight(int keyboardHeight) {
        Utils.sKeyboardHeight = keyboardHeight;
    }

    public static int getKeyboardHeight() {
        return sKeyboardHeight;
    }
}
