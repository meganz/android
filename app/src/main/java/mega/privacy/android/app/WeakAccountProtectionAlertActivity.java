package mega.privacy.android.app;

import android.app.AlertDialog;
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

import mega.privacy.android.app.listeners.ResendVerificationEmailListener;
import mega.privacy.android.app.listeners.WhyAmIBlockedListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class WeakAccountProtectionAlertActivity extends PinActivityLollipop implements View.OnClickListener {

    private static final String IS_INFO_DIALOG_SHOWN = "IS_INFO_DIALOG_SHOWN";
    private static final String IS_ACCOUNT_BLOCKED = "IS_ACCOUNT_BLOCKED";

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

        app.setIsBlockedDueToWeakAccount(true);

        setContentView(R.layout.activity_weak_account_protection_alert);

        getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_red_alert));

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
//        Do nothing: do not permit to skip the warning, account blocked
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.setIsBlockedDueToWeakAccount(false);
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
                    logWarning("Exception dismissing infoDialog");
                }
                break;

            case R.id.logout_button:
                new AccountController(this).logout(this, megaApi);
                break;
        }
    }

    /**
     * Creates and shows an info dialog with possible causes why
     * the user is blocked
     */
    private void showInfoDialog() {
        if (infoDialog != null && infoDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
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
                Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
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
