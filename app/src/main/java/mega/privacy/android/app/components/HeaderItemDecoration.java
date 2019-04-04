package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.ShareContactsHeaderAdapter;

public class HeaderItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDividerNode;
    private Drawable mDividerHeader;
    DisplayMetrics outMetrics;
    Context context;


    public HeaderItemDecoration(Context context, DisplayMetrics outMetrics) {

        mDividerNode = ContextCompat.getDrawable(context, R.drawable.line_divider);
        mDividerHeader = ContextCompat.getDrawable(context, R.drawable.line_divider_camera_uploads_list);

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
            int bottomMonth = topMonth + mDividerHeader.getIntrinsicHeight();

            mDividerNode.setBounds(leftNode, topNode, rightNode, bottomNode);
            mDividerHeader.setBounds(leftMonth,topMonth,rightMonth,bottomMonth);

            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);

            if (viewType == ShareContactsHeaderAdapter.ITEM_VIEW_TYPE_NODE) {

                mDividerNode.draw(c);

            }else if(viewType == ShareContactsHeaderAdapter.ITEM_VIEW_TYPE_HEADER){

//                mDividerHeader.draw(c);

            }
        }
    }
}
