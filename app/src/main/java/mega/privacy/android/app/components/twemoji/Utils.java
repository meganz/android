package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class Utils {

  @NonNull static <T> T checkNotNull(@Nullable final T reference, final String message) {
    if (reference == null) {
      throw new IllegalArgumentException(message);
    }

    return reference;
  }

  private Utils() {
    throw new AssertionError("No instances.");
  }
}
