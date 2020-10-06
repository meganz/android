package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

@SuppressWarnings("PMD.ForLoopCanBeForeach")
public final class VariantEmojiManager implements VariantEmoji {
    private static final int EMOJI_GUESS_SIZE = 5;
    private static final String PREFERENCE_EMOJI = "variant-emoji-manager";
    private static final String PREFERENCE_REACTION = "variant-reaction-manager";
    private static final String EMOJI_DELIMITER = "~";
    private static final String VARIANT_EMOJIS = "variant-emojis";
    private static final String VARIANT_REACTIONS = "variant-reactions";
    @NonNull
    private final Context context;
    private String type;

    @NonNull
    private List<Emoji> variantsEmojiList = new ArrayList<>(0);

    public VariantEmojiManager(@NonNull final Context context, final String typeView) {
        this.context = context.getApplicationContext();
        this.type = typeView;
    }

    /**
     * Method for obtaining the variant of an Emoji.
     *
     * @param desiredEmoji The emoji to retrieve the variant for. If none is found,
     *                     the passed emoji should be returned.
     * @return The Emoji
     */
    @NonNull
    @Override
    public Emoji getVariant(final Emoji desiredEmoji) {
        if (variantsEmojiList.isEmpty()) {
            initFromSharedPreferences();
        }

        final Emoji baseEmoji = desiredEmoji.getBase();
        for (Emoji emoji : variantsEmojiList) {
            if (baseEmoji.equals(emoji.getBase())) {
                return emoji;
            }
        }

        return desiredEmoji;
    }

    /**
     * Method for adding a variant of an emoji.
     *
     * @param newVariant The new variant to save.
     */
    @Override
    public void addVariant(@NonNull final Emoji newVariant) {
        final Emoji newVariantBase = newVariant.getBase();

        if (variantsEmojiList.size() == 0) {
            variantsEmojiList.add(newVariant);
        }

        for (Emoji variant : variantsEmojiList) {
            if (variant.getBase().equals(newVariantBase)) {
                if (!variant.equals(newVariant)) {
                    variantsEmojiList.remove(variant);
                    variantsEmojiList.add(newVariant);
                }
            }
            return;
        }
    }

    /**
     * Method to save the emoji variable used to display in the recent section.
     */
    @Override
    public void persist() {
        if (variantsEmojiList.size() > 0) {
            final StringBuilder stringBuilder = new StringBuilder(variantsEmojiList.size() * EMOJI_GUESS_SIZE);

            for (Emoji emoji : variantsEmojiList) {
                stringBuilder.append(emoji.getUnicode()).append(EMOJI_DELIMITER);
            }

            stringBuilder.setLength(stringBuilder.length() - EMOJI_DELIMITER.length());
            getPreferences().edit().putString(type.equals(TYPE_EMOJI) ? VARIANT_EMOJIS : VARIANT_REACTIONS, stringBuilder.toString()).apply();
        } else {
            getPreferences().edit().remove(type.equals(TYPE_EMOJI) ? VARIANT_EMOJIS : VARIANT_REACTIONS).apply();
        }
    }

    /**
     * Method to initialize the emojis used that will be shown in the Recents keyboard section.
     */
    private void initFromSharedPreferences() {
        String savedRecentVariants = getPreferences().getString(type.equals(TYPE_EMOJI) ? VARIANT_EMOJIS : VARIANT_REACTIONS, "");
        if (isTextEmpty(savedRecentVariants))
            return;

        final StringTokenizer stringTokenizer = new StringTokenizer(savedRecentVariants, EMOJI_DELIMITER);
        variantsEmojiList = new ArrayList<>(stringTokenizer.countTokens());

        while (stringTokenizer.hasMoreTokens()) {
            final String token = stringTokenizer.nextToken();
            final Emoji emoji = EmojiManager.getInstance().findEmoji(token);

            if (emoji != null && emoji.getLength() == token.length()) {
                variantsEmojiList.add(emoji);
            }
        }
    }

    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(type.equals(TYPE_REACTION) ? PREFERENCE_REACTION : PREFERENCE_EMOJI, Context.MODE_PRIVATE);
    }
}
