package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;

public final class EmojiRange {
  public final int start;
  public final int end;
  public final Emoji emoji;

  EmojiRange(final int start, final int end, @NonNull final Emoji emoji) {
    this.start = start;
    this.end = end;
    this.emoji = emoji;
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final EmojiRange that = (EmojiRange) o;
    return start == that.start && end == that.end && emoji.equals(that.emoji);
  }

  @Override public int hashCode() {
    int result = start;
    result = 31 * result + end;
    result = 31 * result + emoji.hashCode();
    return result;
  }
}
