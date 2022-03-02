package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.graphics.Point;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.PopupWindow;

final class Utils {
  static final int DONT_UPDATE_FLAG = -1;

  @NonNull static <T> T checkNotNull(@Nullable final T reference, final String message) {
    if (reference == null) {
      throw new IllegalArgumentException(message);
    }

    return reference;
  }

  static int dpToPx(@NonNull final Context context, final float dp) {
    return (int) (dp * context.getResources().getDisplayMetrics().density);
  }

  @NonNull static Point locationOnScreen(@NonNull final View view) {
    final int[] location = new int[2];
    view.getLocationOnScreen(location);
    return new Point(location[0], location[1]);
  }

  static void fixPopupLocation(@NonNull final PopupWindow popupWindow, @NonNull final Point desiredLocation) {
    popupWindow.getContentView().post(new Runnable() {
      @Override public void run() {
        final Point actualLocation = locationOnScreen(popupWindow.getContentView());

        if (!(actualLocation.x == desiredLocation.x && actualLocation.y == desiredLocation.y)) {
          final int differenceX = actualLocation.x - desiredLocation.x;
          final int differenceY = actualLocation.y - desiredLocation.y;

          final int fixedOffsetX;
          final int fixedOffsetY;

          if (actualLocation.x > desiredLocation.x) {
            fixedOffsetX = desiredLocation.x - differenceX;
          } else {
            fixedOffsetX = desiredLocation.x + differenceX;
          }

          if (actualLocation.y > desiredLocation.y) {
            fixedOffsetY = desiredLocation.y - differenceY;
          } else {
            fixedOffsetY = desiredLocation.y + differenceY;
          }

          popupWindow.update(fixedOffsetX, fixedOffsetY, DONT_UPDATE_FLAG, DONT_UPDATE_FLAG);
        }
      }
    });
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
