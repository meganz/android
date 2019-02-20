package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.modalbottomsheet.RecoveryKeyBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

/**
 * Created by mega on 3/04/18.
 */

public class TestPasswordActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {

    private EditText passwordEditText;
    private ImageView passwordToggle;
    private TextView passwordErrorText;
    private ImageView passwordErrorImage;
    private Button confirmPasswordButton;
    private Button backupRecoveryKeyButton;
    private Button dismissButton;
    private RelativeLayout containerPasswordError;
    private TextView enterPwdHint;

    private Drawable password_background;

    private boolean passwdVisibility = false;
    private boolean passwordCorrect = false;
    private boolean logout = false;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_password);
        if (getIntent() == null){
            log("intent NULL");
            return;
        }

        if (savedInstanceState != null){
            logout = savedInstanceState.getBoolean("logout", false);
            counter = savedInstanceState.getInt("counter", 0);
        }
        else {
            logout = getIntent().getBooleanExtra("rememberPasswordLogout", false);
            counter = 0;
        }

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

        megaApi = ((MegaApplication)getApplication()).getMegaApi();

        enterPwdHint = (TextView) findViewById(R.id.enter_pwd_hint);
        passwordEditText = (EditText) findViewById(R.id.test_password_edittext);
        passwordToggle = (ImageView) findViewById(R.id.toggle_button);
        passwordErrorText = (TextView) findViewById(R.id.test_password_text_error_text);
        passwordErrorImage = (ImageView) findViewById(R.id.test_password_text_error_icon);
        confirmPasswordButton = (Button) findViewById(R.id.test_password_confirm_button);
        backupRecoveryKeyButton = (Button) findViewById(R.id.test_password_backup_button);
        dismissButton = (Button) findViewById(R.id.test_password_dismiss_button);
        containerPasswordError = (RelativeLayout) findViewById(R.id.test_password_text_error);

        passwordEditText.getBackground().clearColorFilter();

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String password = passwordEditText.getText().toString();
                    passwordCorrect = megaApi.checkPassword(password);
                    showError(passwordCorrect);
                    return true;
                }
                return false;
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!passwordCorrect){
                    quitError();
                }
            }
        });

        passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    passwordToggle.setVisibility(View.VISIBLE);
                    passwordToggle.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_b_shared_read));
                }
                else {
                    passwordToggle.setVisibility(View.GONE);
                    passwdVisibility = false;
                    showHidePassword();
                }
            }
        });
        password_background = passwordEditText.getBackground().mutate().getConstantState().newDrawable();

        passwordToggle.setOnClickListener(this);
        confirmPasswordButton.setOnClickListener(this);
        backupRecoveryKeyButton.setOnClickListener(this);
        dismissButton.setOnClickListener(this);
        if (logout) {
            backupRecoveryKeyButton.setText(R.string.option_export_recovery_key);
            dismissButton.setText(R.string.option_logout_anyway);
        }
        else {
            backupRecoveryKeyButton.setText(R.string.action_export_master_key);
            dismissButton.setText(R.string.general_dismiss);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("logout", logout);
        outState.putInt("counter", counter);
    }

    void quitError(){
        if(containerPasswordError.getVisibility() != View.GONE){
            enterPwdHint.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
            containerPasswordError.setVisibility(View.GONE);
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                passwordEditText.setBackgroundDrawable(password_background);
            } else{
                passwordEditText.setBackground(password_background);
            }
            backupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
        }
    }

    void showError (boolean correct) {
        hideKeyboard();
        if(containerPasswordError.getVisibility() == View.GONE){
            containerPasswordError.setVisibility(View.VISIBLE);
            Drawable background = password_background.mutate().getConstantState().newDrawable();
            PorterDuffColorFilter porterDuffColorFilter;
            if (correct){
                porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.green_unlocked_rewards), PorterDuff.Mode.SRC_ATOP);
                passwordErrorText.setText(getString(R.string.test_pwd_accepted));
                passwordErrorText.setTextColor(ContextCompat.getColor(this, R.color.green_unlocked_rewards));
                passwordErrorImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_accept_test));
                confirmPasswordButton.setVisibility(View.GONE);
                backupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
                passwordEditText.setEnabled(false);
                megaApi.passwordReminderDialogSucceeded(this);
                if (logout) {
                    dismissButton.setText(R.string.action_logout);
                }
            }
            else {
                counter++;
                porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                passwordErrorText.setText(getString(R.string.test_pwd_wrong));
                enterPwdHint.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
                passwordErrorText.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
                Drawable errorIcon = ContextCompat.getDrawable(this, R.drawable.ic_input_warning);
                errorIcon.setColorFilter(porterDuffColorFilter);
                passwordErrorImage.setImageDrawable(errorIcon);
                confirmPasswordButton.setVisibility(View.VISIBLE);
                backupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
                if (counter == 3) {
                    Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
                    startActivity(intent);
                    finish();
                }
            }
            background.setColorFilter(porterDuffColorFilter);
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                passwordEditText.setBackgroundDrawable(background);
            } else{
                passwordEditText.setBackground(background);
            }
        }
    }

    void hideKeyboard (){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    void showHidePassword (){
        if(!passwdVisibility){
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
            passwordEditText.setSelection(passwordEditText.getText().length());
        }else{
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordEditText.setSelection(passwordEditText.getText().length());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK){
            log("REQUEST_DOWNLOAD_FOLDER");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null){
                log("parentPath no NULL");
                String[] split = Util.rKFile.split("/");
                parentPath = parentPath+"/"+split[split.length-1];
                Intent newIntent = new Intent(this, ManagerActivityLollipop.class);
                newIntent.putExtra("parentPath", parentPath);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                newIntent.setAction(Constants.ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT);
                startActivity(newIntent);
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.toggle_button:{
                if (passwdVisibility) {
                    passwordToggle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_shared_read));
                    passwdVisibility = false;
                    showHidePassword();
                }
                else {
                    passwordToggle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_see));
                    passwdVisibility = true;
                    showHidePassword();
                }
                break;
            }
            case R.id.test_password_confirm_button:{
                if (megaApi == null) {
                    megaApi = ((MegaApplication)getApplication()).getMegaApi();
                }
                String password = passwordEditText.getText().toString();
                passwordCorrect = megaApi.checkPassword(password);
                showError(passwordCorrect);
                break;
            }
            case R.id.test_password_backup_button:{
                if (logout){
                    RecoveryKeyBottomSheetDialogFragment recoveryKeyBottomSheetDialogFragment = new RecoveryKeyBottomSheetDialogFragment();
                    recoveryKeyBottomSheetDialogFragment.show(getSupportFragmentManager(), recoveryKeyBottomSheetDialogFragment.getTag());
                }
                else {
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setAction(Constants.ACTION_RECOVERY_KEY_EXPORTED);
                    startActivity(intent);
                }
                break;
            }
            case R.id.test_password_dismiss_button:{
                megaApi.passwordReminderDialogSkipped(this);
                if (logout){
                    AccountController ac = new AccountController(this);
                    ac.logout(this, megaApi);
                }
                this.finish();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("TestPasswordActivity", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if(request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER && e.getErrorCode() == MegaError.API_OK){
            log("New value of attribute USER_ATTR_PWD_REMINDER: " +request.getText());
        }
        else if (request.getType() == MegaRequest.TYPE_LOGOUT){
            log("logout finished");

            if(Util.isChatEnabled()){
                log("END logout sdk request - wait chat logout");
            }
            else{
                log("END logout sdk request - chat disabled");
                if (dbH == null){
                    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                }
                if (dbH != null){
                    dbH.clearEphemeral();
                }

                AccountController aC = new AccountController(this);
                aC.logoutConfirmed(this);

                Intent tourIntent = new Intent(this, LoginActivityLollipop.class);
                tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                this.startActivity(tourIntent);

                finish();
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
