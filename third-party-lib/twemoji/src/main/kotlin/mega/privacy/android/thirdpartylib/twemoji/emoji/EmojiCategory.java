package mega.privacy.android.thirdpartylib.twemoji.emoji;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/*Interface for defining a category.*/
public interface EmojiCategory {
  /*Returns all of the emojis it can display*/
  @NonNull Emoji[] getEmojis();

  /*Returns the icon of the category that should be displayed*/
  @DrawableRes int getIcon();
}
