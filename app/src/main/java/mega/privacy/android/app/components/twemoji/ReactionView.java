package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;

public class ReactionView extends LinearLayout implements ViewPager.OnPageChangeListener {
    private static final long INITIAL_INTERVAL = TimeUnit.SECONDS.toMillis(1) / 2;
    private static final int NORMAL_INTERVAL = 50;

    @ColorInt
    private final int themeAccentColor;
    @ColorInt
    private final int themeIconColor;

    private final ImageButton[] emojiTabs;
    private final EmojiPagerAdapter emojiPagerAdapter;

    private int emojiTabLastSelectedIndex = -1;

    public ReactionView(final Context context, final OnEmojiClickListener onEmojiClickListener, final OnEmojiLongClickListener onEmojiLongClickListener, @NonNull final VariantEmoji variantManager) {
        super(context);

        View.inflate(context, R.layout.reactions_view, this);

        setOrientation(VERTICAL);
        setBackgroundColor(ContextCompat.getColor(context, R.color.background_chat));

        themeIconColor = ContextCompat.getColor(context, R.color.emoji_icons);
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        themeAccentColor = value.data;

        final ViewPager emojisPager = findViewById(R.id.reactions_pager);

        final View emojiDivider = findViewById(R.id.reactions_divider);
        emojiDivider.setBackgroundColor(ContextCompat.getColor(context, R.color.divider_upgrade_account));

        final LinearLayout emojisTab = findViewById(R.id.reactions_tab);
        emojisPager.addOnPageChangeListener(this);

        final EmojiCategory[] categories = EmojiManager.getInstance().getCategories();

        emojiTabs = new ImageButton[categories.length + 2];
        for (int i = 0; i < categories.length; i++) {
            emojiTabs[i + 1] = inflateButton(context, categories[i].getIcon(), emojisTab);
        }
        handleOnClicks(emojisPager);

        emojiPagerAdapter = new EmojiPagerAdapter(onEmojiClickListener, onEmojiLongClickListener, null, variantManager);
        emojisPager.setAdapter(emojiPagerAdapter);
        final int startIndex = 1;
        emojisPager.setCurrentItem(startIndex);
        onPageSelected(startIndex);
    }

    private void handleOnClicks(final ViewPager emojisPager) {
        for (int i = 1; i < emojiTabs.length - 1; i++) {
            emojiTabs[i].setOnClickListener(new EmojiTabsClickListener(emojisPager, i));
        }
    }

    private ImageButton inflateButton(final Context context, @DrawableRes final int icon, final ViewGroup parent) {
        final ImageButton button = (ImageButton) LayoutInflater.from(context).inflate(R.layout.emoji_category, parent, false);

        button.setImageDrawable(AppCompatResources.getDrawable(context, icon));
        button.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
        parent.addView(button);

        return button;
    }

    @Override
    public void onPageSelected(final int i) {
        if (emojiTabLastSelectedIndex != i) {
            if (emojiTabLastSelectedIndex >= 0 && emojiTabLastSelectedIndex < emojiTabs.length) {
                emojiTabs[emojiTabLastSelectedIndex].setSelected(false);
                emojiTabs[emojiTabLastSelectedIndex].setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
            }

            emojiTabs[i].setSelected(true);
            emojiTabs[i].setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);

            emojiTabLastSelectedIndex = i;
        }
    }

    @Override
    public void onPageScrolled(final int i, final float v, final int i2) {
    }

    @Override
    public void onPageScrollStateChanged(final int i) {
    }

    static class EmojiTabsClickListener implements OnClickListener {
        private final ViewPager emojisPager;
        private final int position;

        EmojiTabsClickListener(final ViewPager emojisPager, final int position) {
            this.emojisPager = emojisPager;
            this.position = position;
        }

        @Override
        public void onClick(final View v) {
            emojisPager.setCurrentItem(position);
        }
    }


}
