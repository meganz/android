package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;

//Interface for a custom emoji implementation that can be used with EmojiManager
public interface EmojiProvider {
  //return The Array of categories
  @NonNull EmojiCategory[] getCategories();
}
