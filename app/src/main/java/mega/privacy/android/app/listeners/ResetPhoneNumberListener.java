package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

/**
 * Listener for reset phone number.
 *
 * @see nz.mega.sdk.MegaApiJava#resetSmsVerifiedPhoneNumber
 */
public class ResetPhoneNumberListener extends BaseListener {

    private OnResetPhoneNumberCallback callback;

    public ResetPhoneNumberListener(Context context, OnResetPhoneNumberCallback callback) {
        super(context);
        this.callback = callback;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_RESET_SMS_VERIFIED_NUMBER) return;
        if (callback != null) {
            callback.onResetPhoneNumber(e);
        }
    }

    public interface OnResetPhoneNumberCallback {

        /**
         * When try to reset an account's phone number, but the account doesn't have a verified phone number.
         * API returns error code API_ENOENT(-9) and error string "Not found".
         */
        String ERR_NOT_FOUND = "Not found";

        void onResetPhoneNumber(MegaError e);
    }
}
