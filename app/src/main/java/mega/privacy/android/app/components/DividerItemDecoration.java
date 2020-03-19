package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import mega.privacy.android.app.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDividerNode;
    private Drawable mDividerMonth;
    DisplayMetrics outMetrics;
    Context context;

    public static final int ITEM_VIEW_TYPE_NODE= 0;
    public static final int ITEM_VIEW_TYPE_MONTH = 1;

    public DividerItemDecoration(Context context, DisplayMetrics outMetrics) {

        mDividerNode = ContextCompat.getDrawable(context, R.drawable.line_divider);
        mDividerMonth = ContextCompat.getDrawable(context, R.drawable.line_divider_camera_uploads_list);

        this.outMetrics = outMetrics;
        this.context = context;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int leftNode = (int) context.getResources().getDimension(R.dimen.recycler_view_separator);
        int rightNode = parent.getWidth() - parent.getPaddingRight();

        int rightMonth = parent.getWidth() - parent.getPaddingRight();
        int leftMonth= 0;

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int topNode = child.getBottom() + params.bottomMargin;
            int bottomNode = topNode + mDividerNode.getIntrinsicHeight();

            int topMonth = child.getTop();
            int bottomMonth = topMonth + mDividerMonth.getIntrinsicHeight();

            mDividerNode.setBounds(leftNode, topNode, rightNode, bottomNode);
            mDividerMonth.setBounds(leftMonth,topMonth,rightMonth,bottomMonth);

            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);

            if (viewType == ITEM_VIEW_TYPE_NODE) {

                mDividerNode.draw(c);

            }else if(viewType == ITEM_VIEW_TYPE_MONTH){

                mDividerMonth.draw(c);

            }
        }
    }
}
