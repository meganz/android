package mega.privacy.android.app.components.twemoji;

import static mega.privacy.android.app.components.twemoji.Utils.checkNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.text.EmojiCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;
import timber.log.Timber;

/* EmojiManager where an EmojiProvider can be installed for further usage.*/
public final class EmojiManager {

    private static final EmojiManager INSTANCE = new EmojiManager();
    private static final int GUESSED_UNICODE_AMOUNT = 3000;
    private static final int GUESSED_TOTAL_PATTERN_LENGTH = GUESSED_UNICODE_AMOUNT * 4;
    private static final Pattern SPACE_REMOVAL = Pattern.compile("\\s");
    private static final Comparator<String> STRING_LENGTH_COMPARATOR =
            (first, second) -> Integer.compare(second.length(), first.length());

    @SuppressLint("CheckResult")
    private static final EmojiReplacer DEFAULT_EMOJI_REPLACER = (context, text, emojiSize, defaultEmojiSize, fallback) -> {

        final EmojiManager emojiManager = EmojiManager.getInstance();
        final List<EmojiRange> findAllEmojis = emojiManager.findAllEmojis(text);
        EmojiCompat emojiCompat = EmojiCompat.get();
        boolean shouldProcess = emojiCompat.getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED;

        if (findAllEmojis.size() == 0) {
            if (shouldProcess) {
                emojiCompat.process(text);
            }
        } else {
            if (emojiManager.isOnlyEmojis(text.toString())) {
                for (int i = 0; i < findAllEmojis.size(); i++) {
                    final EmojiRange location = findAllEmojis.get(i);
                    text.setSpan(new EmojiSpan(context, location.emoji, emojiSize), location.start, location.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                for (int i = 0; i < findAllEmojis.size(); i++) {
                    EmojiRange currentLocation = findAllEmojis.get(i);

                    if (i == 0) {
                        if (shouldProcess) {
                            emojiCompat.process(text, 0, currentLocation.start);
                        }

                        if (i == findAllEmojis.size() - 1 && shouldProcess) {
                            emojiCompat.process(text, currentLocation.end, text.length());
                        }
                    } else if (i == findAllEmojis.size() - 1) {
                        EmojiRange previewLocation = findAllEmojis.get(i - 1);
                        if (shouldProcess) {
                            emojiCompat.process(text, previewLocation.end, currentLocation.start);
                            emojiCompat.process(text, currentLocation.end, text.length());
                        }
                    } else {
                        EmojiRange previewLocation = findAllEmojis.get(i - 1);
                        if (shouldProcess) {
                            emojiCompat.process(text, previewLocation.end, currentLocation.start);
                        }
                    }
                    text.setSpan(new EmojiSpan(context, currentLocation.emoji, emojiSize), currentLocation.start, currentLocation.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    };

    private final Map<String, Emoji> emojiMap = new LinkedHashMap<>(GUESSED_UNICODE_AMOUNT);
    private EmojiCategory[] categories;
    private Pattern emojiPattern;
    private Pattern emojiRepetitivePattern;
    private EmojiReplacer emojiReplacer;

    private EmojiManager() {
        // No instances apart from singleton.
    }

    public static EmojiManager getInstance() {
        return INSTANCE;
    }

    //Installs the given EmojiProvider.
    // NOTE: That only one can be present at any time.
    //param provider the provider that should be installed.
    public static void install(@NonNull final EmojiProvider provider) {
        INSTANCE.categories = checkNotNull(provider.getCategories(), "categories == null");
        INSTANCE.emojiMap.clear();
        INSTANCE.emojiReplacer = provider instanceof EmojiReplacer ? (EmojiReplacer) provider : DEFAULT_EMOJI_REPLACER;
        final List<String> unicodesForPattern = new ArrayList<>(GUESSED_UNICODE_AMOUNT);


        for (EmojiCategory category : INSTANCE.categories) {
            final Emoji[] emojis = checkNotNull(category.getEmojis(), "emojis == null");

            for (Emoji emoji : emojis) {
                final String unicode = emoji.getUnicode();
                final List<Emoji> variants = emoji.getVariants();
                INSTANCE.emojiMap.put(unicode, emoji);
                unicodesForPattern.add(unicode);

                for (Emoji variant: variants) {
                    final String variantUnicode = variant.getUnicode();
                    INSTANCE.emojiMap.put(variantUnicode, variant);
                    unicodesForPattern.add(variantUnicode);

                }
            }
        }
        if (unicodesForPattern.isEmpty()) {
            throw new IllegalArgumentException("Your EmojiProvider must at least have one category with at least one emoji.");
        }
        // We need to sort the unicodes by length so the longest one gets matched first.
        unicodesForPattern.sort(STRING_LENGTH_COMPARATOR);
        final StringBuilder patternBuilder = new StringBuilder(GUESSED_TOTAL_PATTERN_LENGTH);
        final int unicodesForPatternSize = unicodesForPattern.size();
        for (int i = 0; i < unicodesForPatternSize; i++) {
            patternBuilder.append(Pattern.quote(unicodesForPattern.get(i))).append('|');
        }
        final String regex = patternBuilder.deleteCharAt(patternBuilder.length() - 1).toString();
        INSTANCE.emojiPattern = Pattern.compile(regex);
        INSTANCE.emojiRepetitivePattern = Pattern.compile('(' + regex + ")+");
    }

    public static void destroy() {
        release();
        INSTANCE.emojiMap.clear();
        INSTANCE.categories = null;
        INSTANCE.emojiPattern = null;
        INSTANCE.emojiRepetitivePattern = null;
        INSTANCE.emojiReplacer = null;
    }

    public static void release() {
        for (final Emoji emoji : INSTANCE.emojiMap.values()) {
            emoji.destroy();
        }
    }

    public void replaceWithImages(final Context context, final Spannable text, final float emojiSize, final float defaultEmojiSize) {
        verifyInstalled();
        emojiReplacer.replaceWithImages(context, text, emojiSize, defaultEmojiSize, DEFAULT_EMOJI_REPLACER);
    }

    EmojiCategory[] getCategories() {
        verifyInstalled();
        return categories;
    }

    Pattern getEmojiRepetitivePattern() {
        return emojiRepetitivePattern;
    }

    /**
     * returns true when the string contains only emojis. Note that whitespace will be filtered out.
     */
    public boolean isOnlyEmojis(@Nullable final String text) {
        try {
            if (!TextUtils.isEmpty(text)) {
                final String inputWithoutSpaces = SPACE_REMOVAL.matcher(text).replaceAll(Matcher.quoteReplacement(""));
                return EmojiManager.getInstance()
                        .getEmojiRepetitivePattern()
                        .matcher(inputWithoutSpaces)
                        .matches();
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return false;
    }

    public int getNumEmojis(@Nullable final CharSequence text) {
        List<EmojiRange> emojis = findAllEmojis(text);
        return emojis.size();
    }

    public @NonNull
    List<EmojiRange> findAllEmojis(@Nullable final CharSequence text) {
        final List<EmojiRange> result = new ArrayList<>();
        try {
            verifyInstalled();
            if (!TextUtils.isEmpty(text)) {
                final Matcher matcher = emojiPattern.matcher(text);
                while (matcher.find()) {
                    final Emoji found = findEmoji(text.subSequence(matcher.start(), matcher.end()));
                    if (found != null) {
                        result.add(new EmojiRange(matcher.start(), matcher.end(), found));
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
            return result;
        }
        return result;
    }

    @Nullable
    Emoji findEmoji(@NonNull final CharSequence candidate) {
        verifyInstalled();
        // We need to call toString on the candidate, since the emojiMap may not find the requested entry otherwise, because
        // the type is different.

        return emojiMap.get(candidate.toString());
    }

    public Emoji getFirstEmoji(@NonNull final CharSequence candidate) {
        List<EmojiRange> emojis = findAllEmojis(candidate);
        if (emojis.size() > 0) return emojis.get(0).emoji;
        return null;

    }

    void verifyInstalled() {
        if (categories == null) {
            throw new IllegalStateException("Please install an EmojiProvider through the EmojiManager.install() method first.");
        }
    }
}