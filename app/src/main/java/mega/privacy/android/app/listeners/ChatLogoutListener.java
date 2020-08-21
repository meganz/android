package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;

import static mega.privacy.android.app.utils.Constants.ACCOUNT_BLOCKED_STRING;
import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.showErrorAlertDialog;

public class ChatLogoutListener extends ChatBaseListener{

    private String accountBlockedString;

    public ChatLogoutListener(Context context) {
        super(context);
    }

    /**
     * Constructor used when an account has been blocked and
     * a warning dialog has to be shown when the logout process has finished.
     *
     * @param context               current Context
     * @param accountBlockedString  text to show in the warning
     */
    public ChatLogoutListener(Context context, String accountBlockedString) {
        super(context);

        this.accountBlockedString = accountBlockedString;
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_LOGOUT) return;

        // Code for LoginFragment
        MegaApplication.getInstance().disableMegaChatApi();
        resetLoggerSDK();

        if (!isTextEmpty(accountBlockedString)) {
            if (context instanceof LoginActivityLollipop) {
                showErrorAlertDialog(accountBlockedString, false, (LoginActivityLollipop) context);
            } else {
                MegaApplication app = MegaApplication.getInstance();
                Intent loginIntent = new Intent(app.getApplicationContext(), LoginActivityLollipop.class)
                        .setAction(ACTION_SHOW_WARNING_ACCOUNT_BLOCKED)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                        .putExtra(ACCOUNT_BLOCKED_STRING, accountBlockedString);
                app.startActivity(loginIntent);
            }
        }
    }
}
