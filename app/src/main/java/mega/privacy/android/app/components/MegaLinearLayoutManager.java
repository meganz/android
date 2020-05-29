package mega.privacy.android.app.components;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.AttributeSet;

public class MegaLinearLayoutManager extends LinearLayoutManager {
    /**
     * Disable predictive animations. There is a bug in RecyclerView which causes views that
     * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
     * adapter size has decreased since the ViewHolder was recycled.
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    public MegaLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MegaLinearLayoutManager(Context context) {
        super(context);
    }

    public MegaLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }
}
