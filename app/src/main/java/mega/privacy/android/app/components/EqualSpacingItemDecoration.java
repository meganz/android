package mega.privacy.android.app.components;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EqualSpacingItemDecoration extends RecyclerView.ItemDecoration {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int GRID = 2;
    private final int spacing;
    private int displayMode;

    public EqualSpacingItemDecoration(int spacing, int displayMode) {
        this.spacing = spacing;
        this.displayMode = displayMode;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildViewHolder(view).getAdapterPosition();
        int itemCount = state.getItemCount();
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        setSpacingForDirection(outRect, layoutManager, position, itemCount);
    }

    private void setSpacingForDirection(Rect outRect, RecyclerView.LayoutManager layoutManager, int position, int itemCount) {
        if (displayMode == -1) {
            displayMode = resolveDisplayMode(layoutManager);
        }

        switch (displayMode) {
            case HORIZONTAL:
                outRect.left = spacing;
                outRect.right = position == itemCount - 1 ? spacing : 0;
                outRect.top = spacing;
                outRect.bottom = spacing;
                break;

            case VERTICAL:
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = position == itemCount - 1 ? spacing : 0;
                break;

            case GRID:
                if (layoutManager instanceof GridLayoutManager) {
                    GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                    int cols = gridLayoutManager.getSpanCount();
                    int rows = itemCount / cols;

                    outRect.left = spacing;
                    outRect.right = position % cols == cols - 1 ? spacing : 0;
                    outRect.top = spacing;
                    outRect.bottom = position / cols == rows - 1 ? spacing : 0;
                }
                break;
        }
    }

    private int resolveDisplayMode(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager)
            return GRID;

        if (layoutManager.canScrollHorizontally())
            return HORIZONTAL;

        return VERTICAL;
    }
}