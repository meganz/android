package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.text.Spannable;

//EmojiProviders can implement this interface to perform text emoji image replacement in a more efficient way.
 //For instance, the GooogleCompatEmojiProvider calls the corresponding AppCompat Emoji
 // Support library replace method directly for emoji in the default size
public interface EmojiReplacer {
  void replaceWithImages(Context context, Spannable text, float emojiSize, float defaultEmojiSize, EmojiReplacer fallback);
}
