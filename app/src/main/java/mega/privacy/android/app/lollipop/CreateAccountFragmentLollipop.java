package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.EphemeralCredentials;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.OnKeyboardVisibilityListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class CreateAccountFragmentLollipop extends Fragment implements View.OnClickListener, MegaRequestListenerInterface, OnKeyboardVisibilityListener {

    private Context context;

    private Button bRegister;
    private Button bLogin;
    private TextInputLayout userNameLayout;
    private AppCompatEditText userName;
    private ImageView userNameError;
    private TextInputLayout userLastNameLayout;
    private AppCompatEditText userLastName;
    private ImageView userLastNameError;
    private TextInputLayout userEmailLayout;
    private AppCompatEditText userEmail;
    private ImageView userEmailError;
    private TextInputLayout userPasswordLayout;
    private AppCompatEditText userPassword;
    private ImageView userPasswordError;
    private TextInputLayout userPasswordConfirmLayout;
    private AppCompatEditText userPasswordConfirm;
    private ImageView userPasswordConfirmError;
    private RelativeLayout createAccountAndAcceptLayout;

    private CheckBox chkTOS;
    //TOP for 'terms of password'
    private CheckBox chkTOP;

    private MegaApiAndroid megaApi;

    private LinearLayout createAccountLayout;
    private LinearLayout creatingAccountLayout;

    private TextView creatingAccountTextView;
    private ProgressBar createAccountProgressBar;

    private ImageView toggleButtonPasswd;
    private ImageView toggleButtonConfirmPasswd;
    private boolean passwdVisibility;
    private LinearLayout containerPasswdElements;
    private ImageView firstShape;
    private ImageView secondShape;
    private ImageView tirdShape;
    private ImageView fourthShape;
    private ImageView fifthShape;
    private TextView passwdType;
    private TextView passwdAdvice;
    private boolean passwdValid;

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            logWarning("context is null");
            return;
        }

        ((LoginActivityLollipop) context).setKeyboardVisibilityListener(this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        View v = inflater.inflate(R.layout.fragment_create_account, container, false);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();

        createAccountLayout = v.findViewById(R.id.create_account_create_layout);
        createAccountAndAcceptLayout = v.findViewById(R.id.create_account_and_accept_layout);

        userNameLayout = v.findViewById(R.id.create_account_name_text_layout);
        userName = v.findViewById(R.id.create_account_name_text);
        userNameError = v.findViewById(R.id.create_account_name_text_error_icon);
        userNameError.setVisibility(View.GONE);
        userLastNameLayout = v.findViewById(R.id.create_account_last_name_text_layout);
        userLastName = v.findViewById(R.id.create_account_last_name_text);
        userLastNameError = v.findViewById(R.id.create_account_last_name_text_error_icon);
        userLastNameError.setVisibility(View.GONE);
        userEmailLayout = v.findViewById(R.id.create_account_email_text_layout);
        userEmail = v.findViewById(R.id.create_account_email_text);
        userEmailError = v.findViewById(R.id.create_account_email_text_error_icon);
        userEmailError.setVisibility(View.GONE);
        userPasswordLayout = v.findViewById(R.id.create_account_password_text_layout);
        userPassword = v.findViewById(R.id.create_account_password_text);
        userPasswordError = v.findViewById(R.id.create_account_password_text_error_icon);
        userPasswordError.setVisibility(View.GONE);
        userPasswordConfirmLayout = v.findViewById(R.id.create_account_password_text_confirm_layout);
        userPasswordConfirm = v.findViewById(R.id.create_account_password_text_confirm);
        userPasswordConfirmError = v.findViewById(R.id.create_account_password_text_confirm_error_icon);
        userPasswordConfirmError.setVisibility(View.GONE);

        toggleButtonPasswd = v.findViewById(R.id.toggle_button_passwd);
        toggleButtonPasswd.setOnClickListener(this);
        toggleButtonConfirmPasswd = v.findViewById(R.id.toggle_button_confirm_passwd);
        toggleButtonConfirmPasswd.setOnClickListener(this);
        passwdVisibility = false;
        passwdValid = false;

        userName.requestFocus();
        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userName);
            }
        });

        userLastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userLastName);
            }
        });

        userEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userEmail);
            }
        });

        userPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logDebug("Text changed: " + s.toString() + "_ " + start + "__" + before + "__" + count);
                if (s != null){
                    if (s.length() > 0) {
                        String temp = s.toString();
                        containerPasswdElements.setVisibility(View.VISIBLE);

                        checkPasswordStrenght(temp.trim());
                    }
                    else{
                        passwdValid = false;
                        containerPasswdElements.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().isEmpty()) {
                    quitError(userPassword);
                }
            }
        });

        userPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    toggleButtonPasswd.setVisibility(View.VISIBLE);
                    toggleButtonPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
                }
                else {
                    toggleButtonPasswd.setVisibility(View.GONE);
                    passwdVisibility = false;
                    showHidePassword(false);
                }
            }
        });

        userPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userPasswordConfirm);
            }
        });

        userPasswordConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    toggleButtonConfirmPasswd.setVisibility(View.VISIBLE);
                    toggleButtonConfirmPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
                }
                else {
                    toggleButtonConfirmPasswd.setVisibility(View.GONE);
                    passwdVisibility = false;
                    showHidePassword(true);
                }
            }
        });

        TextView tos = (TextView)v.findViewById(R.id.tos);

        String textToShow = context.getString(R.string.tos);
        try{
            textToShow = textToShow.replace("[A]", "<u>");
            textToShow = textToShow.replace("[/A]", "</u>");
        }
        catch (Exception e){}

        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }

        tos.setText(result);

        tos.setOnClickListener(this);

        TextView top = v.findViewById(R.id.top);

        String textToShowTOP = context.getString(R.string.top);
        try {
            textToShowTOP = textToShowTOP.replace("[B]", "<font color=\'#00BFA5\'>")
                    .replace("[/B]", "</font>")
                    .replace("[A]", "<u>")
                    .replace("[/A]", "</u>");
        } catch (Exception e) {
            logError("Exception formatting string", e);
        }

        Spanned resultTOP;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultTOP = Html.fromHtml(textToShowTOP,Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultTOP = Html.fromHtml(textToShowTOP);
        }

        top.setText(resultTOP);
        top.setOnClickListener(this);

        chkTOS = (CheckBox) v.findViewById(R.id.create_account_chkTOS);
        chkTOS.setOnClickListener(this);

        chkTOP = v.findViewById(R.id.chk_top);
        chkTOP.setOnClickListener(this);

        bRegister = (Button) v.findViewById(R.id.button_create_account_create);
        bRegister.setText(getString(R.string.create_account));
        bRegister.setOnClickListener(this);

        bLogin = (Button) v.findViewById(R.id.button_login_create);
        bLogin.setOnClickListener(this);

        bLogin.setText(getString(R.string.login_text));

        creatingAccountLayout = (LinearLayout) v.findViewById(R.id.create_account_creating_layout);
        creatingAccountTextView = (TextView) v.findViewById(R.id.create_account_creating_text);
        createAccountProgressBar = (ProgressBar) v.findViewById(R.id.create_account_progress_bar);

        createAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountLayout.setVisibility(View.GONE);
        creatingAccountTextView.setVisibility(View.GONE);
        createAccountProgressBar.setVisibility(View.GONE);

        containerPasswdElements = (LinearLayout) v.findViewById(R.id.container_passwd_elements);
        containerPasswdElements.setVisibility(View.GONE);
        firstShape = (ImageView) v.findViewById(R.id.shape_passwd_first);
        secondShape = (ImageView) v.findViewById(R.id.shape_passwd_second);
        tirdShape = (ImageView) v.findViewById(R.id.shape_passwd_third);
        fourthShape = (ImageView) v.findViewById(R.id.shape_passwd_fourth);
        fifthShape = (ImageView) v.findViewById(R.id.shape_passwd_fifth);
        passwdType = (TextView) v.findViewById(R.id.password_type);
        passwdAdvice = (TextView) v.findViewById(R.id.password_advice_text);

        return v;
    }

    public void checkPasswordStrenght(String s) {

        if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK || s.length() < 4){
            firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_very_weak));
            secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_very_weak));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.login_warning));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = false;

            userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceVeryWeak);
            userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceVeryWeak);
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK){
            firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
            secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
            tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_weak));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.pass_weak));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = true;

            userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceWeak);
            userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceWeak);
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM){
            firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
            secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
            tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
            fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_medium));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.green_unlocked_rewards));

            passwdAdvice.setText(getString(R.string.passwd_medium));

            passwdValid = true;

            userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceMedium);
            userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceMedium);
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD){
            firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
            secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
            tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
            fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
            fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));

            passwdType.setText(getString(R.string.pass_good));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.pass_good));

            passwdAdvice.setText(getString(R.string.passwd_good));

            passwdValid = true;

            userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceGood);
            userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceGood);
        }
        else {
            firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));

            passwdType.setText(getString(R.string.pass_strong));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.blue_unlocked_rewards));

            passwdAdvice.setText(getString(R.string.passwd_strong));

            passwdValid = true;

            userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceStrong);
            userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceStrong);
        }

        userPasswordError.setVisibility(View.GONE);
        userPasswordLayout.setError(" ");
    }

    public void showHidePassword (boolean confirm) {
        if(!passwdVisibility){
            if (!confirm){
                userPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                userPassword.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                userPassword.setSelection(userPassword.getText().length());
            }
            else {
                userPasswordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                userPasswordConfirm.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                userPasswordConfirm.setSelection(userPasswordConfirm.getText().length());
            }
        }else{
            if (!confirm){
                userPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                userPassword.setSelection(userPassword.getText().length());
            }
            else {
                userPasswordConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                userPasswordConfirm.setSelection(userPasswordConfirm.getText().length());
            }
        }
    }

    void hidePasswordIfVisible () {
        if (passwdVisibility) {
            passwdVisibility = false;
            toggleButtonPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
            showHidePassword(false);
            toggleButtonConfirmPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
            showHidePassword(true);
        }
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch (v.getId()) {
            case R.id.create_account_chkTOS:
                hidePasswordIfVisible();
                break;

            case R.id.button_create_account_create:
                hidePasswordIfVisible();
                submitForm();
                break;

            case R.id.button_login_create:
                hidePasswordIfVisible();
                ((LoginActivityLollipop) context).showFragment(LOGIN_FRAGMENT);
                break;

            case R.id.tos:
                logDebug("Show ToS");
//				Intent browserIntent = new Intent(Intent.ACTION_VIEW);
//				browserIntent.setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
//				browserIntent.setDataAndType(Uri.parse("http://www.google.es"), "text/html");
//				browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
//				startActivity(browserIntent);
                hidePasswordIfVisible();
                try {
                    String url = "https://mega.nz/terms";
                    Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(url));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse("https://mega.nz/terms"));
                    startActivity(viewIntent);
                }

                break;
            case R.id.top:
                logDebug("Show terms of password");
                hidePasswordIfVisible();
                try {
                    Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(viewIntent);
                }

                break;
            case R.id.toggle_button_passwd:
                if (passwdVisibility) {
                    toggleButtonPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
                    passwdVisibility = false;
                    showHidePassword(false);
                }
                else {
                    toggleButtonPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_see));
                    passwdVisibility = true;
                    showHidePassword(false);
                }
                break;

            case R.id.toggle_button_confirm_passwd:
                if (passwdVisibility) {
                    toggleButtonConfirmPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_shared_read));
                    passwdVisibility = false;
                    showHidePassword(true);
                }
                else {
                    toggleButtonConfirmPasswd.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_see));
                    passwdVisibility = true;
                    showHidePassword(true);
                }
                break;
        }
    }

    /*
	 * Registration form submit
	 */
    private void submitForm() {
        logDebug("submit form!");

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        dbH.clearCredentials();

        if (!validateForm()) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(userEmail.getWindowToken(), 0);

        if(!isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
        createAccountAndAcceptLayout.setVisibility(View.GONE);

        megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), userLastName.getText().toString(),this);
    }

    private boolean validateForm() {
        String emailError = getEmailError();
        String passwordError = getPasswordError();
        String usernameError = getUsernameError();
        String userLastnameError = getUserLastnameError();
        String passwordConfirmError = getPasswordConfirmError();

        // Set or remove errors
        setError(userName, usernameError);
        setError(userLastName, userLastnameError);
        setError(userEmail, emailError);
        setError(userPassword, passwordError);
        setError(userPasswordConfirm, passwordConfirmError);

        // Return false on any error or true on success
        if (usernameError != null) {
            userName.requestFocus();
            return false;
        } else if(userLastnameError != null){
            userLastName.requestFocus();
            return false;
        }else if (emailError != null) {
            userEmail.requestFocus();
            return false;
        } else if (passwordError != null) {
            userPassword.requestFocus();
            return false;
        } else if (passwordConfirmError != null) {
            userPasswordConfirm.requestFocus();
            return false;
        } else if (!chkTOS.isChecked()) {
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.create_account_no_terms));
            return false;
        } else if (!chkTOP.isChecked()) {
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.create_account_no_top));
            return false;
        }
        return true;
    }

    private String getEmailError() {
        String value = userEmail.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_email);
        }
        if (!EMAIL_ADDRESS.matcher(value).matches()) {
            return getString(R.string.error_invalid_email);
        }
        return null;
    }

    private String getUsernameError() {
        String value = userName.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_username);
        }
        return null;
    }

    private String getUserLastnameError() {
        String value = userLastName.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_userlastname);
        }
        return null;
    }

    private String getPasswordError() {
        String value = userPassword.getText().toString();
        if (value.isEmpty()) {
            return getString(R.string.error_enter_password);
        }
        else if (!passwdValid){
            containerPasswdElements.setVisibility(View.GONE);
            return getString(R.string.error_password);
        }
        return null;
    }

    private String getPasswordConfirmError() {
        String password = userPassword.getText().toString();
        String confirm = userPasswordConfirm.getText().toString();
        if (confirm.isEmpty()) {
            return getString(R.string.error_enter_password);
        } else if (password.equals(confirm) == false) {
            return getString(R.string.error_passwords_dont_match);
        }
        return null;
    }

    private void onKeysGenerated(final String privateKey, final String publicKey) {
        if(!isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
        megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), userLastName.getText().toString(),this);
