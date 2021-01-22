package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

import mega.privacy.android.app.R;

public class PositionDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable divider;
    private DisplayMetrics outMetrics;
    private Context context;
    private int position = -1;
    private boolean drawAllDividers = false;

    public PositionDividerItemDecoration(Context context, DisplayMetrics outMetrics) {
        divider = ContextCompat.getDrawable(context, R.drawable.line_divider);

        this.outMetrics = outMetrics;
        this.context = context;
    }

    public PositionDividerItemDecoration (Context context, DisplayMetrics outMetrics, int position) {
        divider = ContextCompat.getDrawable(context, R.drawable.line_divider);

        this.outMetrics = outMetrics;
        this.context = context;
        this.position = position;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int left = (int) context.getResources().getDimension(R.dimen.recycler_view_separator);
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);

            if (draw(pos)) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + divider.getIntrinsicHeight();

                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }
    }

    public void setDrawAllDividers(boolean drawAllDividers) {
        this.drawAllDividers = drawAllDividers;
    }

    private boolean draw(int pos) {
        if (drawAllDividers) {
            return true;
        }

        if (pos == 0 || (position != -1 && position-1 == pos)
                || (position != -1 && position == pos)) {
            return false;
        }
        return true;
    }
}
