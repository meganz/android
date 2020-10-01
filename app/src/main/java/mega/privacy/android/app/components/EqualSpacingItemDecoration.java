package mega.privacy.android.app.components;

import android.graphics.Rect;
import android.view.View;
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
        setSpacingForDirection(outRect, position, itemCount);
    }

    private void setSpacingForDirection(Rect outRect, int position, int itemCount) {
        switch (displayMode) {
            case HORIZONTAL:
                outRect.left = outRect.bottom = outRect.top = spacing;
                outRect.right = position == itemCount - 1 ? spacing : 0;
                break;

            case VERTICAL:
                outRect.right = outRect.bottom = outRect.top = spacing;
                outRect.left = position == itemCount - 1 ? spacing : 0;
                break;
        }
    }
}