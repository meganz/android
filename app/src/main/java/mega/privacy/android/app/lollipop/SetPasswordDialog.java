package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class SetPasswordDialog extends AlertDialog implements View.OnClickListener {

    private AlertDialog mDialog;
    private TextInputLayout userPasswordLayout;
    private AppCompatEditText userPassword;
    private ImageView userPasswordError;
    private TextInputLayout userPasswordConfimrLayout;
    private AppCompatEditText userPasswordConfirm;
    private ImageView userPasswordConfirmError;
    private MegaApiAndroid megaApi;
    private ImageView firstShape;
    private ImageView secondShape;
    private ImageView thirdShape;
    private ImageView fourthShape;
    private ImageView fifthShape;
    private LinearLayout containerPasswordElements;
    private TextView passwordType;
    private TextView passwordAdvice;
    private boolean isPasswordValid;
    private SetPasswordCallback mCallback;
    private Context mContext;
    private Builder builder;

    interface SetPasswordCallback {
        void onConfirmed(String password);
        void onCanceled();
    }

    protected SetPasswordDialog(@NonNull Context context, @NonNull SetPasswordCallback callback, MegaApiAndroid api) {
        super(context);
        mContext = context;
        builder = new Builder(context);
        mCallback = callback;
        megaApi = api;
        setupView();
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void show() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void setupView() {
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_set_password_link, null);
        builder.setTitle(mContext.getString(R.string.set_password_protection_dialog));
        isPasswordValid = false;
        final Button confirmButton = v.findViewById(R.id.button_confirm_password);
        confirmButton.setOnClickListener(this);
        Button cancelButton = v.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        containerPasswordElements = v.findViewById(R.id.container_passwd_elements);
        containerPasswordElements.setVisibility(View.GONE);
        userPasswordLayout = v.findViewById(R.id.password_layout);
        userPassword = v.findViewById(R.id.password_text);
        userPasswordError = v.findViewById(R.id.password_error_icon);
        userPasswordConfimrLayout = v.findViewById(R.id.confirm_password_layout);
        userPasswordConfirm = v.findViewById(R.id.confirm_password_text);
        userPasswordConfirmError = v.findViewById(R.id.confirm_password_error_icon);
        userPassword.getBackground().clearColorFilter();
        userPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logDebug("onTextChanged: " + s.toString() + ":" + start + ":" + before + ":" + count);
                if (s.length() > 0) {
                    String temp = s.toString();
                    containerPasswordElements.setVisibility(View.VISIBLE);
                    checkPasswordStrength(temp.trim());
                } else {
                    isPasswordValid = false;
                    containerPasswordElements.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userPassword);
            }
        });

        userPassword.setOnFocusChangeListener((v1, hasFocus) -> setPasswordToggle(userPasswordLayout, hasFocus));

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

        userPasswordConfirm.setOnFocusChangeListener((v12, hasFocus) -> setPasswordToggle(userPasswordConfimrLayout, hasFocus));

        userPasswordConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setButtonClicked();
                }
                return false;
            }
        });

        firstShape = v.findViewById(R.id.shape_passwd_first);
        secondShape = v.findViewById(R.id.shape_passwd_second);
        thirdShape = v.findViewById(R.id.shape_passwd_third);
        fourthShape = v.findViewById(R.id.shape_passwd_fourth);
        fifthShape = v.findViewById(R.id.shape_passwd_fifth);
        passwordType = v.findViewById(R.id.password_type);
        passwordAdvice = v.findViewById(R.id.password_advice_text);

        builder.setView(v);
    }

    private void checkPasswordStrength(String s) {

        int passwordStrength = megaApi.getPasswordStrength(s);
        Drawable veryWeak = ContextCompat.getDrawable(mContext, R.drawable.passwd_very_weak);
        Drawable weak = ContextCompat.getDrawable(mContext, R.drawable.passwd_weak);
        Drawable medium = ContextCompat.getDrawable(mContext, R.drawable.passwd_medium);
        Drawable good = ContextCompat.getDrawable(mContext, R.drawable.passwd_good);
        Drawable strong = ContextCompat.getDrawable(mContext, R.drawable.passwd_strong);
        Drawable shape = ContextCompat.getDrawable(mContext, R.drawable.shape_password);
        
        if (passwordStrength == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK) {
            firstShape.setBackground(veryWeak);
            secondShape.setBackground(shape);
            thirdShape.setBackground(shape);
            fourthShape.setBackground(shape);
            fifthShape.setBackground(shape);
            passwordType.setText(mContext.getString(R.string.pass_very_weak));
            passwordType.setTextColor(ContextCompat.getColor(mContext, R.color.login_warning));
            passwordAdvice.setText(mContext.getString(R.string.passwd_weak));
            isPasswordValid = false;
        } else if (passwordStrength == MegaApiJava.PASSWORD_STRENGTH_WEAK) {
            firstShape.setBackground(weak);
            secondShape.setBackground(weak);
            thirdShape.setBackground(shape);
            fourthShape.setBackground(shape);
            fifthShape.setBackground(shape);
            passwordType.setText(mContext.getString(R.string.pass_weak));
            passwordType.setTextColor(ContextCompat.getColor(mContext, R.color.pass_weak));
            passwordAdvice.setText(mContext.getString(R.string.passwd_weak));
            isPasswordValid = true;
        } else if (passwordStrength == MegaApiJava.PASSWORD_STRENGTH_MEDIUM) {
            firstShape.setBackground(medium);
            secondShape.setBackground(medium);
            thirdShape.setBackground(medium);
            fourthShape.setBackground(shape);
            fifthShape.setBackground(shape);
            passwordType.setText(mContext.getString(R.string.pass_medium));
            passwordType.setTextColor(ContextCompat.getColor(mContext, R.color.green_unlocked_rewards));
            passwordAdvice.setText(mContext.getString(R.string.passwd_medium));
            isPasswordValid = true;
        } else if (passwordStrength == MegaApiJava.PASSWORD_STRENGTH_GOOD) {
            firstShape.setBackground(good);
            secondShape.setBackground(good);
            thirdShape.setBackground(good);
            fourthShape.setBackground(good);
            fifthShape.setBackground(shape);
            passwordType.setText(mContext.getString(R.string.pass_good));
            passwordType.setTextColor(ContextCompat.getColor(mContext, R.color.pass_good));
            passwordAdvice.setText(mContext.getString(R.string.passwd_good));
            isPasswordValid = true;
        } else {
            firstShape.setBackground(strong);
            secondShape.setBackground(strong);
            thirdShape.setBackground(strong);
            fourthShape.setBackground(strong);
            fifthShape.setBackground(strong);
            passwordType.setText(mContext.getString(R.string.pass_strong));
            passwordType.setTextColor(ContextCompat.getColor(mContext, R.color.blue_unlocked_rewards));
            passwordAdvice.setText(mContext.getString(R.string.passwd_strong));
            isPasswordValid = true;
        }
    }

    private void quitError(AppCompatEditText editText) {
        switch (editText.getId()) {
            case R.id.password_text:
                userPasswordLayout.setError(null);
                userPasswordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userPasswordError.setVisibility(View.GONE);
                break;

            case R.id.confirm_password_text:
                userPasswordConfimrLayout.setError(null);
                userPasswordConfimrLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                userPasswordConfirmError.setVisibility(View.GONE);
                break;
        }
    }

    private boolean validateForm() {
        String passwordError = getPasswordError();
        String passwordConfirmError = getPasswordConfirmError();

        // Set or remove errors
        setError(userPassword, passwordError);
        setError(userPasswordConfirm, passwordConfirmError);

        // Return false on any error or true on success
        if (passwordError != null) {
            userPassword.requestFocus();
            return false;
        } else if (passwordConfirmError != null) {
            userPasswordConfirm.requestFocus();
            return false;
        }
        return true;
    }

    private void setError(final AppCompatEditText editText, String error) {
        if (error == null || error.equals("")) {
            return;
        }

        switch (editText.getId()) {
            case R.id.password_text:
                userPasswordLayout.setError(error);
                userPasswordLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userPasswordError.setVisibility(View.VISIBLE);
                break;

            case R.id.confirm_password_text:
                userPasswordConfimrLayout.setError(error);
                userPasswordConfimrLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                userPasswordConfirmError.setVisibility(View.VISIBLE);
                break;
        }
    }

    private String getPasswordError() {
        String value = userPassword.getText().toString();
        if (value.trim().isEmpty()) {
            return mContext.getString(R.string.error_enter_password);
        } else if (!isPasswordValid) {
            containerPasswordElements.setVisibility(View.GONE);
            return mContext.getString(R.string.error_password);
        }
        return null;
    }

    private String getPasswordConfirmError() {
        String password = userPassword.getText().toString();
        String confirm = userPasswordConfirm.getText().toString();

        if (confirm.trim().isEmpty()) {
            return mContext.getString(R.string.error_enter_password);
        } else if (!password.equals(confirm)) {
            return mContext.getString(R.string.error_passwords_dont_match);
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_confirm_password:
                setButtonClicked();
                break;

            case R.id.button_cancel:
                dismiss();
                if (mCallback != null) {
                    mCallback.onCanceled();
                }
                break;
        }
    }

    private void setButtonClicked(){
        if (validateForm()) {
            dismiss();
            if (mCallback != null) {
                mCallback.onConfirmed(userPassword.getText().toString());
            }
        }
    }
}
