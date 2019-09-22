package mega.privacy.android.app.components.twemoji.emoji;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.graphics.Color.WHITE;
import static java.util.Arrays.asList;

public class Emoji implements Serializable {

  private static final long serialVersionUID = 3L;
  @NonNull private final String unicode;
  @DrawableRes private final int resource;
  @NonNull private final List<Emoji> variants;
  @Nullable private Emoji base;

  public Emoji(@NonNull final int[] codePoints, @DrawableRes final int resource) {
    this(codePoints, resource, new Emoji[0]);
  }

  public Emoji(final int codePoint, @DrawableRes final int resource) {
    this(codePoint, resource, new Emoji[0]);
  }

  public Emoji(final int codePoint, @DrawableRes final int resource, final Emoji... variants) {
    this(new int[]{codePoint}, resource, variants);
  }

  public Emoji(@NonNull final int[] codePoints, @DrawableRes final int resource, final Emoji... variants) {
    this.unicode = new String(codePoints, 0, codePoints.length);
    this.resource = resource;
    // asList seems to always allocate a new object, even for empty lists.
    this.variants = variants.length == 0 ? Collections.<Emoji>emptyList() : asList(variants);
    for (final Emoji variant : variants) {
      variant.base = this;
    }
  }

  @NonNull public String getUnicode() {

    return unicode;
  }

//  /* deprecated Please migrate to getDrawable(). May return -1 in the future for providers that don't use resources*/
//  @Deprecated @DrawableRes public int getResource() {
//    log("***** getResource()");
//    return resource;
//  }

  @NonNull public Drawable getDrawable(final Context context) {

//    Drawable drawableInitial = AppCompatResources.getDrawable(context, resource);
    Bitmap bitmapInitial = BitmapFactory.decodeResource(context.getResources(), resource);
    Bitmap bitmapFinal = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bitmapFinal);
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setColor(WHITE);
    c.drawBitmap(bitmapInitial, 3f, 3f, null);
    Drawable drawableFinal = new BitmapDrawable(context.getResources(), bitmapFinal);
    return drawableFinal;
  }

  @NonNull public List<Emoji> getVariants() {
    return new ArrayList<>(variants);
  }

  @NonNull public Emoji getBase() {
    Emoji result = this;
    while (result.base != null) {
      result = result.base;
    }
    return result;
  }

  public int getLength() {
    return unicode.length();
  }

  public boolean hasVariants() {
    return !variants.isEmpty();
  }

  public void destroy() {
    // For inheritors to override.
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Emoji emoji = (Emoji) o;
    return resource == emoji.resource
            && unicode.equals(emoji.unicode)
            && variants.equals(emoji.variants);
  }

  @Override public int hashCode() {
    int result = unicode.hashCode();
    result = 31 * result + resource;
    result = 31 * result + variants.hashCode();
    return result;
  }
}