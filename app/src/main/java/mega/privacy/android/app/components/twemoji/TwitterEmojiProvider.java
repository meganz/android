package mega.privacy.android.app.components.twemoji;

import androidx.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.category.ActivityCategory;
import mega.privacy.android.app.components.twemoji.category.FlagsCategory;
import mega.privacy.android.app.components.twemoji.category.FoodCategory;
import mega.privacy.android.app.components.twemoji.category.NatureCategory;
import mega.privacy.android.app.components.twemoji.category.ObjectsCategory;
import mega.privacy.android.app.components.twemoji.category.PeopleCategory;
import mega.privacy.android.app.components.twemoji.category.SymbolsCategory;
import mega.privacy.android.app.components.twemoji.category.TravelCategory;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;


public final class TwitterEmojiProvider implements EmojiProvider {
  @Override @NonNull public EmojiCategory[] getCategories() {
    return new EmojiCategory[] {
            new PeopleCategory(),
            new NatureCategory(),
            new FoodCategory(),
            new ActivityCategory(),
            new TravelCategory(),
            new ObjectsCategory(),
            new SymbolsCategory(),
            new FlagsCategory()
    };
  }
}

