package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.*;

public class HeaderItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDividerNode;
    DisplayMetrics outMetrics;
    Context context;


    public HeaderItemDecoration(Context context, DisplayMetrics outMetrics) {

        mDividerNode = ContextCompat.getDrawable(context, R.drawable.line_divider);

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

            int top= child.getBottom() + params.bottomMargin;
            int bottom = top + mDividerNode.getIntrinsicHeight();

            mDividerNode.setBounds(left, top, right, bottom);

            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);

            if (viewType == ITEM_VIEW_TYPE) {
                mDividerNode.draw(c);
            }
        }
    }
}
