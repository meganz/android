package mega.privacy.android.app.components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;

public class BubbleDrawable extends Drawable {


    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    private Paint mPaint;
    private int mColor;

    private RectF mBoxRect;
    private int mBoxWidth;
    private int mBoxHeight;
    private float mCornerRad;
    private Rect mBoxPadding = new Rect();

    private Path mPointer;
    private int mPointerWidth;
    private int mPointerHeight;
    private int mPointerAlignment;

    public BubbleDrawable(int pointerAlignment, @ColorInt int argb) {
        setPointerAlignment(pointerAlignment);
        mColor = argb;
        initBubble();
    }

    public void setPadding(int left, int top, int right, int bottom) {
        mBoxPadding.left = left;
        mBoxPadding.top = top;
        mBoxPadding.right = right;
        mBoxPadding.bottom = bottom;
    }

    public void setCornerRadius(float cornerRad) {
        mCornerRad = cornerRad;
    }

    public void setPointerAlignment(int pointerAlignment) {
        if (pointerAlignment >= 0 && pointerAlignment <= 3) {
            mPointerAlignment = pointerAlignment;
        }
    }

    public void setPointerWidth(int pointerWidth) {
        mPointerWidth = pointerWidth;
    }

    public void setPointerHeight(int pointerHeight) {
        mPointerHeight = pointerHeight;
    }

    public void setColor(@ColorInt int argb) {
        mColor = argb;
    }

    private void initBubble() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);
        mCornerRad = 0;
        setPointerWidth(20);
        setPointerHeight(20);
    }

    private void updatePointerPath() {
        mPointer = new Path();
        mPointer.setFillType(Path.FillType.EVEN_ODD);

        // Set the starting point
        mPointer.moveTo(pointerHorizontalStart(), mBoxHeight);

        // Define the lines
        mPointer.rLineTo(mPointerWidth, 0);
        mPointer.rLineTo(-(mPointerWidth / 2), mPointerHeight);
        mPointer.rLineTo(-(mPointerWidth / 2), -mPointerHeight);
        mPointer.close();
    }

    private float pointerHorizontalStart() {
        float x = 0;
        switch (mPointerAlignment) {
            case LEFT:
                x = mCornerRad;
                break;
            case CENTER:
                x = (mBoxWidth / 2) - (mPointerWidth / 2);
                break;
            case RIGHT:
                x = mBoxWidth - mCornerRad - mPointerWidth;
        }
        return x;
    }

    @Override
    public void draw(Canvas canvas) {
        mBoxRect = new RectF(0.0f, 0.0f, mBoxWidth, mBoxHeight);
        canvas.drawRoundRect(mBoxRect, mCornerRad, mCornerRad, mPaint);
        updatePointerPath();
        canvas.drawPath(mPointer, mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.set(mBoxPadding);

        // Adjust the padding to include the height of the pointer
        padding.bottom += mPointerHeight;
        return true;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mBoxWidth = bounds.width();
        mBoxHeight = getBounds().height() - mPointerHeight;
        super.onBoundsChange(bounds);
    }
}
