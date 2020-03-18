package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.GridView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;

class EmojiGridView extends GridView {
  protected EmojiArrayAdapter emojiArrayAdapter;

  EmojiGridView(final Context context) {
    super(context);

    final Resources resources = getResources();
    final int width = resources.getDimensionPixelSize(R.dimen.emoji_grid_view_column_width);
    final int spacing = resources.getDimensionPixelSize(R.dimen.emoji_grid_view_spacing);

    setColumnWidth(width);
    setHorizontalSpacing(spacing);
    setVerticalSpacing(spacing);
    setPadding(spacing, spacing, spacing, spacing);
    setNumColumns(AUTO_FIT);
    setClipToPadding(false);
    setVerticalScrollBarEnabled(false);
  }

  public EmojiGridView init(@Nullable final OnEmojiClickListener onEmojiClickListener,
                            @Nullable final OnEmojiLongClickListener onEmojiLongClickListener,
                            @NonNull final EmojiCategory category, @NonNull final VariantEmoji variantManager) {
    emojiArrayAdapter = new EmojiArrayAdapter(getContext(), category.getEmojis(), variantManager,
            onEmojiClickListener, onEmojiLongClickListener);

    setAdapter(emojiArrayAdapter);

    return this;
  }
}
