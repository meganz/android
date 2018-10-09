/*
 *
 * https://github.com/rockerhieu/emojicon
 *
 * Copyright 2014 Hieu Rocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mega.privacy.android.app.components;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.support.text.emoji.widget.EmojiAppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;

import io.github.rockerhieu.emojicon.EmojiconHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;


public class EmojiconTextView extends EmojiAppCompatTextView{
    public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT = 190;
    public static final int LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE = 260;
    private int mEmojiconSize;
    private int mEmojiconAlignment;
    private int mEmojiconTextSize;
    private int mTextStart = 0;
    private int mTextLength = -1;
    private boolean mUseSystemDefault = false;
    private Context mContext;
    private DisplayMetrics mOutMetrics;
    private int textViewMaxWidth;

    public EmojiconTextView(Context context) {
        super(context);
        mContext = context;
        init(null);
    }


    public EmojiconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    public EmojiconTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    private void init(AttributeSet attrs) {

        mEmojiconTextSize = (int) getTextSize();
        if (attrs == null) {
            mEmojiconSize = (int) getTextSize();
        } else {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Emojicon);
            mEmojiconSize = (int) a.getDimension(R.styleable.Emojicon_emojiconSize, getTextSize());
            mEmojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan.ALIGN_BASELINE);
            mTextStart = a.getInteger(R.styleable.Emojicon_emojiconTextStart, 0);
            mTextLength = a.getInteger(R.styleable.Emojicon_emojiconTextLength, -1);
            mUseSystemDefault = a.getBoolean(R.styleable.Emojicon_emojiconUseSystemDefault, false);
            a.recycle();
        }
    
        //get screen metrics
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
        mOutMetrics = new DisplayMetrics ();
        display.getMetrics(mOutMetrics);
        
        //get max width of the text view
        if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_LANDSCAPE, mOutMetrics);
        }else{
            textViewMaxWidth = Util.scaleWidthPx(LAST_MESSAGE_TEXTVIEW_WIDTH_PORTRAIT, mOutMetrics);
        }
        
        setText(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            EmojiconHandler.addEmojis(getContext(), builder, mEmojiconSize, mEmojiconAlignment, mEmojiconTextSize, mTextStart, mTextLength, mUseSystemDefault);
    
            //format text if too long
            text = TextUtils.ellipsize(builder, getPaint(), textViewMaxWidth, TextUtils.TruncateAt.END);
        }
        super.setText(text, type);
    }


    //Set the size of emojicon in pixels.
    public void setEmojiconSize(int pixels) {
        mEmojiconSize = pixels;
        super.setText(getText());
    }

    // Set whether to use system default emojicon
    public void setUseSystemDefault(boolean useSystemDefault) {
        mUseSystemDefault = useSystemDefault;
    }
}
