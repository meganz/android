/*
 * This code was taken from:
 * 
 * http://stackoverflow.com/questions/31242812/how-to-add-divider-line-in-recyclerview-in-android
 * Username: Nilesh
 * 
 */

package mega.privacy.android.app.components;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    DisplayMetrics outMetrics;
    Context context;
 
    public SimpleDividerItemDecoration(Context context, DisplayMetrics outMetrics) {
        mDivider = context.getResources().getDrawable(R.drawable.line_divider);
        this.outMetrics = outMetrics;
        this.context = context;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int left = (int) context.getResources().getDimension(R.dimen.recycler_view_separator);
        int right = parent.getWidth() - parent.getPaddingRight();
 
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
 
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
 
            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();
 
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }
}
