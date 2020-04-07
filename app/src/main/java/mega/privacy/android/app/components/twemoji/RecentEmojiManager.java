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

public final class RecentEmojiManager implements RecentEmoji {
    static final int EMOJI_GUESS_SIZE = 5;
    static final int MAX_RECENTS = 40;
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
    }

    @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts", "checkstyle:nestedifdepth"})
    @NonNull
    @Override
    public Collection<Emoji> getRecentEmojis() {
        if (emojiList.size() == 0) {
            String savedRecentEmojis = null;

            if (type.equals(TYPE_EMOJI)) {
                savedRecentEmojis = getPreferences().getString(RECENT_EMOJIS, "");
            } else if (type.equals(TYPE_REACTION)) {
                savedRecentEmojis = getPreferences().getString(RECENT_REACTIONS, "");
            }

            if (savedRecentEmojis.length() > 0) {
                final StringTokenizer stringTokenizer = new StringTokenizer(savedRecentEmojis, EMOJI_DELIMITER);
                emojiList = new EmojiList(stringTokenizer.countTokens());

                while (stringTokenizer.hasMoreTokens()) {
                    final String token = stringTokenizer.nextToken();

                    final String[] parts = token.split(TIME_DELIMITER);

                    if (parts.length == 2) {
                        final Emoji emoji = EmojiManager.getInstance().findEmoji(parts[0]);

                        if (emoji != null && emoji.getLength() == parts[0].length()) {
                            final long timestamp = Long.parseLong(parts[1]);

                            emojiList.add(emoji, timestamp);
                        }
                    }
                }
            } else {
                emojiList = new EmojiList(0);
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
        if (emojiList.size() > 0) {
            final StringBuilder stringBuilder = new StringBuilder(emojiList.size() * EMOJI_GUESS_SIZE);

            for (int i = 0; i < emojiList.size(); i++) {
                final Data data = emojiList.get(i);
                stringBuilder.append(data.emoji.getUnicode())
                        .append(TIME_DELIMITER)
                        .append(data.timestamp)
                        .append(EMOJI_DELIMITER);
            }

            stringBuilder.setLength(stringBuilder.length() - EMOJI_DELIMITER.length());

            if (type.equals(TYPE_EMOJI)) {
                getPreferences().edit().putString(RECENT_EMOJIS, stringBuilder.toString()).apply();
            } else if (type.equals(TYPE_REACTION)) {
                getPreferences().edit().putString(RECENT_REACTIONS, stringBuilder.toString()).apply();
            }
        }
    }

    private SharedPreferences getPreferences() {
        if (type.equals(TYPE_REACTION)) {
            return context.getSharedPreferences(PREFERENCE_REACTION, Context.MODE_PRIVATE);
        }

        return context.getSharedPreferences(PREFERENCE_EMOJI, Context.MODE_PRIVATE);
    }

    static class EmojiList {
        static final Comparator<Data> COMPARATOR = (lhs, rhs) -> Long.valueOf(rhs.timestamp).compareTo(lhs.timestamp);

        @NonNull
        final List<Data> emojis;

        EmojiList(final int size) {
            emojis = new ArrayList<>(size);
        }

        void add(final Emoji emoji) {
            add(emoji, System.currentTimeMillis());
        }

        void add(final Emoji emoji, final long timestamp) {
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

    static class Data {
        final Emoji emoji;
        final long timestamp;

        Data(final Emoji emoji, final long timestamp) {
            this.emoji = emoji;
            this.timestamp = timestamp;
        }
    }
}
