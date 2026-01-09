package mega.privacy.android.thirdpartylib.twemoji;

import androidx.annotation.NonNull;

import mega.privacy.android.thirdpartylib.twemoji.emoji.EmojiCategory;

//Interface for a custom emoji implementation that can be used with EmojiManager
public interface EmojiProvider {
  //return The Array of categories
  @NonNull EmojiCategory[] getCategories();
}
