package mega.privacy.android.app.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class WrapTextView extends TextView {
    public WrapTextView(Context context) {
        super(context);
    }

    public WrapTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WrapTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Now fix width
        float max = 0;
        Layout layout = getLayout();
        for (int i = 0, size = layout.getLineCount(); i < size; i++) {
            final float lineWidth = layout.getLineMax(i);
            if (lineWidth > max) {
                max = lineWidth;
            }
        }

        final int height = getMeasuredHeight();
        final int width = (int) Math.ceil(max) + getCompoundPaddingLeft() + getCompoundPaddingRight();

        setMeasuredDimension(width, height);
    }
}
