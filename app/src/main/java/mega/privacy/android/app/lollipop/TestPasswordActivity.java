package mega.privacy.android.app.lollipop;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.modalbottomsheet.RecoveryKeyBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class TestPasswordActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {

    private LinearLayout passwordReminderLayout;
    private ImageView passwordReminderCloseButton;
    private CheckBox blockCheckBox;
    private TextView dialogTest;
    private Button testPasswordButton;
    private Button passwordReminderBackupRecoveryKeyButton;
    private Button passwordReminderDismissButton;

    private LinearLayout testPasswordLayout;
    private Toolbar tB;
    private ActionBar aB;
    private TextInputLayout passwordLayout;
    private AppCompatEditText passwordText;
    private ImageView passwordErrorImage;
    private Button confirmPasswordButton;
    private Button testPasswordbackupRecoveryKeyButton;
    private Button testPasswordDismissButton;
    private Button proceedToLogout;

    private ProgressBar progressBar;

    private boolean passwordCorrect;
    private boolean logout;

    private int counter;
    private boolean testingPassword;
    private boolean dismissPasswordReminder;
    private int numRequests;

    private RecoveryKeyBottomSheetDialogFragment recoveryKeyBottomSheetDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_password);
        if (getIntent() == null){
            logWarning("Intent NULL");
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

        passwordReminderLayout = findViewById(R.id.password_reminder_layout);
        passwordReminderCloseButton = findViewById(R.id.password_reminder_close_image_button);
        passwordReminderCloseButton.setOnClickListener(this);
        dialogTest = findViewById(R.id.password_reminder_text);
        blockCheckBox = findViewById(R.id.password_reminder_checkbox);
        blockCheckBox.setOnClickListener(this);
        testPasswordButton = findViewById(R.id.password_reminder_test_button);
        testPasswordButton.setOnClickListener(this);
        passwordReminderBackupRecoveryKeyButton = findViewById(R.id.password_reminder_recoverykey_button);
        passwordReminderBackupRecoveryKeyButton.setOnClickListener(this);
        passwordReminderDismissButton = findViewById(R.id.password_reminder_dismiss_button);
        passwordReminderDismissButton.setOnClickListener(this);

        testPasswordLayout = findViewById(R.id.test_password_layout);
        tB = findViewById(R.id.toolbar);
        passwordLayout = findViewById(R.id.test_password_text_layout);
        passwordText = findViewById(R.id.test_password_edittext);
        passwordErrorImage = findViewById(R.id.test_password_text_error_icon);
        confirmPasswordButton = findViewById(R.id.test_password_confirm_button);
        confirmPasswordButton.setOnClickListener(this);
        testPasswordbackupRecoveryKeyButton = findViewById(R.id.test_password_backup_button);
        testPasswordbackupRecoveryKeyButton.setOnClickListener(this);
        testPasswordDismissButton = findViewById(R.id.test_password_dismiss_button);
        testPasswordDismissButton.setOnClickListener(this);
        proceedToLogout = findViewById(R.id.proceed_to_logout_button);
        proceedToLogout.setOnClickListener(this);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        if (isLogout()) {
            passwordReminderCloseButton.setVisibility(View.VISIBLE);
            dialogTest.setText(R.string.remember_pwd_dialog_text_logout);
            passwordReminderDismissButton.setText(R.string.proceed_to_logout);
            passwordReminderDismissButton.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300));
            testPasswordDismissButton.setVisibility(View.GONE);
            proceedToLogout.setVisibility(View.VISIBLE);
        }
        else {
            passwordReminderCloseButton.setVisibility(View.GONE);
            dialogTest.setText(R.string.remember_pwd_dialog_text);
            passwordReminderDismissButton.setText(R.string.general_dismiss);
            passwordReminderDismissButton.setTextColor(ColorUtils.getThemeColor(this, R.attr.colorSecondary));
            testPasswordDismissButton.setVisibility(View.VISIBLE);
            proceedToLogout.setVisibility(View.GONE);
        }

        passwordText.getBackground().clearColorFilter();

        passwordText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String password = passwordText.getText().toString();
                passwordCorrect = megaApi.checkPassword(password);
                showError(passwordCorrect);
                return true;
            }
            return false;
        });

        passwordText.addTextChangedListener(new TextWatcher() {
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

        passwordLayout.setEndIconVisible(false);
        passwordText.setOnFocusChangeListener((v1, hasFocus) ->
                passwordLayout.setEndIconVisible(hasFocus));

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
        passwordLayout.setError(null);
        passwordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
        passwordErrorImage.setVisibility(View.GONE);
        testPasswordbackupRecoveryKeyButton.setTextColor(ColorUtils.getThemeColor(this, R.attr.colorSecondary));
        confirmPasswordButton.setEnabled(true);
        confirmPasswordButton.setAlpha(1F);
    }

    void showError (boolean correct) {
        hideKeyboard(this, 0);

        Drawable icon;

        if (correct){
            passwordLayout.setError(getString(R.string.test_pwd_accepted));
            passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Medium);
            passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Medium);
            icon = ContextCompat.getDrawable(this, R.drawable.ic_accept_test);
            icon.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.green_500_green_400), PorterDuff.Mode.SRC_ATOP));
            passwordErrorImage.setImageDrawable(icon);
            testPasswordbackupRecoveryKeyButton.setTextColor(ColorUtils.getThemeColor(this, R.attr.colorSecondary));
            passwordText.setEnabled(false);
            passwordReminderSucceeded();
        }
        else {
            counter++;
            passwordLayout.setError(getString(R.string.test_pwd_wrong));
            passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error);
            passwordLayout.setErrorTextAppearance(R.style.TextAppearance_InputHint_Error);
            icon = ContextCompat.getDrawable(this, R.drawable.ic_input_warning);
            icon.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.red_600_red_300), PorterDuff.Mode.SRC_ATOP));
            passwordErrorImage.setImageDrawable(icon);
            testPasswordbackupRecoveryKeyButton.setTextColor(ContextCompat.getColor(this, R.color.red_600_red_300));
            if (counter == 3) {
                Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
                intent.putExtra("logout", isLogout());
                startActivity(intent);
                onBackPressed();
            }
        }

        passwordErrorImage.setVisibility(View.VISIBLE);
        confirmPasswordButton.setEnabled(false);
        confirmPasswordButton.setAlpha(0.3F);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK){
            logDebug("REQUEST_DOWNLOAD_FOLDER");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null){
                logDebug("parentPath no NULL");
                parentPath = parentPath + File.separator + getRecoveryKeyFileName();
                AccountController ac = new AccountController(this);
                ac.exportMK(parentPath);
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.password_reminder_checkbox: {
                if (blockCheckBox.isChecked()) {
                    logDebug("Block CheckBox checked!");
                }
                else {
                    logDebug("Block CheckBox does NOT checked!");
                }
                break;
            }
            case R.id.password_reminder_test_button: {
                shouldBlockPasswordReminder();
                testingPassword = true;
                setTestPasswordLayout();
                break;
            }
            case R.id.test_password_confirm_button:{
                String password = passwordText.getText().toString();
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
        logDebug("showSnackbar");
        showSnackbar(findViewById(R.id.container_layout), s);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    logDebug("REQUEST_WRITE_STORAGE PERMISSIONS GRANTED");
                }
                break;
            }
        }
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
                    logDebug("New value of attribute USER_ATTR_PWD_REMINDER: " + request.getText());
                    if (dismissPasswordReminder && isLogout() && numRequests <= 0) {
                        AccountController ac = new AccountController(this);
                        ac.logout(this, megaApi);
                    }
                }
                else {
                    logError("Error: MegaRequest.TYPE_SET_ATTR_USER | MegaApiJava.USER_ATTR_PWD_REMINDER " + e.getErrorString());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_LOGOUT) {
            logDebug("END logout sdk request - wait chat logout");
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
