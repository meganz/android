/*
 * This code was taken from:
 *
 * http://stackoverflow.com/questions/31242812/how-to-add-divider-line-in-recyclerview-in-android
 * Username: Nilesh
 *
 */

package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    protected Context context;
    protected Drawable mDivider;
    protected DisplayMetrics outMetrics;

    protected int left;
    protected int right;
    protected int childCount;

    public SimpleDividerItemDecoration(Context context, DisplayMetrics outMetrics) {
        mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
        this.context = context;
        this.outMetrics = outMetrics;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        initItemDecoration(parent);

        for (int i = 0; i < childCount; i++) {
            drawDivider(c, parent.getChildAt(i));
        }
    }

    protected void initItemDecoration(RecyclerView parent) {
        left = (int) MegaApplication.getInstance().getResources().getDimension(R.dimen.recycler_view_separator);
        right = parent.getWidth() - parent.getPaddingRight();

        childCount = parent.getChildCount();
    }

    protected void drawDivider(Canvas c, View child) {

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

        int top = child.getBottom() + params.bottomMargin;
        int bottom = top + mDivider.getIntrinsicHeight();

        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);
    }
}
