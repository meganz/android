package mega.privacy.android.app.components.twemoji;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class EmojiManagerShortcodes {
    private static final String EMOJI_SHORTCODES = "emojisshortcodes/emoji.json";
    static List<EmojiShortcodes> emojiData;

    public static void initEmojiData(Context context) {
        if (emojiData == null || emojiData.size() < 1)
            try {
                Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setLenient().create();
                BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(EMOJI_SHORTCODES)));
                emojiData = gson.fromJson(reader, new TypeToken<ArrayList<EmojiShortcodes>>() {
                }.getType());
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
