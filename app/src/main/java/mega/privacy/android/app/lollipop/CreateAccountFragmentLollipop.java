package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.EphemeralCredentials;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class CreateAccountFragmentLollipop extends Fragment implements View.OnClickListener, MegaRequestListenerInterface {

    private Context context;

    private Button bRegister;
    private Button bLogin;
    private TextView createAccountTitle;
    private TextView textAlreadyAccount;
    private EditText userName;
    private EditText userLastName;
    private EditText userEmail;
    private EditText userPassword;
    private EditText userPasswordConfirm;
    private ScrollView scrollView;

    private CheckBox chkTOS;
    private TextView tos;

    private MegaApiAndroid megaApi;

    private LinearLayout createAccountLayout;
    private LinearLayout creatingAccountLayout;

    private TextView creatingAccountTextView;
    private ProgressBar createAccountProgressBar;

    private RelativeLayout email_error_layout;
    private RelativeLayout password_confirm_error_layout;
    private RelativeLayout name_error_layout;
    private RelativeLayout last_name_error_layout;
    private RelativeLayout password_error_layout;
    private TextView email_error_text;
    private TextView password_confirm_error_text;
    private TextView name_error_text;
    private TextView last_name_error_text;
    private TextView password_error_text;

    private Drawable email_background;
    private Drawable password_confirm_background;
    private Drawable name_background;
    private Drawable lastname_background;
    private Drawable password_background;

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
        LogUtil.logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            LogUtil.logWarning("context is null");
            return;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtil.logDebug("onCreateView");

        View v = inflater.inflate(R.layout.fragment_create_account, container, false);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_account);
        createAccountLayout = (LinearLayout) v.findViewById(R.id.create_account_create_layout);
        createAccountTitle = (TextView) v.findViewById(R.id.create_account_text_view);

        userName = (EditText) v.findViewById(R.id.create_account_name_text);
        userLastName = (EditText) v.findViewById(R.id.create_account_last_name_text);
        userEmail = (EditText) v.findViewById(R.id.create_account_email_text);
        userPassword = (EditText) v.findViewById(R.id.create_account_password_text);
        userPasswordConfirm = (EditText) v.findViewById(R.id.create_account_password_text_confirm);

        toggleButtonPasswd  = (ImageView) v.findViewById(R.id.toggle_button_passwd);
        toggleButtonPasswd.setOnClickListener(this);
        toggleButtonConfirmPasswd = (ImageView) v.findViewById(R.id.toggle_button_confirm_passwd);
        toggleButtonConfirmPasswd.setOnClickListener(this);
        passwdVisibility = false;
        passwdValid = false;

        userName.getBackground().clearColorFilter();
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

        name_background = userName.getBackground().mutate().getConstantState().newDrawable();

        userLastName.getBackground().clearColorFilter();
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
        lastname_background = userLastName.getBackground().mutate().getConstantState().newDrawable();

        userEmail.getBackground().clearColorFilter();
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

        email_background = userEmail.getBackground().mutate().getConstantState().newDrawable();

        userPassword.getBackground().clearColorFilter();
        userPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                LogUtil.logDebug("Text changed: " + s.toString() + "_ " + start + "__" + before + "__" + count);
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
                quitError(userPassword);
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

        password_background = userPassword.getBackground().mutate().getConstantState().newDrawable();

        userPasswordConfirm.getBackground().clearColorFilter();
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

        password_confirm_background = userPasswordConfirm.getBackground().mutate().getConstantState().newDrawable();

        email_error_layout = (RelativeLayout) v.findViewById(R.id.create_account_email_error);
        email_error_layout.setVisibility(View.GONE);
        email_error_text = (TextView) v.findViewById(R.id.create_account_email_error_text);
        password_confirm_error_layout = (RelativeLayout) v.findViewById(R.id.create_account_password_confirm_error);
        password_confirm_error_layout.setVisibility(View.GONE);
        password_confirm_error_text = (TextView) v.findViewById(R.id.create_account_password_confirm_error_text);
        name_error_layout = (RelativeLayout) v.findViewById(R.id.create_account_name_error);
        name_error_layout.setVisibility(View.GONE);
        name_error_text = (TextView) v.findViewById(R.id.create_account_name_error_text);
        last_name_error_layout = (RelativeLayout) v.findViewById(R.id.create_account_last_name_error);
        last_name_error_layout.setVisibility(View.GONE);
        last_name_error_text = (TextView) v.findViewById(R.id.create_account_last_name_error_text);
        password_error_layout = (RelativeLayout) v.findViewById(R.id.create_account_password_error);
        password_error_layout.setVisibility(View.GONE);
        password_error_text = (TextView) v.findViewById(R.id.create_account_password_error_text);

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

        chkTOS = (CheckBox) v.findViewById(R.id.create_account_chkTOS);
        chkTOS.setOnClickListener(this);

        bRegister = (Button) v.findViewById(R.id.button_create_account_create);
        bRegister.setText(getString(R.string.create_account));
        bRegister.setOnClickListener(this);

        textAlreadyAccount = (TextView) v.findViewById(R.id.text_already_account);

        bLogin = (Button) v.findViewById(R.id.button_login_create);
        bLogin.setOnClickListener(this);

        bLogin.setText(getString(R.string.login_text));

        creatingAccountLayout = (LinearLayout) v.findViewById(R.id.create_account_creating_layout);
        creatingAccountTextView = (TextView) v.findViewById(R.id.create_account_creating_text);
        createAccountProgressBar = (ProgressBar) v.findViewById(R.id.create_account_progress_bar);

        createAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountLayout.setVisibility(View.GONE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
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
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                firstShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_very_weak));
                secondShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
            } else{
                firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_very_weak));
                secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            }

            passwdType.setText(getString(R.string.pass_very_weak));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.login_warning));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = false;
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK){
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                firstShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
                secondShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
                tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
            } else{
                firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
                secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_weak));
                tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            }

            passwdType.setText(getString(R.string.pass_weak));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.pass_weak));

            passwdAdvice.setText(getString(R.string.passwd_weak));

            passwdValid = true;
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM){
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                firstShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                secondShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
            } else{
                firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_medium));
                fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
                fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            }

            passwdType.setText(getString(R.string.pass_medium));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.green_unlocked_rewards));

            passwdAdvice.setText(getString(R.string.passwd_medium));

            passwdValid = true;
        }
        else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD){
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                firstShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                secondShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.shape_password));
            } else{
                firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_good));
                fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_password));
            }

            passwdType.setText(getString(R.string.pass_good));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.pass_good));

            passwdAdvice.setText(getString(R.string.passwd_good));

            passwdValid = true;
        }
        else {
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                firstShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                secondShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            } else{
                firstShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                secondShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                tirdShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                fourthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
                fifthShape.setBackground(ContextCompat.getDrawable(context, R.drawable.passwd_strong));
            }

            passwdType.setText(getString(R.string.pass_strong));
            passwdType.setTextColor(ContextCompat.getColor(context, R.color.blue_unlocked_rewards));

            passwdAdvice.setText(getString(R.string.passwd_strong));

            passwdValid = true;
        }
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
        LogUtil.logDebug("onClick");

        switch (v.getId()) {
            case R.id.create_account_chkTOS:
                hidePasswordIfVisible();
                break;

            case R.id.button_create_account_create:
                hidePasswordIfVisible();
                onCreateAccountClick(v);
                break;

            case R.id.button_login_create:
                hidePasswordIfVisible();
                ((LoginActivityLollipop) context).showFragment(Constants.LOGIN_FRAGMENT);
                break;

            case R.id.tos:
                LogUtil.logDebug("Show ToS");
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

    public void onCreateAccountClick (View v){
        submitForm();
    }

    /*
	 * Registration form submit
	 */
    private void submitForm() {
        LogUtil.logDebug("submit form!");

//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        dbH.clearCredentials();
//        megaApi.localLogout();

        if (!validateForm()) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(userEmail.getWindowToken(), 0);

        if(!Util.isOnline(context))
        {
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        creatingAccountTextView.setVisibility(View.GONE);
        createAccountProgressBar.setVisibility(View.VISIBLE);

        if(!Util.isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
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
        }
        return true;
    }

    private String getEmailError() {
        String value = userEmail.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_email);
        }
        if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
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
        if (value.length() == 0) {
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
        if (password.equals(confirm) == false) {
            return getString(R.string.error_passwords_dont_match);
        }
        return null;
    }

    private void onKeysGenerated(final String privateKey, final String publicKey) {
        if(!Util.isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
        megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), userLastName.getText().toString(),this);
//		megaApi.fastCreateAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), privateKey, userName.getText().toString().trim(), this);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        LogUtil.logDebug("onRequestStart" + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request,
                                MegaError e) {
        LogUtil.logDebug("onRequestFinish");

        if (isAdded()) {
            if (e.getErrorCode() != MegaError.API_OK) {
                LogUtil.logWarning("ERROR CODE: " + e.getErrorCode() + "_ ERROR MESSAGE: " + e.getErrorString());

                if (e.getErrorCode() == MegaError.API_EEXIST) {
                    try {
                        ((LoginActivityLollipop) context).showSnackbar(getString(R.string.error_email_registered));
                        createAccountLayout.setVisibility(View.VISIBLE);
                        creatingAccountLayout.setVisibility(View.GONE);
                        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
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
                        ((LoginActivityLollipop) context).showFragment(Constants.LOGIN_FRAGMENT);
                        createAccountLayout.setVisibility(View.VISIBLE);
                        creatingAccountLayout.setVisibility(View.GONE);
                        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
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

                ((LoginActivityLollipop)context).showFragment(Constants.CONFIRM_EMAIL_FRAGMENT);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        LogUtil.logWarning("onRequestTemporaryError");
    }


    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttach(Context context) {
        LogUtil.logDebug("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        LogUtil.logDebug("onAttach Activity");
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
        Display  display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        switch (editText.getId()){
            case R.id.create_account_email_text:{
                email_error_layout.setVisibility(View.VISIBLE);
                email_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = email_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    userEmail.setBackgroundDrawable(background);
                } else{
                    userEmail.setBackground(background);
                }
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userEmail.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userEmail.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_password_text_confirm:{
                password_confirm_error_layout.setVisibility(View.VISIBLE);
                password_confirm_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = password_confirm_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    userPasswordConfirm.setBackgroundDrawable(background);
                } else{
                    userPasswordConfirm.setBackground(background);
                }
                RelativeLayout.LayoutParams textParamsEditText = (RelativeLayout.LayoutParams) userPasswordConfirm.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userPasswordConfirm.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_name_text:{
                name_error_layout.setVisibility(View.VISIBLE);
                name_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = name_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    userName.setBackgroundDrawable(background);
                } else{
                    userName.setBackground(background);
                }
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userName.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userName.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_last_name_text:{
                last_name_error_layout.setVisibility(View.VISIBLE);
                last_name_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                Drawable background = lastname_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    userLastName.setBackgroundDrawable(background);
                } else{
                    userLastName.setBackground(background);
                }
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userLastName.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userLastName.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_password_text:{
                password_error_layout.setVisibility(View.VISIBLE);
                password_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = password_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    userPassword.setBackgroundDrawable(background);
                } else{
                    userPassword.setBackground(background);
                }
                RelativeLayout.LayoutParams textParamsEditText = (RelativeLayout.LayoutParams) userPassword.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userPassword.setLayoutParams(textParamsEditText);
            }
            break;
        }
    }

    private void quitError(EditText editText){
        Display  display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        switch (editText.getId()){
            case R.id.create_account_email_text:{
                if(email_error_layout.getVisibility() != View.GONE){
                    email_error_layout.setVisibility(View.GONE);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        userEmail.setBackgroundDrawable(email_background);
                    } else{
                        userEmail.setBackground(email_background);
                    }
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)userEmail.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userEmail.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_password_text_confirm:{
                if(password_confirm_error_layout.getVisibility() != View.GONE){
                    password_confirm_error_layout.setVisibility(View.GONE);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        userPasswordConfirm.setBackgroundDrawable(password_confirm_background);
                    } else{
                        userPasswordConfirm.setBackground(password_confirm_background);
                    }
                    RelativeLayout.LayoutParams textParamsEditText = (RelativeLayout.LayoutParams) userPasswordConfirm.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userPasswordConfirm.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_name_text:{
                if(name_error_layout.getVisibility() != View.GONE){
                    name_error_layout.setVisibility(View.GONE);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        userName.setBackgroundDrawable(name_background);
                    } else{
                        userName.setBackground(name_background);
                    }
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)userName.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userName.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_last_name_text:{
                if(last_name_error_layout.getVisibility() != View.GONE){
                    last_name_error_layout.setVisibility(View.GONE);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        userLastName.setBackgroundDrawable(lastname_background);
                    } else{
                        userLastName.setBackground(lastname_background);
                    }
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)userLastName.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userLastName.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_password_text:{
                if(password_error_layout.getVisibility() != View.GONE){
                    password_error_layout.setVisibility(View.GONE);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        userPassword.setBackgroundDrawable(password_background);
                    } else{
                        userPassword.setBackground(password_background);
                    }
                    RelativeLayout.LayoutParams textParamsEditText = (RelativeLayout.LayoutParams) userPassword.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userPassword.setLayoutParams(textParamsEditText);
                }
            }
            break;
        }
    }
}
