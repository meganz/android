package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;

public class SetPasswordDialog extends AlertDialog implements View.OnClickListener {

    private AlertDialog mDialog;
    private EditText userPassword, userPasswordConfirm;
    private MegaApiAndroid megaApi;
    private RelativeLayout passwordConfirmErrorLayout, passwordErrorLayout;
    private Drawable passwordConfirmBackground, passwordBackground;
    private ImageView toggleButtonPassword, toggleButtonConfirmPassword, firstShape, secondShape, thirdShape, fourthShape, fifthShape;
    private LinearLayout containerPasswordElements;
    private TextView passwordType, passwordAdvice, passwordConfirmErrorText, passwordErrorText, passwordTitle, confirmPasswordTitle;
    private boolean isPasswordValid, isPasswordVisible;
    private SetPasswordCallback mCallback;
    private Context mContext;
    private Builder builder;
    private int accentColor;
    private Drawable showPassword, hidePassword;

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
        accentColor = ContextCompat.getColor(mContext, R.color.accentColor);
        showPassword = ContextCompat.getDrawable(mContext, R.drawable.ic_b_see);
        hidePassword = ContextCompat.getDrawable(mContext, R.drawable.ic_b_shared_read);
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
        isPasswordVisible = false;
        isPasswordValid = false;
        final Button confirmButton = v.findViewById(R.id.button_confirm_password);
        confirmButton.setOnClickListener(this);
        Button cancelButton = v.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        passwordTitle = v.findViewById(R.id.lbl_password);
        confirmPasswordTitle = v.findViewById(R.id.lbl_confirm_password);
        passwordErrorText = v.findViewById(R.id.create_account_password_error_text);
        passwordConfirmErrorText = v.findViewById(R.id.create_account_password_confirm_error_text);
        containerPasswordElements = v.findViewById(R.id.container_passwd_elements);
        containerPasswordElements.setVisibility(View.GONE);
        userPassword = v.findViewById(R.id.create_account_password_text);
        userPasswordConfirm = v.findViewById(R.id.create_account_password_text_confirm);
        toggleButtonPassword = v.findViewById(R.id.toggle_button_passwd);
        toggleButtonPassword.setOnClickListener(this);
        toggleButtonPassword.setVisibility(View.GONE);
        toggleButtonConfirmPassword = v.findViewById(R.id.toggle_button_confirm_passwd);
        toggleButtonConfirmPassword.setOnClickListener(this);
        toggleButtonConfirmPassword.setVisibility(View.GONE);
        userPassword.getBackground().clearColorFilter();
        userPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log("onTextChanged: " + s.toString() + ":" + start + ":" + before + ":" + count);
                if (s.length() > 0) {
                    String temp = s.toString();
                    containerPasswordElements.setVisibility(View.VISIBLE);
                    passwordTitle.setVisibility(View.VISIBLE);
                    checkPasswordStrength(temp.trim());
                } else {
                    isPasswordValid = false;
                    containerPasswordElements.setVisibility(View.GONE);
                    passwordTitle.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userPassword);
                passwordTitle.setTextColor(accentColor);
            }
        });

        userPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Util.showKeyboardDelayed(v);
                    toggleButtonPassword.setVisibility(View.VISIBLE);
                    toggleButtonPassword.setImageDrawable(hidePassword);
                } else {
                    toggleButtonPassword.setVisibility(View.GONE);
                    isPasswordVisible = false;
                    showHidePassword(false);
                }
            }
        });

        passwordBackground = userPassword.getBackground().mutate().getConstantState().newDrawable();
        userPasswordConfirm.getBackground().clearColorFilter();
        userPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    confirmPasswordTitle.setVisibility(View.VISIBLE);
                } else {
                    confirmPasswordTitle.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(userPasswordConfirm);
                confirmPasswordTitle.setTextColor(accentColor);
            }
        });

        userPasswordConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    toggleButtonConfirmPassword.setVisibility(View.VISIBLE);
                    toggleButtonConfirmPassword.setImageDrawable(hidePassword);
                } else {
                    toggleButtonConfirmPassword.setVisibility(View.GONE);
                    isPasswordVisible = false;
                    showHidePassword(true);
                }
            }
        });

        userPasswordConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setButtonClicked();
                }
                return false;
            }
        });

        passwordConfirmBackground = userPasswordConfirm.getBackground().mutate().getConstantState().newDrawable();
        passwordConfirmErrorLayout = v.findViewById(R.id.create_account_password_confirm_error);
        passwordConfirmErrorLayout.setVisibility(View.GONE);
        passwordErrorLayout = v.findViewById(R.id.create_account_password_error);
        passwordErrorLayout.setVisibility(View.GONE);

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

    private void showHidePassword(boolean isConfirmPassword) {
        if (isPasswordVisible) {
            if (isConfirmPassword) {
                userPasswordConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                userPasswordConfirm.setSelection(userPasswordConfirm.getText().length());
            } else {
                userPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                userPassword.setSelection(userPassword.getText().length());
            }
        } else {
            if (isConfirmPassword) {
                userPasswordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                userPasswordConfirm.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                userPasswordConfirm.setSelection(userPasswordConfirm.getText().length());
            } else {
                userPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                userPassword.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                userPassword.setSelection(userPassword.getText().length());
            }
        }
    }

    private void quitError(EditText editText) {
        switch (editText.getId()) {
            case R.id.create_account_password_text_confirm: {
                if (passwordConfirmErrorLayout.getVisibility() != View.GONE) {
                    passwordConfirmErrorLayout.setVisibility(View.GONE);
                    userPasswordConfirm.setBackground(passwordConfirmBackground);
                }
            }
            break;
            case R.id.create_account_password_text: {
                if (passwordErrorLayout.getVisibility() != View.GONE) {
                    passwordErrorLayout.setVisibility(View.GONE);
                    userPassword.setBackground(passwordBackground);
                }
            }
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

    private void setError(final EditText editText, String error) {
        if (error == null || error.equals("")) {
            return;
        }
        int errorColor = ContextCompat.getColor(mContext, R.color.login_warning);
        PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(errorColor, PorterDuff.Mode.SRC_ATOP);
        Drawable background;
        switch (editText.getId()) {
            case R.id.create_account_password_text_confirm:
                passwordConfirmErrorLayout.setVisibility(View.VISIBLE);
                passwordConfirmErrorText.setText(error);
                background = passwordConfirmBackground.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userPasswordConfirm.setBackground(background);
                confirmPasswordTitle.setTextColor(errorColor);
                break;
            case R.id.create_account_password_text:
                passwordErrorLayout.setVisibility(View.VISIBLE);
                passwordErrorText.setText(error);
                background = passwordBackground.mutate().getConstantState().newDrawable();
                background.setColorFilter(porterDuffColorFilter);
                userPassword.setBackground(background);
                passwordTitle.setTextColor(errorColor);
                break;
        }
    }

    private String getPasswordError() {
        String value = userPassword.getText().toString();
        if (value.length() == 0) {
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
        if (!password.equals(confirm)) {
            return mContext.getString(R.string.error_passwords_dont_match);
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toggle_button_passwd:
                if (isPasswordVisible) {
                    toggleButtonPassword.setImageDrawable(hidePassword);
                    isPasswordVisible = false;
                } else {
                    toggleButtonPassword.setImageDrawable(showPassword);
                    isPasswordVisible = true;
                }
                showHidePassword(false);
                break;
            case R.id.toggle_button_confirm_passwd:
                if (isPasswordVisible) {
                    toggleButtonConfirmPassword.setImageDrawable(hidePassword);
                    isPasswordVisible = false;
                } else {
                    toggleButtonConfirmPassword.setImageDrawable(showPassword);
                    isPasswordVisible = true;
                }
                showHidePassword(true);
                break;
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

    private void log(String log) {
        Util.log("SetPasswordDialog", log);
    }
}
