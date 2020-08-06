package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import static mega.privacy.android.app.utils.Constants.*;

public class HeaderItemDecoration extends SimpleDividerItemDecoration {

    public HeaderItemDecoration(Context context, DisplayMetrics outMetrics) {
        super(context, outMetrics);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        initItemDecoration(parent);

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);
            drawDivider(c, child, viewType);
        }
    }

    protected void drawDivider(Canvas c, View child, int viewType) {
        if (viewType == ITEM_VIEW_TYPE) {
            drawDivider(c, child);
        }
    }
}
