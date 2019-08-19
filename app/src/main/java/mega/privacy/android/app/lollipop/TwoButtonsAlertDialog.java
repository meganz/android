package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public abstract class TwoButtonsAlertDialog extends AlertDialog {

    interface SetPasswordCallback {
        void onConfirmed(String password);
        void onCanceled();
    }

    protected Context mContext;
    protected Builder builder;
    protected SetPasswordCallback mCallback;

    protected TwoButtonsAlertDialog(@NonNull Context context, @NonNull SetPasswordCallback callback) {
        super(context);
        mContext = context;
        mCallback = callback;
        builder = new Builder(context);
    }

    public abstract void show();

    public abstract void dismiss();
}
