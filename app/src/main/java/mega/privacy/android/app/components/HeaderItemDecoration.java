package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import static mega.privacy.android.app.utils.Constants.*;

public class HeaderItemDecoration extends SimpleDividerItemDecoration {

    public HeaderItemDecoration(Context context) {
        super(context);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        initItemDecoration(c, parent);

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);
            drawDivider(c, parent, child, viewType);
        }
    }

    /**
     * Draws the item decoration.
     *
     * @param c        Canvas in which the dividers will be drawn.
     * @param parent   RecyclerView in which the item decorations will be set.
     * @param child    View which makes reference to each holder of the RecyclerView.
     * @param viewType The type of holder. Depending on it, the divider will be drawn or not.
     */
    protected void drawDivider(Canvas c, RecyclerView parent, View child, int viewType) {
        if (viewType == ITEM_VIEW_TYPE) {
            drawDivider(c, parent, child);
        }
    }
}
