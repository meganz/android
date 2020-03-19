package mega.privacy.android.app.components.twemoji;

import androidx.annotation.DimenRes;
import androidx.annotation.Px;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;

/* Interface used to allow custom EmojiEditText objects on another project.The implementer must be a class that inherits from android.view.View*/
public interface EmojiEditTextInterface {
  void backspace();

  void input(Emoji emoji);

  /** sets the emoji size in pixels and automatically invalidates the text and renders it with the new size */
  float getEmojiSize();

  /** sets the emoji size in pixels and automatically invalidates the text and renders it with the new size */
  void setEmojiSize(@Px int pixels);

  /** sets the emoji size in pixels and automatically invalidates the text and renders it with the new size when {@code shouldInvalidate} is true */
  void setEmojiSize(@Px int pixels, boolean shouldInvalidate);

  /** sets the emoji size in pixels with the provided resource and automatically invalidates the text and renders it with the new size */
  void setEmojiSizeRes(@DimenRes int res);

  /** sets the emoji size in pixels with the provided resource and invalidates the text and renders it with the new size when {@code shouldInvalidate} is true */
  void setEmojiSizeRes(@DimenRes int res, boolean shouldInvalidate);
}
