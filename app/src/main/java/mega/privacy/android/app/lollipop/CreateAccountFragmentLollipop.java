package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
    private LinearLayout createAccountLoginLayout;

    private TextView creatingAccountTextView;
    private ProgressBar createAccountProgressBar;

    private RelativeLayout email_error_layout;
    private RelativeLayout password_confirm_error_layout;
    private RelativeLayout name_error_layout;
    private RelativeLayout password_error_layout;
    private TextView email_error_text;
    private TextView password_confirm_error_text;
    private TextView name_error_text;
    private TextView password_error_text;


    private Drawable email_background;
    private Drawable password_confirm_background;
    private Drawable name_background;
    private Drawable password_background;

     /*
     * Task to process email and password
     */
    private class HashTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... args) {
            String privateKey = megaApi.getBase64PwKey(args[1]);
            String publicKey = megaApi.getStringHash(privateKey, args[0]);
            return new String[]{new String(privateKey), new String(publicKey)};
        }

        @Override
        protected void onPostExecute(String[] key) {
            onKeysGenerated(key[0], key[1]);
        }
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        View v = inflater.inflate(R.layout.fragment_create_account, container, false);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_account);
        createAccountLayout = (LinearLayout) v.findViewById(R.id.create_account_create_layout);
        createAccountLoginLayout = (LinearLayout) v.findViewById(R.id.create_account_login_layout);
        createAccountTitle = (TextView) v.findViewById(R.id.create_account_text_view);

        userName = (EditText) v.findViewById(R.id.create_account_name_text);
        userLastName = (EditText) v.findViewById(R.id.create_account_last_name_text);
        userEmail = (EditText) v.findViewById(R.id.create_account_email_text);
        userPassword = (EditText) v.findViewById(R.id.create_account_password_text);
        userPasswordConfirm = (EditText) v.findViewById(R.id.create_account_password_text_confirm);

        userName.getBackground().clearColorFilter();
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
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userPassword);
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
        createAccountLoginLayout.setVisibility(View.VISIBLE);
        creatingAccountLayout.setVisibility(View.GONE);
        creatingAccountTextView.setVisibility(View.GONE);
        createAccountProgressBar.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        switch (v.getId()) {
            case R.id.button_create_account_create:
                onCreateAccountClick(v);
                break;

            case R.id.button_login_create:
                ((LoginActivityLollipop) context).showFragment(Constants.LOGIN_FRAGMENT);
                break;

            case R.id.tos:
                log("Show tos");
//				Intent browserIntent = new Intent(Intent.ACTION_VIEW);
//				browserIntent.setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
//				browserIntent.setDataAndType(Uri.parse("http://www.google.es"), "text/html");
//				browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
//				startActivity(browserIntent);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://mega.co.nz/mobile_privacy.html"));
                startActivity(viewIntent);
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
        log("submit form!");

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
        createAccountLoginLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountTextView.setVisibility(View.GONE);
        createAccountProgressBar.setVisibility(View.VISIBLE);

        if(!Util.isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        createAccountLayout.setVisibility(View.GONE);
        createAccountLoginLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
        log("[CREDENTIALS]userEmail: _" + userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH) + "_");
        log("[CREDENTIALS]userPassword: _" + userPassword.getText().toString() +"_");
        megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), userLastName.getText().toString(),this);

//        new HashTask().execute(userEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim(), userPassword.getText().toString());
    }

    private boolean validateForm() {
        String emailError = getEmailError();
        String passwordError = getPasswordError();
        String usernameError = getUsernameError();
        String passwordConfirmError = getPasswordConfirmError();

        // Set or remove errors
        setError(userName, usernameError);
        setError(userEmail, emailError);
        setError(userPassword, passwordError);
        setError(userPasswordConfirm, passwordConfirmError);

        // Return false on any error or true on success
        if (usernameError != null) {
            userName.requestFocus();
            return false;
        } else if (emailError != null) {
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
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

    private String getPasswordError() {
        String value = userPassword.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_password);
        } else if (value.length() < 5) {
            return getString(R.string.error_short_password);
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
        createAccountLoginLayout.setVisibility(View.GONE);
        creatingAccountLayout.setVisibility(View.VISIBLE);
        creatingAccountTextView.setVisibility(View.VISIBLE);
        createAccountProgressBar.setVisibility(View.VISIBLE);
        log("[CREDENTIALS]userEmail: _" + userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH) + "_");
        log("[CREDENTIALS]userPassword: _" + userPassword.getText().toString() +"_");
        megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), userLastName.getText().toString(),this);
