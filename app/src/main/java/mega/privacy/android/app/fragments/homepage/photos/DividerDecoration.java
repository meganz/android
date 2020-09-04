package mega.privacy.android.app.fragments.homepage.photos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

public class DividerDecoration extends RecyclerView.ItemDecoration {
    private final static float DEFAULT_HEIGHT = 1.0f;

    private final Paint mPaint;
    private int mHeightDp;
    private int left;

    public DividerDecoration(Context context) {
        this(context, context.getResources().getColor(R.color.grid_item_separator), DEFAULT_HEIGHT);
    }

    public DividerDecoration(Context context, int color, float heightDp) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
        mHeightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, context.getResources().getDisplayMetrics());
        left = (int) MegaApplication.getInstance().getResources().getDimension(R.dimen.recycler_view_separator);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == -1 ) return;
        int viewType = parent.getAdapter().getItemViewType(position);
        if (viewType == PhotoNodeItem.TYPE_PHOTO) {
            outRect.set(0, 0, 0, mHeightDp);
        } else {
            outRect.setEmpty();
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(view);
            if (position == -1) return;
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType == PhotoNodeItem.TYPE_PHOTO) {
                c.drawRect(left, view.getBottom(), view.getRight(), view.getBottom() + mHeightDp, mPaint);
            }
        }
    }
}
