package mega.privacy.android.app.components;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomizedGridLayoutManager extends GridLayoutManager {

    public CustomizedGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,int position) {

        int first = findFirstCompletelyVisibleItemPosition();

        if (first < position) {
            int last = findLastCompletelyVisibleItemPosition();
            double dif = last - first;
            double spanCount = getSpanCount();
            double visibleRows = Math.ceil(dif / spanCount);
            position = position + (int) (visibleRows * spanCount - (spanCount - 1));
            if (position > getItemCount()){
                position = getItemCount();
            }
        }

        super.smoothScrollToPosition(recyclerView,state,position);
    }
}