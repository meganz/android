package mega.privacy.android.app.components.twemoji.emoji;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

/*Interface for defining a category.*/
public interface EmojiCategory {
  /*Returns all of the emojis it can display*/
  @NonNull Emoji[] getEmojis();

  /*Returns the icon of the category that should be displayed*/
  @DrawableRes int getIcon();
}
