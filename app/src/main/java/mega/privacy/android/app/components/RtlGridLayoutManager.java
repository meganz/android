package mega.privacy.android.app.components;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;

public class RtlGridLayoutManager extends GridLayoutManager {

    public RtlGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    protected boolean isLayoutRTL() {
        return true;
    }
}
