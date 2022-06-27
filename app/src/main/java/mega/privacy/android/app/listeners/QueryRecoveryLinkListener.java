package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.utils.Constants.ACTION_PARK_ACCOUNT;
import static mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS;
import static mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.TOUR_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.Util.matchRegexs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.LoginActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import timber.log.Timber;

public class QueryRecoveryLinkListener extends BaseListener {

    OpenLinkActivity openLinkActivity;

    public QueryRecoveryLinkListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_QUERY_RECOVERY_LINK) return;
        Timber.d("TYPE_GET_RECOVERY_LINK");

        if (context instanceof OpenLinkActivity) {
            openLinkActivity = (OpenLinkActivity) context;
        }

        String url = request.getLink();

        if (openLinkActivity != null) {
            if (url == null) {
                Timber.w("Error opening link URL null: %s___%d", e.getErrorString(), e.getErrorCode());
                openLinkActivity.setError(openLinkActivity.getString(R.string.general_text_error));
            }

            switch (e.getErrorCode()) {
                case MegaError.API_OK:
                    Intent intent = null;

                    if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        intent = new Intent(openLinkActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
                        if (request.getFlag()) {
                            intent.setAction(ACTION_RESET_PASS);
                        } else {
                            intent.setAction(ACTION_PARK_ACCOUNT);
                        }
                    }

                    if (intent != null) {
                        intent.setData(Uri.parse(url));
                        openLinkActivity.startActivity(intent);
                        openLinkActivity.finish();
                    }
                    break;

                case MegaError.API_EEXPIRED:
                    if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        openLinkActivity.setError(openLinkActivity.getString(R.string.recovery_link_expired));
                    }
                    break;

                case MegaError.API_EACCESS:
                    if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
                        Timber.w("Error opening link not related to this account: %s___%d", e.getErrorString(), e.getErrorCode());
                        openLinkActivity.setError(openLinkActivity.getString(R.string.error_not_logged_with_correct_account));
                    }
                    break;

                default:
                    openLinkActivity.setError(openLinkActivity.getString(R.string.invalid_link));

            }
        }
    }
}
