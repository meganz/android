package mega.privacy.android.app.components.twemoji;

import android.support.annotation.NonNull;

import mega.privacy.android.app.components.twemoji.category.ActivitiesCategory;
import mega.privacy.android.app.components.twemoji.category.AnimalsAndNatureCategory;
import mega.privacy.android.app.components.twemoji.category.FlagsCategory;
import mega.privacy.android.app.components.twemoji.category.FoodAndDrinkCategory;
import mega.privacy.android.app.components.twemoji.category.ObjectsCategory;
import mega.privacy.android.app.components.twemoji.category.SmileysAndPeopleCategory;
import mega.privacy.android.app.components.twemoji.category.SymbolsCategory;
import mega.privacy.android.app.components.twemoji.category.TravelAndPlacesCategory;
import mega.privacy.android.app.components.twemoji.emoji.EmojiCategory;


public final class TwitterEmojiProvider implements EmojiProvider {
  @Override @NonNull public EmojiCategory[] getCategories() {
    return new EmojiCategory[] {
      new SmileysAndPeopleCategory(),
      new AnimalsAndNatureCategory(),
      new FoodAndDrinkCategory(),
      new ActivitiesCategory(),
      new TravelAndPlacesCategory(),
      new ObjectsCategory(),
      new SymbolsCategory(),
      new FlagsCategory()
    };
  }
}
