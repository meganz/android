package mega.privacy.android.app;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Util;

public class SMSVerificationActivity extends PinActivityLollipop implements View.OnClickListener {
    
    private TextView helperText, errorInvalidCountryCode, errorInvalidPhoneNumber, titleCountryCode, titlePhoneNumber;
    private ImageView errorInvalidPhoneNumberIcon;
    private RelativeLayout countrySelector;
    private EditText phoneNumberInput;
    private Button nextButton;
    private boolean isSelectedCountryValid;
    private boolean isPhoneNumberValid;
    private String selectedCountryCode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("SMSVerificationActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);
        
        //titles
        titleCountryCode = findViewById(R.id.verify_account_country_label);
        titlePhoneNumber = findViewById(R.id.verify_account_phone_number_label);
        
        //set helper text
        helperText = findViewById(R.id.verify_account_helper);
        String text;
        //todo detect use case and set different text
        if (true) {
            text = getResources().getString(R.string.verify_account_helper_add_new);
        } else {
            text = getResources().getString(R.string.verify_account_helper_locked);
        }
        
        //learn more
        int start = text.length();
        text += getResources().getString(R.string.verify_account_helper_learn_more);
        int end = text.length();
        SpannableString spanString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Log.d("click","Yuan ");
                
            }
            
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        spanString.setSpan(clickableSpan,start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        helperText.setText(spanString);
        helperText.setMovementMethod(LinkMovementMethod.getInstance());
        helperText.setHighlightColor(Color.TRANSPARENT);
        
        //country selector
        countrySelector = findViewById(R.id.verify_account_country_selector);
        countrySelector.setOnClickListener(this);
        
        //phone number input
        phoneNumberInput = findViewById(R.id.verify_account_phone_number_input);
        phoneNumberInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v,int actionId,KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    nextButtonClicked();
                    return true;
                }
                return false;
            }
        });
        phoneNumberInput.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
        phoneNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                log("onTextChanged");
                errorInvalidPhoneNumber.setVisibility(View.GONE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                log("afterTextChanged " + input);
                int inputLength = input == null ? 0 : input.length();
                if (inputLength > 0) {
                    titlePhoneNumber.setTextColor(Color.parseColor("#FF00BFA5"));
                    titlePhoneNumber.setVisibility(View.VISIBLE);
                } else {
                    titlePhoneNumber.setVisibility(View.GONE);
                }
            }
        });
        
        //next button
        nextButton = findViewById(R.id.verify_account_next_button);
        nextButton.setOnClickListener(this);
        
        //error message and icon
        errorInvalidCountryCode = findViewById(R.id.verify_account_invalid_country_code);
        errorInvalidPhoneNumber = findViewById(R.id.verify_account_invalid_phone_number);
        errorInvalidPhoneNumberIcon = findViewById(R.id.verify_account_invalid_phone_number_icon);
    }
    
    @Override
    public void onBackPressed() {
        log("onBackPressed");
        //todo finish activity as per scenario
        if (true) {
            finish();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onClick(View v) {
        //todo
        switch (v.getId()) {
            case (R.id.verify_account_country_selector): {
                log("verify_account_country_selector clicked");
                //todo launch list
                selectedCountryCode = "23423";
                break;
            }
            case (R.id.verify_account_next_button): {
                log("verify_account_next_button clicked");
                nextButtonClicked();
                break;
            }
            default:
                break;
        }
    }
    
    private void nextButtonClicked() {
        log("nextButtonClicked");
        Util.hideKeyboard(this);
        hideError();
        validateFields();
        resetTitles();
        if (isPhoneNumberValid && isSelectedCountryValid) {
            hideError();
            RequestTxt();
        } else {
            showError();
        }
    }
    
    private void resetTitles() {
    
    }
    
    private void validateFields() {
        //validate phone number
        String input = phoneNumberInput.getText().toString();
        int phoneNumberMinLength = 5;
        int phoneNumberMaxLength = 20;
        int inputLength = input == null ? 0 : input.length();
        if (inputLength >= phoneNumberMinLength && inputLength <= phoneNumberMaxLength) {
            isPhoneNumberValid = true;
        } else {
            isPhoneNumberValid = false;
        }
        
        if (selectedCountryCode != null && selectedCountryCode.length() >= 3) {
            isSelectedCountryValid = true;
        } else {
            isSelectedCountryValid = false;
        }
        
        log("validateFields isSelectedCountryValid " + isSelectedCountryValid + " isPhoneNumberValid " + isPhoneNumberValid);
    }
    
    private void showError() {
        if (!isSelectedCountryValid) {
            log("show invalid country error");
            errorInvalidCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FFFF333A"));
        }
        
        if (!isPhoneNumberValid) {
            log("show invalid phone number error");
            errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
            errorInvalidPhoneNumberIcon.setVisibility(View.VISIBLE);
            titlePhoneNumber.setVisibility(View.VISIBLE);
            titlePhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
        }
        
    }
    
    private void hideError() {
        log("hide Errors");
        errorInvalidCountryCode.setVisibility(View.GONE);
        errorInvalidPhoneNumber.setVisibility(View.GONE);
        errorInvalidPhoneNumberIcon.setVisibility(View.GONE);
        titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
        titlePhoneNumber.setTextColor(Color.parseColor("#FF00BFA5"));
    }
    
    private void RequestTxt() {
        //todo request txt and launch next activity
        log(" RequestTxt ");
    }
    
    public static void log(String message) {
        //Util.log("SMSVerificationActivity",message);
        Log.d("click","yuan " + message);
    }
}
