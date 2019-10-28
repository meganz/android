package mega.privacy.android.app;

import android.app.AlertDialog;
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
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class WeakAccountProtectionAlertActivity extends PinActivityLollipop implements View.OnClickListener {

    private static final String IS_INFO_DIALOG_SHOWN = "IS_INFO_DIALOG_SHOWN";

    private MegaApiAndroid megaApi;

    private ScrollView scrollContentLayout;
    private TextView verifyEmailText;
    private RelativeLayout whyAmISeeingThisLayout;
    private Button resendEmailButton;

    private AlertDialog infoDialog;
    private boolean isInfoDialogShown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        megaApi = MegaApplication.getInstance().getMegaApi();

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

        if (savedInstanceState != null) {
            isInfoDialogShown = savedInstanceState.getBoolean(IS_INFO_DIALOG_SHOWN, false);
            if (isInfoDialogShown) {
                showInfoDialog();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_INFO_DIALOG_SHOWN, isInfoDialogShown);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        megaApi.whyAmIBlocked(new WhyAmIBlockedListener());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.why_am_i_seeing_this_layout:
                showInfoDialog();
                break;
            case R.id.resend_email_button:
                megaApi.resendVerificationEmail(new ResendVerificationEmailListener());
                break;
            case R.id.ok_button:
                isInfoDialogShown = false;
                try {
                    infoDialog.dismiss();
                } catch (Exception e) {
                    logWarning("Exception dismissing infoDialog");
                }
                break;
        }
    }

    private void showInfoDialog() {
        if (infoDialog != null && infoDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_enable_2fa_create_account, null);
        builder.setView(v);

        Button okButton = v.findViewById(R.id.ok_button);
        okButton.setOnClickListener(this);

        infoDialog = builder.create();
        infoDialog.setCanceledOnTouchOutside(false);
        infoDialog.show();
        isInfoDialogShown = true;
    }

    public void whyAmIBlockedResult(String result) {
        if (!result.equals(WEAK_PROTECTION_ACCOUNT_BLOCK)) {
            finish();
        }
    }

    public void showSnackbar(int stringResource) {
        showSnackbar(scrollContentLayout, getString(stringResource));
    }
}
