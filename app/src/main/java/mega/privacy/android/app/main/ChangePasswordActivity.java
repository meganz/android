package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.IntentConstants.EXTRA_MASTER_KEY;
import static mega.privacy.android.app.utils.Constants.ACTION_PASS_CHANGED;
import static mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS_FROM_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT;
import static mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.RESULT;
import static mega.privacy.android.app.utils.Constants.URL_E2EE;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL;
import static mega.privacy.android.app.utils.Util.changeActionBarElevation;
import static mega.privacy.android.app.utils.Util.getScaleH;
import static mega.privacy.android.app.utils.Util.getScaleW;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.showAlert;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.CoroutineScope;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.domain.qualifier.ApplicationScope;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class ChangePasswordActivity extends PasscodeActivity implements OnClickListener, MegaRequestListenerInterface {

    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;

    public static final String KEY_IS_LOGOUT = "logout";
    private static final float DISABLED_BUTTON_ALPHA = 0.5F;
    private static final float ENABLED_BUTTON_ALPHA = 1F;
    private AlertDialog progress;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    private MegaApiAndroid megaApi;

    boolean changePassword = true;

    private TextInputLayout newPassword1Layout;
    private AppCompatEditText newPassword1;
    private ImageView newPassword1Error;
    private TextInputLayout newPassword2Layout;
    private AppCompatEditText newPassword2;
    private ImageView newPassword2Error;
    private Button changePasswordButton;
    private LinearLayout generalContainer;
    private String linkToReset;
    private String mk;

    // TOP for 'terms of password'
    private CheckBox chkTOP;

    private LinearLayout containerPasswdElements;
    private ImageView firstShape;
    private ImageView secondShape;
    private ImageView tirdShape;
    private ImageView fourthShape;
    private ImageView fifthShape;
    private TextView passwdType;
    private TextView passwdAdvice;
    private boolean passwdValid;

    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        generalContainer = findViewById(R.id.change_password_container);
        megaApi = ((MegaApplication) getApplication()).getMegaApi();

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        passwdValid = false;

        containerPasswdElements = findViewById(R.id.container_passwd_elements);
        containerPasswdElements.setVisibility(View.GONE);
        firstShape = findViewById(R.id.shape_passwd_first);
        secondShape = findViewById(R.id.shape_passwd_second);
        tirdShape = findViewById(R.id.shape_passwd_third);
        fourthShape = findViewById(R.id.shape_passwd_fourth);
        fifthShape = findViewById(R.id.shape_passwd_fifth);
        passwdType = findViewById(R.id.password_type);
        passwdAdvice = findViewById(R.id.password_advice_text);

        newPassword1Layout = findViewById(R.id.change_password_newPassword1_layout);
        newPassword1Layout.setEndIconVisible(false);
        newPassword1 = findViewById(R.id.change_password_newPassword1);
        newPassword1.setOnFocusChangeListener((v1, hasFocus) -> {
            newPassword1Layout.setEndIconVisible(hasFocus);

            if (!hasFocus) {
                checkFirstPasswordField();
            }
        });
        newPassword1Error = findViewById(R.id.change_password_newPassword1_error_icon);
        newPassword1Error.setVisibility(View.GONE);

        newPassword1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Timber.d("Text changed: %s_ %d__%d__%d", s.toString(), start, before, count);
                if (s != null) {
                    if (s.length() > 0) {
                        String temp = s.toString();
                        containerPasswdElements.setVisibility(View.VISIBLE);

                        checkPasswordStrength(temp.trim(), false);
                    } else {
                        passwdValid = false;
                        containerPasswdElements.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String normalHint = StringResourcesUtils.getString(R.string.my_account_change_password_newPassword1);

                if (newPassword1Layout.getHint() != null
                        && !newPassword1Layout.getHint().toString().equals(normalHint)) {
                    newPassword1Layout.setHint(normalHint);
                    changePasswordButton.setEnabled(true);
                    changePasswordButton.setAlpha(ENABLED_BUTTON_ALPHA);
                }

                if (editable.toString().isEmpty()) {
                    quitError(newPassword1);
                }

                if (savedInstanceState != null && !newPassword1.hasFocus()) {
                    checkFirstPasswordField();
                }
            }
        });

        newPassword2Layout = findViewById(R.id.change_password_newPassword2_layout);
        newPassword2Layout.setEndIconVisible(false);
        newPassword2 = findViewById(R.id.change_password_newPassword2);
        newPassword2.setOnFocusChangeListener((v1, hasFocus) ->
                newPassword2Layout.setEndIconVisible(hasFocus));
        newPassword2Error = findViewById(R.id.change_password_newPassword2_error_icon);
        newPassword2Error.setVisibility(View.GONE);

        newPassword2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(newPassword2);
            }
        });

        changePasswordButton = findViewById(R.id.action_change_password);
        changePasswordButton.setOnClickListener(this);

        findViewById(R.id.action_cancel).setOnClickListener(v -> finish());

        TextView top = findViewById(R.id.top);

        String textToShowTOP = getString(R.string.top);
        try {
            textToShowTOP = textToShowTOP.replace("[B]", "<font color=\'"
                            + ColorUtils.getThemeColorHexString(this, R.attr.colorSecondary)
                            + "\'>")
                    .replace("[/B]", "</font>")
                    .replace("[A]", "<u>")
                    .replace("[/A]", "</u>");
        } catch (Exception e) {
            Timber.e(e, "Exception formatting string");
        }

        Spanned resultTOP;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultTOP = Html.fromHtml(textToShowTOP, Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultTOP = Html.fromHtml(textToShowTOP);
        }

        top.setText(resultTOP);

        top.setOnClickListener(this);

        chkTOP = findViewById(R.id.chk_top);
        chkTOP.setOnClickListener(this);

        progress = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.my_account_changing_password));
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);

        Toolbar tB = findViewById(R.id.change_password_toolbar);
        setSupportActionBar(tB);
        ActionBar aB = getSupportActionBar();
        if (aB != null) {
            aB.setTitle(getString(R.string.my_account_change_password));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        } else {
            Timber.w("Action Bar is null");
        }

        var scrollView = findViewById(R.id.change_password_scroll_view);
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                changeActionBarElevation(this, findViewById(R.id.app_bar_layout_change_password), scrollView.canScrollVertically(-1)));

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        Intent intentReceived = getIntent();
        if (intentReceived != null) {
            Timber.d("There is an intent!");
            if (intentReceived.getAction() != null) {
                if (getIntent().getAction().equals(ACTION_RESET_PASS_FROM_LINK)) {
                    Timber.d("ACTION_RESET_PASS_FROM_LINK");
                    changePassword = false;
                    linkToReset = getIntent().getDataString();
                    if (linkToReset == null) {
                        Timber.w("link is NULL - close activity");
                        finish();
                    }
                    mk = getIntent().getStringExtra(EXTRA_MASTER_KEY);
                    if (mk == null) {
                        Timber.w("MK is NULL - close activity");
                        showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
                    }

                    if (aB != null) {
                        aB.setTitle(getString(R.string.title_enter_new_password));
                    }
                }
                if (getIntent().getAction().equals(ACTION_RESET_PASS_FROM_PARK_ACCOUNT)) {
                    changePassword = false;
                    Timber.d("ACTION_RESET_PASS_FROM_PARK_ACCOUNT");
                    linkToReset = getIntent().getDataString();
                    if (linkToReset == null) {
                        Timber.w("link is NULL - close activity");
                        showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
                    }
                    mk = null;

                    if (aB != null) {
                        aB.setTitle(getString(R.string.title_enter_new_password));
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");

        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        if (getIntent() != null && getIntent().getBooleanExtra(KEY_IS_LOGOUT, false)) {
            Intent intent = new Intent(this, TestPasswordActivity.class);
            intent.putExtra(KEY_IS_LOGOUT, true);
            startActivity(intent);
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        switch (v.getId()) {
            case R.id.action_change_password: {
                if (changePassword) {
                    Timber.d("Ok proceed to change");
                    onChangePasswordClick();
                } else {
                    Timber.d("Reset pass on click");
                    if (linkToReset == null) {
                        Timber.w("link is NULL");
                        showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
                    } else {
                        if (mk == null) {
                            Timber.d("Proceed to park account");
                            onResetPasswordClick(false);
                        } else {
                            Timber.d("Ok proceed to reset");
                            onResetPasswordClick(true);
                        }
                    }
                }
                break;
            }
            case R.id.lost_authentication_device: {
                try {
                    Intent openTermsIntent = new Intent(this, WebViewActivity.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(RECOVERY_URL));
                    startActivity(openTermsIntent);
                } catch (Exception e) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(RECOVERY_URL));
                    startActivity(viewIntent);
                }
                break;
            }
            case R.id.top:
                Timber.d("Show top");
                try {
                    Intent openTermsIntent = new Intent(this, WebViewActivity.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(openTermsIntent);
                } catch (Exception e) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(viewIntent);
                }

                break;
        }
    }

    public void checkPasswordStrength(String s, boolean isSamePassword) {
        newPassword1Layout.setErrorEnabled(false);

        if (isSamePassword
                || megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK
                || s.length() < 4) {
            firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_very_weak));
            secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_very_weak));
            passwdType.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = false;

            newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_VeryWeak);
            newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_VeryWeak);
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK) {
            firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
            secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
            tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_weak));
            passwdType.setTextColor(ContextCompat.getColor(this, R.color.yellow_600_yellow_300));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = true;

            newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Weak);
            newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Weak);
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM) {
            firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
            secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
            tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
            fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_medium));
            passwdType.setTextColor(ContextCompat.getColor(this, R.color.green_500_green_400));

            passwdAdvice.setText(getString(R.string.passwd_medium));

            passwdValid = true;

            newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium);
            newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium);
        } else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD) {
            firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
            secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
            tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
            fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
            fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_good));
            passwdType.setTextColor(ContextCompat.getColor(this, R.color.lime_green_500_200));

            passwdAdvice.setText(getString(R.string.passwd_good));

            passwdValid = true;

            newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Good);
            newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Good);
        } else {
            firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
            secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
            tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
            fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
            fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));

            passwdType.setText(getString(R.string.pass_strong));
            passwdType.setTextColor(ContextCompat.getColor(this, R.color.dark_blue_500_200));

            passwdAdvice.setText(getString(R.string.passwd_strong));

            passwdValid = true;

            newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Strong);
            newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Strong);
        }

        newPassword1Error.setVisibility(View.GONE);
        newPassword1Layout.setError(" ");
        newPassword1Layout.setErrorEnabled(true);
    }

    public void onResetPasswordClick(boolean hasMk) {
        Timber.d("hasMk: %s", hasMk);

        if (!isOnline(this)) {
            showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        if (!validateForm(false)) {
            return;
        }

        imm.hideSoftInputFromWindow(newPassword1.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(newPassword2.getWindowToken(), 0);

        final String newPass1 = newPassword1.getText().toString();

        progress.setMessage(getString(R.string.my_account_changing_password));
        progress.show();

        if (hasMk) {
            Timber.d("reset with mk");
            megaApi.confirmResetPassword(linkToReset, newPass1, mk, this);
        } else {
            megaApi.confirmResetPassword(linkToReset, newPass1, null, this);
        }
    }

    public void onChangePasswordClick() {
        Timber.d("onChangePasswordClick");
        if (!isOnline(this)) {
            showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        if (!validateForm(true)) {
            return;
        }

        imm.hideSoftInputFromWindow(newPassword1.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(newPassword2.getWindowToken(), 0);

        megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
    }

    /*
     * Validate old password and new passwords
     */
    private boolean validateForm(boolean withOldPass) {
        if (withOldPass) {
            String newPassword1Error = getNewPassword1Error();
            String newPassword2Error = getNewPassword2Error();

            setError(newPassword1, newPassword1Error);
            setError(newPassword2, newPassword2Error);

            if (newPassword1Error != null) {
                newPassword1.requestFocus();
                return false;
            } else if (newPassword2Error != null) {
                newPassword2.requestFocus();
                return false;
            }
        } else {
            String newPassword2Error = getNewPassword2Error();

            setError(newPassword2, newPassword2Error);

            if (checkFirstPasswordField()) {
                newPassword1.requestFocus();
                return false;
            } else if (newPassword2Error != null) {
                newPassword2.requestFocus();
                return false;
            }
        }
        if (!chkTOP.isChecked()) {
            showSnackbar(getString(R.string.create_account_no_top));
            return false;
        }
        return true;
    }

    /*
     * Validate new password1
     */
    private String getNewPassword1Error() {
        String value = newPassword1.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_password);
        } else if (megaApi.checkPassword(value)) {
            return StringResourcesUtils.getString(R.string.error_same_password);
        } else if (!passwdValid) {
            containerPasswdElements.setVisibility(View.GONE);
            return getString(R.string.error_password);
        }
        return null;
    }

    /*
     * Validate new password2
     */
    private String getNewPassword2Error() {
        String value = newPassword2.getText().toString();
        String confirm = newPassword1.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_password);
        } else if (!value.equals(confirm)) {
            return getString(R.string.error_passwords_dont_match);
        }
        return null;
    }

    private void changePassword(String newPassword) {
        Timber.d("changePassword");
        megaApi.changePassword(null, newPassword, this);
        progress.setMessage(getString(R.string.my_account_changing_password));
        progress.show();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart: %s", request.getName());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish");

        if (request.getType() == MegaRequest.TYPE_CHANGE_PW) {
            Timber.d("TYPE_CHANGE_PW");

            if (e.getErrorCode() != MegaError.API_OK) {
                Timber.w("e.getErrorCode = %d__ e.getErrorString = %s", e.getErrorCode(), e.getErrorString());
                try {
                    progress.dismiss();
                } catch (Exception ex) {
                    Timber.w(ex, "Exception dismissing progress dialog");
                }

                showSnackbar(getString(R.string.general_text_error));
            } else {
                Timber.d("Pass changed OK");
                try {
                    progress.dismiss();
                } catch (Exception ex) {
                    Timber.w(ex, "Exception dismissing progress dialog");
                }

                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                if (getIntent() != null && getIntent().getBooleanExtra("logout", false)) {
                    AccountController.logout(this, megaApi, sharingScope);
                } else {
                    //Intent to MyAccount
                    Intent resetPassIntent = new Intent(this, ManagerActivity.class);
                    resetPassIntent.setAction(ACTION_PASS_CHANGED);
                    resetPassIntent.putExtra(RESULT, e.getErrorCode());
                    startActivity(resetPassIntent);
                    finish();
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_CONFIRM_RECOVERY_LINK) {
            Timber.d("TYPE_CONFIRM_RECOVERY_LINK");

            try {
                progress.dismiss();
            } catch (Exception ex) {
                Timber.w(ex, "Exception dismissing progress dialog");
            }

            if (e.getErrorCode() != MegaError.API_OK) {
                Timber.w("e.getErrorCode = %d__ e.getErrorString = %s", e.getErrorCode(), e.getErrorString());
            } else {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }

            Intent resetPassIntent;

            if (megaApi.getRootNode() == null) {
                Timber.d("Not logged in");

                //Intent to Login
                resetPassIntent = new Intent(this, LoginActivity.class);
                resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            } else {
                Timber.d("Logged IN");

                resetPassIntent = new Intent(this, ManagerActivity.class);
            }

            resetPassIntent.setAction(ACTION_PASS_CHANGED);
            resetPassIntent.putExtra(RESULT, e.getErrorCode());
            startActivity(resetPassIntent);
            finish();
        } else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK) {
            if (e.getErrorCode() == MegaError.API_OK) {
                if (request.getFlag()) {
                    Intent intent = new Intent(this, VerifyTwoFactorActivity.class);
                    intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, CHANGE_PASSWORD_2FA);
                    intent.putExtra(VerifyTwoFactorActivity.KEY_NEW_PASSWORD, newPassword1.getText().toString());
                    intent.putExtra(KEY_IS_LOGOUT, getIntent() != null && getIntent().getBooleanExtra(KEY_IS_LOGOUT, false));

                    startActivity(intent);
                } else {
                    changePassword(newPassword1.getText().toString());
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void setError(final EditText editText, String error) {
        Timber.d("setError");
        if (error == null || error.equals("")) {
            return;
        }
        switch (editText.getId()) {

            case R.id.change_password_newPassword1: {
                String samePasswordError = StringResourcesUtils.getString(R.string.error_same_password);
                if (error.equals(samePasswordError)) {
                    checkPasswordStrength(editText.getText().toString(), true);
                    newPassword1Layout.setHint(samePasswordError);
                    newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error);
                    newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error);
                    changePasswordButton.setEnabled(false);
                    changePasswordButton.setAlpha(DISABLED_BUTTON_ALPHA);
                } else {
                    newPassword1Layout.setError(error);
                    newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error);
                    newPassword1Layout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error);
                    newPassword1Error.setVisibility(View.VISIBLE);
                }
                break;
            }
            case R.id.change_password_newPassword2: {
                newPassword2Layout.setError(error);
                newPassword2Layout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error);
                newPassword2Error.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void quitError(EditText editText) {
        switch (editText.getId()) {
            case R.id.change_password_newPassword1: {
                newPassword1Layout.setError(null);
                newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                newPassword1Error.setVisibility(View.GONE);
                break;
            }
            case R.id.change_password_newPassword2: {
                newPassword2Layout.setError(null);
                newPassword2Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                newPassword2Error.setVisibility(View.GONE);
                break;
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
                                        MegaError e) {
        Timber.w("onRequestTemporaryError: %s", request.getName());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDestroy() {
        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        super.onDestroy();
    }

    public void showSnackbar(String s) {
        showSnackbar(generalContainer, s);
    }

    private boolean checkFirstPasswordField() {
        String error = getNewPassword1Error();

        if (error != null) {
            setError(newPassword1, error);
            return true;
        }

        return false;
    }
}
