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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    protected Context context;
    protected Drawable mDivider;

    protected int left;
    protected int right;
    protected int childCount;

    private final Rect mBounds = new Rect();

    public SimpleDividerItemDecoration(Context context) {
        mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
        this.context = context;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        initItemDecoration(c, parent);

        for (int i = 0; i < childCount; i++) {
            drawDivider(c, parent, parent.getChildAt(i));
        }
    }

    protected void initItemDecoration(Canvas c, RecyclerView parent) {
        left = (int) MegaApplication.getInstance().getResources().getDimension(R.dimen.recycler_view_separator) + parent.getPaddingLeft();
        right = parent.getWidth() - parent.getPaddingRight();

        c.clipRect(left, parent.getPaddingTop(), right,
                parent.getHeight() - parent.getPaddingBottom());

        childCount = parent.getChildCount();
    }

    /**
     * Draws the item decoration.
     *
     * @param c        Canvas in which the dividers will be drawn.
     * @param parent   RecyclerView in which the item decorations will be set.
     * @param child    View which makes reference to each holder of the RecyclerView.
     */
    protected void drawDivider(Canvas c, RecyclerView parent, View child) {
        parent.getDecoratedBoundsWithMargins(child, mBounds);
        final int bottom = mBounds.bottom + Math.round(child.getTranslationY());
        final int top = bottom - mDivider.getIntrinsicHeight();

        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);
    }
}
