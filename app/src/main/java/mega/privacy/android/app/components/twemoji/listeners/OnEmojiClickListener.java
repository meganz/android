package mega.privacy.android.app.components.twemoji.listeners;

import androidx.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.EmojiImageView;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;

public interface OnEmojiClickListener {
  void onEmojiClick(@NonNull EmojiImageView emoji, @NonNull Emoji imageView);
}
