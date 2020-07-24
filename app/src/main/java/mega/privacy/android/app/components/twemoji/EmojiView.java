package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.graphics.PorterDuff;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.content.res.AppCompatResources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.components.twemoji.listeners.RepeatListener;

import static mega.privacy.android.app.utils.Constants.*;

public final class EmojiView extends LinearLayout implements ViewPager.OnPageChangeListener {
  private static final long INITIAL_INTERVAL = TimeUnit.SECONDS.toMillis(1) / 2;
  private static final int NORMAL_INTERVAL = 50;

  @ColorInt private final int themeAccentColor;
  @ColorInt private final int themeIconColor;

  private ImageButton[] emojiTabs = null;
  private final EmojiPagerAdapter emojiPagerAdapter;
  private String type;
  @Nullable OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;

  private int emojiTabLastSelectedIndex = -1;

  public EmojiView(final Context context, final OnEmojiClickListener onEmojiClickListener, final OnEmojiLongClickListener onEmojiLongClickListener, @NonNull final RecentEmoji recentEmoji, @NonNull final VariantEmoji variantManager, final String typeView) {
    super(context);
    this.type = typeView;

    View.inflate(context, type.equals(TYPE_EMOJI) ? R.layout.emoji_view : R.layout.reactions_view, this);
    setBackgroundColor(ContextCompat.getColor(context, type.equals(TYPE_EMOJI) ? R.color.emoji_background : R.color.background_chat));

    setOrientation(VERTICAL);
    themeIconColor = ContextCompat.getColor(context, R.color.emoji_icons);
    final TypedValue value = new TypedValue();
    context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
    themeAccentColor = value.data;

    final ViewPager emojisPager = findViewById(R.id.emojis_pager);

    final View emojiDivider = findViewById(R.id.emoji_divider);
    emojiDivider.setBackgroundColor(ContextCompat.getColor(context, R.color.divider_upgrade_account));

    final LinearLayout emojisTab = findViewById(R.id.emojis_tab);
    emojisPager.addOnPageChangeListener(this);

    final EmojiCategory[] categories = EmojiManager.getInstance().getCategories();

    if (type.equals(TYPE_EMOJI)) {
      emojiTabs = new ImageButton[categories.length + 2];
      emojiTabs[0] = inflateButton(context, R.drawable.emoji_recent, emojisTab);

      for (int i = 0; i < categories.length; i++) {
        emojiTabs[i + 1] = inflateButton(context, categories[i].getIcon(), emojisTab);
      }
      emojiTabs[emojiTabs.length - 1] = inflateButton(context, R.drawable.emoji_backspace, emojisTab);
    } else if (type.equals(TYPE_REACTION)) {
      emojiTabs = new ImageButton[categories.length + 1];
      emojiTabs[0] = inflateButton(context, R.drawable.emoji_recent, emojisTab);

      for (int i = 0; i < categories.length; i++) {
        emojiTabs[i + 1] = inflateButton(context, categories[i].getIcon(), emojisTab);
      }
    }

    handleOnClicks(emojisPager);

    emojiPagerAdapter = new EmojiPagerAdapter(onEmojiClickListener, onEmojiLongClickListener, recentEmoji, variantManager);
    emojisPager.setAdapter(emojiPagerAdapter);

    final int startIndex = emojiPagerAdapter.numberOfRecentEmojis() > 0 ? 0 : 1;
    emojisPager.setCurrentItem(startIndex);
    onPageSelected(startIndex);
  }

  private void handleOnClicks(final ViewPager emojisPager) {
    int totalLength = type.equals(TYPE_REACTION) ? emojiTabs.length : emojiTabs.length - 1;

    for (int i = 0; i < totalLength; i++) {
      emojiTabs[i].setOnClickListener(new EmojiTabsClickListener(emojisPager, i));
    }

    if (type.equals(TYPE_EMOJI)) {
      emojiTabs[emojiTabs.length - 1].setOnTouchListener(new RepeatListener(INITIAL_INTERVAL, NORMAL_INTERVAL, view -> {
        if (onEmojiBackspaceClickListener != null) {
          onEmojiBackspaceClickListener.onEmojiBackspaceClick(view);
        }
      }));
    }
  }

  public void setOnEmojiBackspaceClickListener(@Nullable final OnEmojiBackspaceClickListener onEmojiBackspaceClickListener) {
    this.onEmojiBackspaceClickListener = onEmojiBackspaceClickListener;
  }

  private ImageButton inflateButton(final Context context, @DrawableRes final int icon, final ViewGroup parent) {
    final ImageButton button = (ImageButton) LayoutInflater.from(context).inflate(R.layout.emoji_category, parent, false);

    button.setImageDrawable(AppCompatResources.getDrawable(context, icon));
    button.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);

    parent.addView(button);

    return button;
  }

  @Override public void onPageSelected(final int i) {
    if (emojiTabLastSelectedIndex != i) {
      if (i == 0) {
        emojiPagerAdapter.invalidateRecentEmojis();
      }
      if (emojiTabLastSelectedIndex >= 0 && emojiTabLastSelectedIndex < emojiTabs.length) {
        emojiTabs[emojiTabLastSelectedIndex].setSelected(false);
        emojiTabs[emojiTabLastSelectedIndex].setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
      }

      emojiTabs[i].setSelected(true);
      emojiTabs[i].setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);

      emojiTabLastSelectedIndex = i;
    }
  }

  @Override public void onPageScrolled(final int i, final float v, final int i2) {
  }

  @Override public void onPageScrollStateChanged(final int i) {
  }

  static class EmojiTabsClickListener implements OnClickListener {
    private final ViewPager emojisPager;
    private final int position;

    EmojiTabsClickListener(final ViewPager emojisPager, final int position) {
      this.emojisPager = emojisPager;
      this.position = position;
    }

    @Override public void onClick(final View v) {
      emojisPager.setCurrentItem(position);
    }
  }
}