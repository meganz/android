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

            switch (e.getErrorCode()) {
                case MegaError.API_OK:
                    Intent intent = null;

                    if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
                        intent = new Intent(openLinkActivity, ManagerActivityLollipop.class);
                        intent.setAction(ACTION_CANCEL_ACCOUNT);
                    } else if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        intent = new Intent(openLinkActivity, LoginActivityLollipop.class);
                        intent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
                        if (request.getFlag()) {
                            intent.setAction(ACTION_RESET_PASS);
                        } else {
                            intent.setAction(ACTION_PARK_ACCOUNT);
                        }
                    } else if (matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
                        intent = new Intent(openLinkActivity, ManagerActivityLollipop.class);
                        intent.setAction(ACTION_CHANGE_MAIL);
                    }

                    if (intent != null) {
                        intent.setData(Uri.parse(url));
                        openLinkActivity.startActivity(intent);
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
                    if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)
                            || matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)
                            || matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
                        logWarning("Error opening link not related to this account: " + e.getErrorString() + "___" + e.getErrorCode());
                        openLinkActivity.setError(openLinkActivity.getString(R.string.error_not_logged_with_correct_account));
                    }
                    break;

                default:
                    openLinkActivity.setError(openLinkActivity.getString(R.string.invalid_link));

            }
        }
    }
}
