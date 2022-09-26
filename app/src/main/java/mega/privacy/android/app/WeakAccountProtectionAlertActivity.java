package mega.privacy.android.app;

import static mega.privacy.android.app.utils.Constants.ACTION_REFRESH_AFTER_BLOCKED;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.WEAK_PROTECTION_ACCOUNT_BLOCK;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.CoroutineScope;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.domain.qualifier.ApplicationScope;
import mega.privacy.android.app.listeners.ResendVerificationEmailListener;
import mega.privacy.android.app.listeners.WhyAmIBlockedListener;
import mega.privacy.android.app.main.LoginActivity;
import mega.privacy.android.app.main.controllers.AccountController;
import timber.log.Timber;

@AndroidEntryPoint
public class WeakAccountProtectionAlertActivity extends PasscodeActivity implements View.OnClickListener {

    private static final String IS_INFO_DIALOG_SHOWN = "IS_INFO_DIALOG_SHOWN";
    private static final String IS_ACCOUNT_BLOCKED = "IS_ACCOUNT_BLOCKED";

    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;

    private ScrollView scrollContentLayout;
    private TextView verifyEmailText;
    private RelativeLayout whyAmISeeingThisLayout;
    private Button resendEmailButton;
    private Button logoutButton;

    private AlertDialog infoDialog;
    private boolean isInfoDialogShown;
    private boolean isAccountBlocked = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MegaApplication.setBlockedDueToWeakAccount(true);

        setContentView(R.layout.activity_weak_account_protection_alert);

        scrollContentLayout = findViewById(R.id.scroll_content_layout);

        verifyEmailText = findViewById(R.id.verify_email_text);
        String text = String.format(getString(R.string.verify_email_and_follow_steps));
        try {
            text = text.replace("[A]", "<b>");
            text = text.replace("[/A]", "</b>");
        } catch (Exception e) {
        }

        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(text);
        }
        verifyEmailText.setText(result);

        whyAmISeeingThisLayout = findViewById(R.id.why_am_i_seeing_this_layout);
        whyAmISeeingThisLayout.setOnClickListener(this);

        resendEmailButton = findViewById(R.id.resend_email_button);
        resendEmailButton.setOnClickListener(this);

        logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            isInfoDialogShown = savedInstanceState.getBoolean(IS_INFO_DIALOG_SHOWN, false);
            isAccountBlocked = savedInstanceState.getBoolean(IS_ACCOUNT_BLOCKED, true);
            if (isInfoDialogShown) {
                showInfoDialog();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_INFO_DIALOG_SHOWN, isInfoDialogShown);
        outState.putBoolean(IS_ACCOUNT_BLOCKED, isAccountBlocked);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isAccountBlocked) {
            megaApi.whyAmIBlocked(new WhyAmIBlockedListener(this));
        }
    }

    @Override
    public void onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
//        Do nothing: do not permit to skip the warning, account blocked
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MegaApplication.setBlockedDueToWeakAccount(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.why_am_i_seeing_this_layout:
                showInfoDialog();
                break;

            case R.id.resend_email_button:
                megaApi.resendVerificationEmail(new ResendVerificationEmailListener(this));
                break;

            case R.id.ok_button:
                isInfoDialogShown = false;
                try {
                    infoDialog.dismiss();
                } catch (Exception e) {
                    Timber.w("Exception dismissing infoDialog");
                }
                break;

            case R.id.logout_button:
                AccountController.logout(this, megaApi, sharingScope);
                break;
        }
    }

    /**
     * Creates and shows an info dialog with possible causes why
     * the user is blocked
     */
    private void showInfoDialog() {
        if (infoDialog != null && infoDialog.isShowing()) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_locked_accounts, null);
        builder.setView(v);

        Button okButton = v.findViewById(R.id.ok_button);
        okButton.setOnClickListener(this);

        infoDialog = builder.create();
        infoDialog.setCanceledOnTouchOutside(false);
        infoDialog.show();
        isInfoDialogShown = true;
    }

    /**
     * Manages the result of the request whyAmIBlocked().
     * If the result is due to weak account protection (700), it does nothing.
     * If not, it starts a new complete login and hide the alert.
     *
     * @param result the reason code of why I am blocked
     */
    public void whyAmIBlockedResult(String result) {
        if (!result.equals(WEAK_PROTECTION_ACCOUNT_BLOCK) && isAccountBlocked) {
            isAccountBlocked = false;
            if (megaApi.getRootNode() == null) {
                Intent intentLogin = new Intent(this, LoginActivity.class);
                intentLogin.setAction(ACTION_REFRESH_AFTER_BLOCKED);
                intentLogin.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentLogin);
            }

            finish();
        }
    }

    public void showSnackbar(int stringResource) {
        showSnackbar(scrollContentLayout, getString(stringResource));
    }
}
