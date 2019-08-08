package mega.privacy.android.app.lollipop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

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

import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.FileUtils.RK_FILE;

/**
 * Created by mega on 3/04/18.
 */

public class TestPasswordActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {

    LinearLayout passwordReminderLayout;
    ImageView passwordReminderCloseButton;
    CheckBox blockCheckBox;
    TextView dialogTest;
    Button testPasswordButton;
    Button passwordReminderBackupRecoveryKeyButton;
    Button passwordReminderDismissButton;

    LinearLayout testPasswordLayout;
    Toolbar tB;
    ActionBar aB;
    private EditText passwordEditText;
    private ImageView passwordToggle;
    private TextView passwordErrorText;
    private ImageView passwordErrorImage;
    private Button confirmPasswordButton;
    private Button testPasswordbackupRecoveryKeyButton;
    private Button testPasswordDismissButton;
    private RelativeLayout containerPasswordError;
    private TextView enterPwdHint;
    private Button proceedToLogout;

    private ProgressBar progressBar;

    private Drawable password_background;

    private boolean passwdVisibility = false;
    private boolean passwordCorrect = false;
    private boolean logout = false;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    int counter = 0;
    boolean testingPassword = false;
    boolean dismissPasswordReminder = false;
    int numRequests = 0;

    private RecoveryKeyBottomSheetDialogFragment recoveryKeyBottomSheetDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_password);
        if (getIntent() == null){
            log("intent NULL");
            return;
        }

        if (savedInstanceState != null){
            counter = savedInstanceState.getInt("counter", 0);
            testingPassword = savedInstanceState.getBoolean("testingPassword", false);
        }
        else {
            counter = 0;
            testingPassword = false;
        }

        logout = getIntent().getBooleanExtra("logout", false);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

        megaApi = ((MegaApplication)getApplication()).getMegaApi();

        passwordReminderLayout = (LinearLayout) findViewById(R.id.password_reminder_layout);
        passwordReminderCloseButton = (ImageView) findViewById(R.id.password_reminder_close_image_button);
        passwordReminderCloseButton.setOnClickListener(this);
        dialogTest = (TextView) findViewById(R.id.password_reminder_text);
        blockCheckBox = (CheckBox) findViewById(R.id.password_reminder_checkbox);
        blockCheckBox.setOnClickListener(this);
        testPasswordButton = (Button) findViewById(R.id.password_reminder_test_button);
        testPasswordButton.setOnClickListener(this);
        passwordReminderBackupRecoveryKeyButton = (Button) findViewById(R.id.password_reminder_recoverykey_button);
        passwordReminderBackupRecoveryKeyButton.setOnClickListener(this);
        passwordReminderDismissButton = (Button) findViewById(R.id.password_reminder_dismiss_button);
        passwordReminderDismissButton.setOnClickListener(this);

        testPasswordLayout = (LinearLayout) findViewById(R.id.test_password_layout);
        tB = (Toolbar) findViewById(R.id.toolbar);
        enterPwdHint = (TextView) findViewById(R.id.test_password_enter_pwd_hint);
        passwordEditText = (EditText) findViewById(R.id.test_password_edittext);
        passwordToggle = (ImageView) findViewById(R.id.toggle_button);
        passwordToggle.setOnClickListener(this);
        passwordErrorText = (TextView) findViewById(R.id.test_password_text_error_text);
        passwordErrorImage = (ImageView) findViewById(R.id.test_password_text_error_icon);
        confirmPasswordButton = (Button) findViewById(R.id.test_password_confirm_button);
        confirmPasswordButton.setOnClickListener(this);
        testPasswordbackupRecoveryKeyButton = (Button) findViewById(R.id.test_password_backup_button);
        testPasswordbackupRecoveryKeyButton.setOnClickListener(this);
        testPasswordDismissButton = (Button) findViewById(R.id.test_password_dismiss_button);
        testPasswordDismissButton.setOnClickListener(this);
        containerPasswordError = (RelativeLayout) findViewById(R.id.test_password_text_error);
        proceedToLogout = (Button) findViewById(R.id.proceed_to_logout_button);
        proceedToLogout.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        if (isLogout()) {
            passwordReminderCloseButton.setVisibility(View.VISIBLE);
            dialogTest.setText(R.string.remember_pwd_dialog_text_logout);
            passwordReminderDismissButton.setText(R.string.proceed_to_logout);
            passwordReminderDismissButton.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
            testPasswordDismissButton.setVisibility(View.GONE);
            proceedToLogout.setVisibility(View.VISIBLE);
        }
        else {
            passwordReminderCloseButton.setVisibility(View.GONE);
            dialogTest.setText(R.string.remember_pwd_dialog_text);
            passwordReminderDismissButton.setText(R.string.general_dismiss);
            passwordReminderDismissButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
            testPasswordDismissButton.setVisibility(View.VISIBLE);
            proceedToLogout.setVisibility(View.GONE);
        }

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

        enterPwdHint.setVisibility(View.INVISIBLE);
        passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterPwdHint.setVisibility(View.VISIBLE);
                    passwordEditText.setHint(null);
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

        if (testingPassword) {
            setTestPasswordLayout();
        }
        else {
            passwordReminderLayout.setVisibility(View.VISIBLE);
            testPasswordLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("counter", counter);
        outState.putBoolean("testingPassword", testingPassword);
    }

    void setTestPasswordLayout () {
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setTitle(getString(R.string.remember_pwd_dialog_button_test).toUpperCase());
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        passwordReminderLayout.setVisibility(View.GONE);
        testPasswordLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        dismissActivity(false);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    void quitError(){
        if(containerPasswordError.getVisibility() != View.INVISIBLE){
            enterPwdHint.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
            containerPasswordError.setVisibility(View.INVISIBLE);
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                passwordEditText.setBackgroundDrawable(password_background);
            } else{
                passwordEditText.setBackground(password_background);
            }
            testPasswordbackupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
            confirmPasswordButton.setEnabled(true);
            confirmPasswordButton.setAlpha(1F);
        }
    }

    void showError (boolean correct) {
        Util.hideKeyboard(this, 0);
        if(containerPasswordError.getVisibility() == View.INVISIBLE){
            containerPasswordError.setVisibility(View.VISIBLE);
            Drawable background = password_background.mutate().getConstantState().newDrawable();
            PorterDuffColorFilter porterDuffColorFilter;
            if (correct){
                porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.green_unlocked_rewards), PorterDuff.Mode.SRC_ATOP);
                passwordErrorText.setText(getString(R.string.test_pwd_accepted));
                passwordErrorText.setTextColor(ContextCompat.getColor(this, R.color.green_unlocked_rewards));
                passwordErrorImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_accept_test));
                testPasswordbackupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
                passwordEditText.setEnabled(false);
                passwordReminderSucceeded();
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
                testPasswordbackupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
                if (counter == 3) {
                    Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
                    intent.putExtra("logout", isLogout());
                    startActivity(intent);
                    onBackPressed();
                }
            }
            confirmPasswordButton.setEnabled(false);
            confirmPasswordButton.setAlpha(0.3F);
            background.setColorFilter(porterDuffColorFilter);
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                passwordEditText.setBackgroundDrawable(background);
            } else{
                passwordEditText.setBackground(background);
            }
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
                String[] split = RK_FILE.split(File.separator);
                parentPath = parentPath+"/"+split[split.length-1];
                AccountController ac = new AccountController(this);
                ac.exportMK(parentPath, false);
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.password_reminder_checkbox: {
                if (blockCheckBox.isChecked()) {
                    log("Block CheckBox checked!");
                }
                else {
                    log("Block CheckBox does NOT checked!");
                }
                break;
            }
            case R.id.password_reminder_test_button: {
                shouldBlockPasswordReminder();
                testingPassword = true;
                setTestPasswordLayout();
                break;
            }
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
                String password = passwordEditText.getText().toString();
                passwordCorrect = megaApi.checkPassword(password);
                showError(passwordCorrect);
                break;
            }
            case R.id.password_reminder_recoverykey_button:
            case R.id.test_password_backup_button: {
                if (isBottomSheetDialogShown(recoveryKeyBottomSheetDialogFragment)) break;

                recoveryKeyBottomSheetDialogFragment = new RecoveryKeyBottomSheetDialogFragment();
                recoveryKeyBottomSheetDialogFragment.show(getSupportFragmentManager(), recoveryKeyBottomSheetDialogFragment.getTag());
                break;
            }
            case R.id.test_password_dismiss_button:
            case R.id.password_reminder_close_image_button: {
                onBackPressed();
                break;
            }
            case R.id.password_reminder_dismiss_button: {
                if (isLogout()) {
                    dismissActivity(true);
                }
                else {
                    onBackPressed();
                }
            }
            case R.id.proceed_to_logout_button: {
                dismissActivity(true);
                break;
            }
        }
    }

    void disableUI () {
        if (passwordReminderLayout.getVisibility() == View.VISIBLE) {
            passwordReminderLayout.setEnabled(false);
            passwordReminderLayout.setAlpha(0.3F);
        }
        else if (testPasswordLayout.getVisibility() == View.VISIBLE) {
            testPasswordLayout.setEnabled(false);
            testPasswordLayout.setAlpha(0.3F);
        }
        progressBar.setVisibility(View.VISIBLE);
    }

    public void passwordReminderSucceeded() {
        shouldBlockPasswordReminder();
        enableDismissPasswordReminder();
        numRequests++;
        megaApi.passwordReminderDialogSucceeded(this);
        if (isLogout()) {
            disableUI();
        }
        else {
            finish();
        }
    }

    public void dismissActivity(boolean enableDismissPasswordReminder) {
        if (enableDismissPasswordReminder) {
            enableDismissPasswordReminder();
            if (isLogout()) {
                disableUI();
            }
        }
        numRequests++;
        megaApi.passwordReminderDialogSkipped(this);
        shouldBlockPasswordReminder();
    }

    void shouldBlockPasswordReminder() {
        if (blockCheckBox.isChecked()) {
            numRequests++;
            megaApi.passwordReminderDialogBlocked(this);
        }
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        showSnackbar(findViewById(R.id.container_layout), s);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_WRITE_STORAGE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    log("REQUEST_WRITE_STORAGE PERMISSIONS GRANTED");
                }
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
        if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER){
            if (request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER) {
                numRequests--;
                if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
                    log("New value of attribute USER_ATTR_PWD_REMINDER: " + request.getText());
                    if (dismissPasswordReminder && isLogout() && numRequests <= 0) {
                        AccountController ac = new AccountController(this);
                        ac.logout(this, megaApi);
                    }
                }
                else {
                    log("Error: MegaRequest.TYPE_SET_ATTR_USER | MegaApiJava.USER_ATTR_PWD_REMINDER " + e.getErrorString());
                }
            }
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

    public boolean isLogout() {
        return logout;
    }

    public void enableDismissPasswordReminder() {
        dismissPasswordReminder = true;
    }

    public void incrementRequests() {
        numRequests++;
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
