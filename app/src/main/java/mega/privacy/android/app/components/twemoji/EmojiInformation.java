package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;

import java.util.List;

public final class EmojiInformation {
  public final boolean isOnlyEmojis;
  @NonNull public final List<EmojiRange> emojis;

  EmojiInformation(final boolean isOnlyEmojis, @NonNull final List<EmojiRange> emojis) {
    this.isOnlyEmojis = isOnlyEmojis;
    this.emojis = emojis;
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final EmojiInformation that = (EmojiInformation) o;
    return isOnlyEmojis == that.isOnlyEmojis && emojis.equals(that.emojis);
  }

  @Override public int hashCode() {
    int result = isOnlyEmojis ? 1 : 0;
    result = 31 * result + emojis.hashCode();
    return result;
  }
}
