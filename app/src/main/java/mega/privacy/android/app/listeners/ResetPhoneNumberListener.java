package mega.privacy.android.app.listeners;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import mega.privacy.android.app.modalbottomsheet.phoneNumber.PhoneNumberCallback;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

/**
 * Listener for reset phone number.
 *
 * @see nz.mega.sdk.MegaApiJava#resetSmsVerifiedPhoneNumber
 */
public class ResetPhoneNumberListener extends BaseListener {

    private PhoneNumberCallback callback;

    public ResetPhoneNumberListener(Context context, PhoneNumberCallback callback) {
        super(context);
        this.callback = callback;
    }

    @Override
    public void onRequestFinish(@NotNull MegaApiJava api, MegaRequest request, @NotNull MegaError e) {
        if (request.getType() != MegaRequest.TYPE_RESET_SMS_VERIFIED_NUMBER) return;

        if (callback != null) {
            callback.onReset(e);
        }
    }
}
