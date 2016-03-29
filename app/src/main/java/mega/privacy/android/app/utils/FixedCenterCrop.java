package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedCenterCrop extends ImageView
{
    public FixedCenterCrop(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final Drawable d = this.getDrawable();

        if (d != null) {
            // ceil not round - avoid thin vertical gaps along the left/right edges
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = (int) Math.ceil(width * (float) d.getIntrinsicHeight() / d.getIntrinsicWidth());
            this.setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
    
    
//    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
//    {
//        final Drawable d = this.getDrawable();
//
//        if(d != null) {
//            int height = MeasureSpec.getSize(heightMeasureSpec);
//            int width = MeasureSpec.getSize(widthMeasureSpec);
//
//            if(width >= height)
//                height = (int) Math.ceil(width * (float) d.getIntrinsicHeight() / d.getIntrinsicWidth());
//            else
//                width = (int) Math.ceil(height * (float) d.getIntrinsicWidth() / d.getIntrinsicHeight());
//
//            this.setMeasuredDimension(width, height);
//
//        } else {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//    }
}
