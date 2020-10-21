package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public final class RecentEmojiManager implements RecentEmoji {
    private static final int EMOJI_GUESS_SIZE = 5;
    private static final int MAX_RECENTS = 40;
    private static final String PREFERENCE_EMOJI = "emoji-recent-manager";
    private static final String PREFERENCE_REACTION = "reaction-recent-manager";
    private static final String TIME_DELIMITER = ";";
    private static final String EMOJI_DELIMITER = "~";
    private static final String RECENT_EMOJIS = "recent-emojis";
    private static final String RECENT_REACTIONS = "recent-reactions";

    @NonNull
    private final Context context;
    private String type;
    @NonNull
    private EmojiList emojiList = new EmojiList(0);

    public RecentEmojiManager(@NonNull final Context context, final String typeView) {
        this.context = context.getApplicationContext();
        this.type = typeView;
        getRecentEmojis();
    }

    @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts", "checkstyle:nestedifdepth"})
    @NonNull
    @Override
    public Collection<Emoji> getRecentEmojis() {
        if (emojiList.size() > 0) {
            return emojiList.getEmojis();
        }

        String savedRecentEmojis = getPreferences().getString(type.equals(TYPE_EMOJI) ? RECENT_EMOJIS : RECENT_REACTIONS, "");

        if (isTextEmpty(savedRecentEmojis)) {
            return new EmojiList(0).getEmojis();
        }

        final StringTokenizer stringTokenizer = new StringTokenizer(savedRecentEmojis, EMOJI_DELIMITER);
        emojiList = new EmojiList(stringTokenizer.countTokens());

        while (stringTokenizer.hasMoreTokens()) {
            final String token = stringTokenizer.nextToken();
            final String[] parts = token.split(TIME_DELIMITER);

            if (parts.length == 2) {
                final Emoji emoji = EmojiManager.getInstance().findEmoji(parts[0]);
                if (emoji != null && emoji.getLength() == parts[0].length()) {
                    emojiList.add(emoji, Long.parseLong(parts[1]));
                }
            }
        }
        return emojiList.getEmojis();
    }

    @Override
    public void addEmoji(@NonNull final Emoji emoji) {
        emojiList.add(emoji);
    }

    @Override
    public void persist() {
        if (emojiList.size() == 0)
            return;

        final StringBuilder stringBuilder = new StringBuilder(emojiList.size() * EMOJI_GUESS_SIZE);

        for (int i = 0; i < emojiList.size(); i++) {
            final Data data = emojiList.get(i);
            stringBuilder.append(data.emoji.getUnicode())
                    .append(TIME_DELIMITER)
                    .append(data.timestamp)
                    .append(EMOJI_DELIMITER);
        }

        stringBuilder.setLength(stringBuilder.length() - EMOJI_DELIMITER.length());
        getPreferences().edit().putString(type.equals(TYPE_EMOJI) ? RECENT_EMOJIS : RECENT_REACTIONS, stringBuilder.toString()).apply();
    }

    /**
     * Obtain the preferences.
     *
     * @return The preferences related to reactions.
     */
    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(type.equals(TYPE_REACTION) ? PREFERENCE_REACTION : PREFERENCE_EMOJI, Context.MODE_PRIVATE);
    }

    private static class EmojiList {
        static final Comparator<Data> COMPARATOR = (lhs, rhs) -> Long.valueOf(rhs.timestamp).compareTo(lhs.timestamp);

        @NonNull
        final List<Data> emojis;

        EmojiList(final int size) {
            emojis = new ArrayList<>(size);
        }

        private void add(final Emoji emoji) {
            add(emoji, System.currentTimeMillis());
        }

        private void add(final Emoji emoji, final long timestamp) {
            final Iterator<Data> iterator = emojis.iterator();
            final Emoji emojiBase = emoji.getBase();

            while (iterator.hasNext()) {
                final Data data = iterator.next();
                if (data.emoji.getBase().equals(emojiBase)) {
                    iterator.remove();
                }
            }

            emojis.add(0, new Data(emoji, timestamp));

            if (emojis.size() > MAX_RECENTS) {
                emojis.remove(MAX_RECENTS);
            }
        }

        Collection<Emoji> getEmojis() {
            Collections.sort(emojis, COMPARATOR);
            final Collection<Emoji> sortedEmojis = new ArrayList<>(emojis.size());

            for (final Data data : emojis) {
                sortedEmojis.add(data.emoji);
            }
            return sortedEmojis;
        }

        int size() {
            return emojis.size();
        }
        Data get(final int index) {
            return emojis.get(index);
        }
    }

    private static class Data {
        final Emoji emoji;
        final long timestamp;

        Data(final Emoji emoji, final long timestamp) {
            this.emoji = emoji;
            this.timestamp = timestamp;
        }
    }
}
