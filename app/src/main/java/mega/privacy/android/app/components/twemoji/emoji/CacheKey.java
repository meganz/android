package mega.privacy.android.app.components.twemoji.emoji;

public final class CacheKey {
  private final int x;
  private final int y;

  public CacheKey(final int x, final int y) {
    this.x = x;
    this.y = y;
  }

  @Override public boolean equals(final Object o) {
    return o instanceof CacheKey
        && x == ((CacheKey) o).x
        && y == ((CacheKey) o).y;
  }

  @Override public int hashCode() {
    return (x << 16) ^ y;
  }
}
