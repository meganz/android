package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import static mega.privacy.android.app.utils.Constants.*;

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

    @NonNull
    @Override
    public Emoji getVariant(final Emoji desiredEmoji) {
        if (variantsEmojiList.isEmpty()) {
            initFromSharedPreferences();
        }

        final Emoji baseEmoji = desiredEmoji.getBase();

        for (int i = 0; i < variantsEmojiList.size(); i++) {
            final Emoji emoji = variantsEmojiList.get(i);

            if (baseEmoji.equals(emoji.getBase())) {
                return emoji;
            }
        }

        return desiredEmoji;
    }

    @Override
    public void addVariant(@NonNull final Emoji newVariant) {
        final Emoji newVariantBase = newVariant.getBase();

        if (variantsEmojiList.size() == 0) {
            variantsEmojiList.add(newVariant);
        }

        for (int i = 0; i < variantsEmojiList.size(); i++) {
            final Emoji variant = variantsEmojiList.get(i);

            if (variant.getBase().equals(newVariantBase)) {
                if (variant.equals(newVariant)) {
                    return;
                }
                variantsEmojiList.remove(i);
                variantsEmojiList.add(newVariant);
                return;
            }
        }
    }

    @Override
    public void persist() {
        if (variantsEmojiList.size() > 0) {
            final StringBuilder stringBuilder = new StringBuilder(variantsEmojiList.size() * EMOJI_GUESS_SIZE);

            for (int i = 0; i < variantsEmojiList.size(); i++) {
                stringBuilder.append(variantsEmojiList.get(i).getUnicode()).append(EMOJI_DELIMITER);
            }

            stringBuilder.setLength(stringBuilder.length() - EMOJI_DELIMITER.length());
            if (type.equals(TYPE_EMOJI)) {
                getPreferences().edit().putString(VARIANT_EMOJIS, stringBuilder.toString()).apply();
            } else if (type.equals(TYPE_REACTION)) {
                getPreferences().edit().putString(VARIANT_REACTIONS, stringBuilder.toString()).apply();
            }

        } else {
            if (type.equals(TYPE_EMOJI)) {
                getPreferences().edit().remove(VARIANT_EMOJIS).apply();
            } else if (type.equals(TYPE_REACTION)) {
                getPreferences().edit().remove(VARIANT_REACTIONS).apply();
            }
        }
    }

    private void initFromSharedPreferences() {
        String savedRecentVariants = null;
        if (type.equals(TYPE_EMOJI)) {
            savedRecentVariants = getPreferences().getString(VARIANT_EMOJIS, "");
        } else if (type.equals(TYPE_REACTION)) {
            savedRecentVariants = getPreferences().getString(VARIANT_REACTIONS, "");
        }

        if (savedRecentVariants.length() > 0) {
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
    }

    private SharedPreferences getPreferences() {
        if (type.equals(TYPE_REACTION)) {
            return context.getSharedPreferences(PREFERENCE_REACTION, Context.MODE_PRIVATE);
        }

        return context.getSharedPreferences(PREFERENCE_EMOJI, Context.MODE_PRIVATE);
    }
}