//		megaApi.fastCreateAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), privateKey, userName.getText().toString().trim(), this);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart" + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request,
                                MegaError e) {
        logDebug("onRequestFinish");

        if (isAdded()) {
            if (e.getErrorCode() != MegaError.API_OK) {
                logWarning("ERROR CODE: " + e.getErrorCode() + "_ ERROR MESSAGE: " + e.getErrorString());

                if (e.getErrorCode() == MegaError.API_EEXIST) {
                    try {
                        ((LoginActivityLollipop) context).showSnackbar(getString(R.string.error_email_registered));
                        createAccountLayout.setVisibility(View.VISIBLE);
                        creatingAccountLayout.setVisibility(View.GONE);
                        creatingAccountTextView.setVisibility(View.GONE);
                        createAccountProgressBar.setVisibility(View.GONE);
                    }
                    catch(Exception ex){}
                    return;
                }
                else{
                    try {
                        String message = e.getErrorString();
                        ((LoginActivityLollipop) context).showSnackbar(message);
                        ((LoginActivityLollipop) context).showFragment(LOGIN_FRAGMENT);
                        createAccountLayout.setVisibility(View.VISIBLE);
                        creatingAccountLayout.setVisibility(View.GONE);
                        creatingAccountTextView.setVisibility(View.GONE);
                        createAccountProgressBar.setVisibility(View.GONE);
                    }
                    catch (Exception ex){}
                    return;
                }
            }
            else{
                ((LoginActivityLollipop)context).setEmailTemp(userEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim());
                ((LoginActivityLollipop)context).setFirstNameTemp(userName.getText().toString());
                ((LoginActivityLollipop)context).setLastNameTemp(userLastName.getText().toString());
                ((LoginActivityLollipop)context).setPasswdTemp(userPassword.getText().toString());
                ((LoginActivityLollipop)context).setWaitingForConfirmAccount(true);

                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                if (dbH != null){
                    dbH.clearEphemeral();

                    EphemeralCredentials ephemeral = new EphemeralCredentials(request.getEmail(), request.getPassword(), request.getSessionKey(), request.getName(), request.getText());

                    dbH.saveEphemeral(ephemeral);
                }

                ((LoginActivityLollipop)context).showFragment(CONFIRM_EMAIL_FRAGMENT);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
    }


    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttach(Context context) {
        logDebug("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        logDebug("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    private void setError(final EditText editText, String error){
        if(error == null || error.equals("")){
            return;
        }

        switch (editText.getId()){
            case R.id.create_account_email_text:{

                userEmailLayout.setError(error);
                userEmailLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userEmailError.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.create_account_password_text_confirm:{
                userPasswordConfirmLayout.setError(error);
                userPasswordConfirmLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userPasswordConfirmError.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.create_account_name_text:{
                userNameLayout.setError(error);
                userNameLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userNameError.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.create_account_last_name_text:{
                userLastNameLayout.setError(error);
                userLastNameLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userLastNameError.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.create_account_password_text:{
                userPasswordLayout.setError(error);
                userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userPasswordLayout.setErrorTextAppearance(R.style.InputTextAppearanceError);
                userPasswordError.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void quitError(EditText editText){
        switch (editText.getId()){
            case R.id.create_account_email_text:{
                userEmailLayout.setError(null);
                userEmailLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userEmailError.setVisibility(View.GONE);
                break;
            }
            case R.id.create_account_password_text_confirm:{
                userPasswordConfirmLayout.setError(null);
                userPasswordConfirmLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userPasswordConfirmError.setVisibility(View.GONE);
                break;
            }
            case R.id.create_account_name_text:{
                userNameLayout.setError(null);
                userNameLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userNameError.setVisibility(View.GONE);
                break;
            }
            case R.id.create_account_last_name_text:{
                userLastNameLayout.setError(null);
                userLastNameLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userLastNameError.setVisibility(View.GONE);
                break;
            }
            case R.id.create_account_password_text:{
                userPasswordLayout.setError(null);
                userPasswordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userPasswordError.setVisibility(View.GONE);
                break;
            }
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (!visible && createAccountLayout.getVisibility() == View.VISIBLE) {
            createAccountAndAcceptLayout.setVisibility(View.VISIBLE);
        } else {
            createAccountAndAcceptLayout.setVisibility(View.GONE);
        }
    }
}
