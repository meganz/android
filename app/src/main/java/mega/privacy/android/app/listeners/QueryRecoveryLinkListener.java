package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class QueryRecoveryLinkListener extends BaseListener {

    OpenLinkActivity openLinkActivity;

    public QueryRecoveryLinkListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_QUERY_RECOVERY_LINK) return;
        logDebug("TYPE_GET_RECOVERY_LINK");

        if (context instanceof OpenLinkActivity) {
            openLinkActivity = (OpenLinkActivity) context;
        }

        String url = request.getLink();

        if (openLinkActivity != null) {
            if (url == null) {
                logWarning("Error opening link URL null: " + e.getErrorString() + "___" + e.getErrorCode());
                openLinkActivity.setError(openLinkActivity.getString(R.string.general_text_error));
            }

            String myEmail = request.getEmail();
            if (myEmail != null && !myEmail.equals(api.getMyEmail())){
                logWarning("Error opening link not related to this account: " + e.getErrorString() + "___" + e.getErrorCode());
                openLinkActivity.setError(openLinkActivity.getString(R.string.error_not_logged_with_correct_account));
            }

            switch (e.getErrorCode()) {
                case MegaError.API_OK:
                    if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
                        Intent cancelAccountIntent = new Intent(openLinkActivity, ManagerActivityLollipop.class);
                        cancelAccountIntent.setAction(ACTION_CANCEL_ACCOUNT);
                        cancelAccountIntent.setData(Uri.parse(url));
                        openLinkActivity.startActivity(cancelAccountIntent);
                        openLinkActivity.finish();
                    } else if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        boolean mk = request.getFlag();
                        if (mk) {
                            Intent resetPassIntent = new Intent(openLinkActivity, LoginActivityLollipop.class);
                            resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                            resetPassIntent.setAction(ACTION_RESET_PASS);
                            resetPassIntent.setData(Uri.parse(url));
                            openLinkActivity.startActivity(resetPassIntent);
                            openLinkActivity.finish();
                        } else {
                            Intent resetPassIntent = new Intent(openLinkActivity, LoginActivityLollipop.class);
                            resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                            resetPassIntent.setAction(ACTION_PARK_ACCOUNT);
                            resetPassIntent.setData(Uri.parse(url));
                            openLinkActivity.startActivity(resetPassIntent);
                            openLinkActivity.finish();
                        }
                    } else if (matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
                        Intent changeMailIntent = new Intent(openLinkActivity, ManagerActivityLollipop.class);
                        changeMailIntent.setAction(ACTION_CHANGE_MAIL);
                        changeMailIntent.setData(Uri.parse(url));
                        openLinkActivity.startActivity(changeMailIntent);
                        openLinkActivity.finish();
                    }
                    break;

                case MegaError.API_EEXPIRED:
                    if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
                        openLinkActivity.setError(openLinkActivity.getString(R.string.cancel_link_expired));
                    } else if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        openLinkActivity.setError(openLinkActivity.getString(R.string.recovery_link_expired));
                    }
                    break;

                case MegaError.API_EACCESS:
                    if (matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
                        openLinkActivity.setError(openLinkActivity.getString(R.string.error_not_logged_with_correct_account));
                    }
                    break;

                default:
                    openLinkActivity.setError(openLinkActivity.getString(R.string.invalid_link));

            }
        }
    }
}