//		megaApi.fastCreateAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), privateKey, userName.getText().toString().trim(), this);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart" + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request,
                                MegaError e) {
        log("onRequestFinish");
        if (e.getErrorCode() != MegaError.API_OK) {
            log("ERROR CODE: " + e.getErrorCode() + "_ ERROR MESSAGE: " + e.getErrorString());

            if (e.getErrorCode() == MegaError.API_EEXIST) {
                ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_email_registered));
                createAccountLayout.setVisibility(View.VISIBLE);
                createAccountLoginLayout.setVisibility(View.VISIBLE);
                creatingAccountLayout.setVisibility(View.GONE);
                creatingAccountTextView.setVisibility(View.GONE);
                createAccountProgressBar.setVisibility(View.GONE);
                return;
            }
            else{
                String message = e.getErrorString();
                ((LoginActivityLollipop)context).showSnackbar(message);
                ((LoginActivityLollipop)context).showFragment(Constants.LOGIN_FRAGMENT);
                createAccountLayout.setVisibility(View.VISIBLE);
                createAccountLoginLayout.setVisibility(View.VISIBLE);
                creatingAccountLayout.setVisibility(View.GONE);
                creatingAccountTextView.setVisibility(View.GONE);
                createAccountProgressBar.setVisibility(View.GONE);
                return;
            }
        }
        else{
            ((LoginActivityLollipop)context).setEmailTemp(userEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim());
            ((LoginActivityLollipop)context).setFirstNameTemp(userName.getText().toString());
            ((LoginActivityLollipop)context).setPasswdTemp(userPassword.getText().toString());
            ((LoginActivityLollipop)context).setWaitingForConfirmAccount(true);

            DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
            if (dbH != null){
                dbH.clearEphemeral();

                log("EphemeralCredentials: (" + request.getEmail() + "," + request.getPassword() + "," + request.getSessionKey() + "," + request.getName() + "," + request.getText() + ")");
                EphemeralCredentials ephemeral = new EphemeralCredentials(request.getEmail(), request.getPassword(), request.getSessionKey(), request.getName(), request.getText());

                dbH.saveEphemeral(ephemeral);
            }

            ((LoginActivityLollipop)context).showFragment(Constants.CONFIRM_EMAIL_FRAGMENT);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log ("onRequestTemporaryError");
    }


    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
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
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = email_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userEmail.setBackground(background);
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userEmail.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userEmail.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_password_text_confirm:{
                password_confirm_error_layout.setVisibility(View.VISIBLE);
                password_confirm_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = password_confirm_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userPasswordConfirm.setBackground(background);
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userPasswordConfirm.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userPasswordConfirm.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_name_text:{
                name_error_layout.setVisibility(View.VISIBLE);
                name_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = name_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userName.setBackground(background);
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userName.getLayoutParams();
                textParamsEditText.bottomMargin = Util.scaleWidthPx(3, outMetrics);
                userName.setLayoutParams(textParamsEditText);
            }
            break;
            case R.id.create_account_password_text:{
                password_error_layout.setVisibility(View.VISIBLE);
                password_error_text.setText(error);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
                Drawable background = password_background.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userPassword.setBackground(background);
                LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userPassword.getLayoutParams();
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
                    userEmail.setBackground(email_background);
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)userEmail.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userEmail.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_password_text_confirm:{
                if(password_confirm_error_layout.getVisibility() != View.GONE){
                    password_confirm_error_layout.setVisibility(View.GONE);
                    userPasswordConfirm.setBackground(password_confirm_background);
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userPasswordConfirm.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userPasswordConfirm.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_name_text:{
                if(name_error_layout.getVisibility() != View.GONE){
                    name_error_layout.setVisibility(View.GONE);
                    userName.setBackground(name_background);
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)userName.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userName.setLayoutParams(textParamsEditText);
                }
            }
            break;
            case R.id.create_account_password_text:{
                if(password_error_layout.getVisibility() != View.GONE){
                    password_error_layout.setVisibility(View.GONE);
                    userPassword.setBackground(password_background);
                    LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) userPassword.getLayoutParams();
                    textParamsEditText.bottomMargin = Util.scaleWidthPx(10, outMetrics);
                    userPassword.setLayoutParams(textParamsEditText);
                }
            }
            break;
        }
    }

    public static void log(String log) {
        Util.log("CreateAccountFragmentLollipop", log);
    }
}
