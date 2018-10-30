package mega.privacy.android.app.components;

import android.view.View;

public class OnScrollChangeListenerAdapter implements View.OnScrollChangeListener {

    private final ListenScrollChangesHelper.OnScrollChangeListenerCompat mOnScrollChangeListener;

    public OnScrollChangeListenerAdapter(ListenScrollChangesHelper.OnScrollChangeListenerCompat onScrollChangeListener) {
        mOnScrollChangeListener = onScrollChangeListener;
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        mOnScrollChangeListener.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY);
    }
}
