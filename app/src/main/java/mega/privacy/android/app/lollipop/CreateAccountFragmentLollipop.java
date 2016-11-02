package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
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

    Context context;

    TextView bRegister;
    TextView bLogin;
    TextView createAccountTitle;
    TextView textAlreadyAccount;
    EditText userName;
    EditText userLastName;
    EditText userEmail;
    EditText userPassword;
    EditText userPasswordConfirm;
    ScrollView scrollView;

    CheckBox chkTOS;
    TextView tos;

    MegaApiAndroid megaApi;

    LinearLayout createAccountLayout;
    LinearLayout creatingAccountLayout;
    LinearLayout createAccountLoginLayout;

    TextView creatingAccountTextView;
    ProgressBar createAccountProgressBar;


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
        float density  = getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(outMetrics, density);
        float scaleH = Util.getScaleH(outMetrics, density);
        float scaleText;
        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

        megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_account);
        createAccountLayout = (LinearLayout) v.findViewById(R.id.create_account_create_layout);
        createAccountLoginLayout = (LinearLayout) v.findViewById(R.id.create_account_login_layout);
        createAccountTitle = (TextView) v.findViewById(R.id.create_account_text_view);
        //Left margin
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)createAccountTitle.getLayoutParams();
        textParams.setMargins(Util.scaleHeightPx(30, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, Util.scaleHeightPx(20, outMetrics));
        createAccountTitle.setLayoutParams(textParams);
        createAccountTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (22*scaleText));

        userName = (EditText) v.findViewById(R.id.create_account_name_text);
        android.view.ViewGroup.LayoutParams paramsb1 = userName.getLayoutParams();
        paramsb1.width = Util.scaleWidthPx(280, outMetrics);
        userName.setLayoutParams(paramsb1);
        //Left margin
        textParams = (LinearLayout.LayoutParams)userName.getLayoutParams();
        textParams.setMargins(Util.scaleWidthPx(30, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
        userName.setLayoutParams(textParams);

        userLastName = (EditText) v.findViewById(R.id.create_account_last_name_text);
        android.view.ViewGroup.LayoutParams paramsb2 = userLastName.getLayoutParams();
        paramsb2.width = Util.scaleWidthPx(280, outMetrics);
        userLastName.setLayoutParams(paramsb2);
        //Left margin
        textParams = (LinearLayout.LayoutParams)userLastName.getLayoutParams();
        textParams.setMargins(Util.scaleWidthPx(30, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
        userLastName.setLayoutParams(textParams);

        userEmail = (EditText) v.findViewById(R.id.create_account_email_text);
        userEmail.setLayoutParams(paramsb1);
        userEmail.setLayoutParams(textParams);

        userPassword = (EditText) v.findViewById(R.id.create_account_password_text);
        userPassword.setLayoutParams(paramsb1);
        userPassword.setLayoutParams(textParams);

        userPasswordConfirm = (EditText) v.findViewById(R.id.create_account_password_text_confirm);
        userPasswordConfirm.setLayoutParams(paramsb1);
        userPasswordConfirm.setLayoutParams(textParams);

        TextView tos = (TextView)v.findViewById(R.id.tos);
        tos.setTextColor(getResources().getColor(R.color.mega));
        tos.setOnClickListener(this);
        LinearLayout.LayoutParams textTos = (LinearLayout.LayoutParams)tos.getLayoutParams();
        textTos.setMargins(Util.scaleWidthPx(35, outMetrics), 0, Util.scaleWidthPx(10, outMetrics), 0);
        tos.setLayoutParams(textTos);

        chkTOS = (CheckBox) v.findViewById(R.id.create_account_chkTOS);
        tos = (TextView) v.findViewById(R.id.tos);
        tos.setTextSize(TypedValue.COMPLEX_UNIT_SP, (8*scaleText));

        bRegister = (TextView) v.findViewById(R.id.button_create_account_create);
        bRegister.setText(getString(R.string.create_account).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsbRegister = bRegister.getLayoutParams();
        paramsbRegister.height = Util.scaleHeightPx(48, outMetrics);
		/*paramsbLogin.width = Util.scaleWidthPx(144, outMetrics);*/
        bRegister.setLayoutParams(paramsbRegister);
        //Margin
        LinearLayout.LayoutParams textParamsRegister = (LinearLayout.LayoutParams)bRegister.getLayoutParams();
        textParamsRegister.setMargins(Util.scaleWidthPx(35, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, 0);
        bRegister.setLayoutParams(textParamsRegister);
        bRegister.setOnClickListener(this);

        textAlreadyAccount = (TextView) v.findViewById(R.id.text_already_account);
        //Margins (left, top, right, bottom)
        LinearLayout.LayoutParams textAAccount = (LinearLayout.LayoutParams)textAlreadyAccount.getLayoutParams();
        textAAccount.setMargins(Util.scaleWidthPx(30, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(25, outMetrics));
        textAlreadyAccount.setLayoutParams(textAAccount);
        textAlreadyAccount.setTextSize(TypedValue.COMPLEX_UNIT_SP, (22*scaleText));

        bLogin = (TextView) v.findViewById(R.id.button_login_create);
        bLogin.setOnClickListener(this);

        bLogin.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsbLogin = bLogin.getLayoutParams();
        paramsbLogin.height = Util.scaleHeightPx(48, outMetrics);
		/*paramsbLogin.width = Util.scaleWidthPx(63, outMetrics);*/
        bLogin.setLayoutParams(paramsbLogin);
        //Margin
        LinearLayout.LayoutParams textParamsLogin = (LinearLayout.LayoutParams)bLogin.getLayoutParams();
        textParamsLogin.setMargins(Util.scaleWidthPx(35, outMetrics), 0, 0, 0);
        bLogin.setLayoutParams(textParamsLogin);

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

        switch (v.getId()) {
            case R.id.button_create_account_create:
                onCreateAccountClick(v);
                break;

            case R.id.button_login_create:
                ((LoginActivityLollipop) context).showFragment(Constants.LOGIN_FRAGMENT);
                break;

            case R.id.tos:
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
        megaApi.logout();

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

        new HashTask().execute(userEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim(), userPassword.getText().toString());
    }

    private boolean validateForm() {
        String emailError = getEmailError();
        String passwordError = getPasswordError();
        String usernameError = getUsernameError();
        String passwordConfirmError = getPasswordConfirmError();

        // Set or remove errors
        userName.setError(usernameError);
        userEmail.setError(emailError);
        userPassword.setError(passwordError);
        userPasswordConfirm.setError(passwordConfirmError);

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
                ((LoginActivityLollipop)context).showFragment(Constants.LOGIN_FRAGMENT);
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

    public static void log(String log) {
        Util.log("CreateAccountFragmentLollipop", log);
    }
}
