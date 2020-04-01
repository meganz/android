package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;

/**
 * Interface for providing some custom implementation for variant emojis.
 *
 * @since 0.5.0
 */
public interface VariantEmoji {
  /**
   * Returns the variant for the passed emoji. Could be loaded from a database, shared preferences or just hard
   * coded.<br>
   *
   * This method will be called more than one time hence it is recommended to hold a collection of
   * desired emojis.
   *
   * @param desiredEmoji The emoji to retrieve the variant for. If none is found,
   *                     the passed emoji should be returned.
   * @since 0.5.0
   */
  @NonNull Emoji getVariant(Emoji desiredEmoji);

  /**
   * Should add the emoji to the variants. After calling this method, {@link #getVariant(Emoji)}
   * should return the emoji that was just added.
   *
   * @param newVariant The new variant to save.
   * @since 0.5.0
   */
  void addVariant(@NonNull Emoji newVariant);

  /**
   * Should persist all emojis.
   *
   * @since 0.5.0
   */
  void persist();
}
