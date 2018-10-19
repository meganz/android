package mega.privacy.android.app.components.twemoji;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;

public class EmojiTextView extends AppCompatTextView {

  private float emojiSize;

  public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT = 190;
  public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE = 260;
  private Context mContext;
  private DisplayMetrics mOutMetrics;
  private int textViewMaxWidth;

  public EmojiTextView(Context context) {
    super(context);
    mContext = context;
  }

  public EmojiTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;

    if (!isInEditMode()) {
      EmojiManager.getInstance().verifyInstalled();
    }

    final Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
    final float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;

    if (attrs == null) {
      emojiSize = defaultEmojiSize;
    } else {
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiTextView);

      try {
        emojiSize = a.getDimension(R.styleable.EmojiTextView_emojiSize, defaultEmojiSize);
      } finally {
        a.recycle();
      }
    }

    if (mContext instanceof ManagerActivityLollipop) {
      Display display = ((ManagerActivityLollipop)mContext).getWindowManager().getDefaultDisplay();
      mOutMetrics = new DisplayMetrics ();
      display.getMetrics(mOutMetrics);
      if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
        textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, mOutMetrics);
      }else{
        textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT, mOutMetrics);
      }
    }else if (mContext instanceof ChatActivityLollipop) {
      Display display = ((ChatActivityLollipop) mContext).getWindowManager().getDefaultDisplay();
      mOutMetrics = new DisplayMetrics();
      display.getMetrics(mOutMetrics);
      if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, mOutMetrics);
      } else {
        textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT, mOutMetrics);
      }
    }

    setText(getText());
  }

  @Override public void setText(CharSequence rawText, BufferType type) {

    CharSequence text = rawText == null ? "" : rawText;
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
    Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
    float defaultEmojiSize = fontMetrics.descent - fontMetrics.ascent;
    EmojiManager.getInstance().replaceWithImages(getContext(), spannableStringBuilder, emojiSize, defaultEmojiSize);

    if (mContext instanceof ManagerActivityLollipop) {
      //format text if too long
      CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
      super.setText(textF, type);
    }else{
      super.setText(spannableStringBuilder, type);
    }
  }

  //sets the emoji size in pixels and automatically invalidates the text and renders it with the new size
  public final void setEmojiSize(int pixels) {
    setEmojiSize(pixels, true);
  }

  // sets the emoji size in pixels and automatically invalidates the text and renders it with the new size when {@code shouldInvalidate} is true
  public final void setEmojiSize(int pixels, boolean shouldInvalidate) {
    emojiSize = pixels;

    if (shouldInvalidate) {
      setText(getText());
    }
  }
  public static void log(String message) {
    Util.log("EmojiTextView", message);
  }
}


