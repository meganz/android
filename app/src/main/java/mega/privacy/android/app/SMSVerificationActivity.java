package mega.privacy.android.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TL;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_CODE;
import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_NAME;
import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.DIAL_CODE;
import static mega.privacy.android.app.lollipop.LoginFragmentLollipop.NAME_USER_LOCKED;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_VERIFY_CODE;

public class SMSVerificationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {
    
    public static final String SELECTED_COUNTRY_CODE = "COUNTRY_CODE";
    public static final String ENTERED_PHONE_NUMBER = "ENTERED_PHONE_NUMBER";
    private TextView helperText, selectedCountry, errorInvalidCountryCode, errorInvalidPhoneNumber, titleCountryCode, titlePhoneNumber;
    private View divider1, divider2;
    private ImageView errorInvalidPhoneNumberIcon;
    private RelativeLayout countrySelector;
    private EditText phoneNumberInput;
    private Button nextButton;
    private boolean isSelectedCountryValid, isPhoneNumberValid, isUserLocked, shouldDisableNextButton;
    private String selectedCountryCode, selectedCountryName, selectedDialCode;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("SMSVerificationActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);
        Intent intent = getIntent();
        if (intent != null) {
            isUserLocked = intent.getBooleanExtra(NAME_USER_LOCKED,false);
        }
        log("is user locked " + isUserLocked);
        
        //divider
        divider1 = findViewById(R.id.verify_account_divider1);
        divider2 = findViewById(R.id.verify_account_divider2);
        
        //titles
        titleCountryCode = findViewById(R.id.verify_account_country_label);
        titlePhoneNumber = findViewById(R.id.verify_account_phone_number_label);
        selectedCountry = findViewById(R.id.verify_account_selected_country);
        
        //set helper text
        helperText = findViewById(R.id.verify_account_helper);
        String text;
        if (isUserLocked) {
            text = getResources().getString(R.string.verify_account_helper_locked);
        } else {
            text = getResources().getString(R.string.verify_account_helper_add_new);
        }
        
        //learn more
        int start = text.length();
        text += getResources().getString(R.string.verify_account_helper_learn_more);
        int end = text.length();
        SpannableString spanString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                //todo open external link
                log("Learn more clicked");
            }
            
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(getResources().getColor(R.color.accentColor));
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
                errorInvalidPhoneNumberIcon.setVisibility(View.GONE);
                divider2.setBackgroundColor(Color.parseColor("#8A000000"));
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
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.verify_account_country_selector): {
                log("verify_account_country_selector clicked");
                launchCountryPicker();
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
    
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == Constants.REQUEST_CODE_COUNTRY_PICKER && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CODE_COUNTRY_PICKER OK");
            selectedCountryCode = data.getStringExtra(COUNTRY_CODE);
            selectedCountryName = data.getStringExtra(COUNTRY_NAME);
            selectedDialCode = data.getStringExtra(DIAL_CODE);
            
            String label = selectedCountryName + " (" + selectedDialCode + ")";
            selectedCountry.setText(label);
            errorInvalidCountryCode.setVisibility(View.GONE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
            selectedCountry.setTextColor(Color.parseColor("#DE000000"));
            divider1.setBackgroundColor(Color.parseColor("#8A000000"));
        } else if (requestCode == Constants.REQUEST_CODE_VERIFY_CODE && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CODE_VERIFY_CODE OK");
            setResult(RESULT_OK);
            finish();
        }
    }
    
    private void launchCountryPicker() {
        log("launchCountryPicker");
        startActivityForResult(new Intent(getApplicationContext(),CountryCodePickerActivityLollipop.class),Constants.REQUEST_CODE_COUNTRY_PICKER);
    }
    
    private void nextButtonClicked() {
        log("nextButtonClicked");
        Util.hideKeyboard(this);
        hideError();
        validateFields();
        if (isPhoneNumberValid && isSelectedCountryValid) {
            log("nextButtonClicked no error");
            hideError();
            RequestTxt();
        } else {
            showCountryCodeValidationError();
            showPhoneNumberValidationError(null);
        }
    }
    
    private void validateFields() {
        log("validateFields");
        //validate phone number
        String phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumberInput.getText().toString(),selectedCountryCode);
        if(phoneNumber != null){
            isPhoneNumberValid = true;
        }else{
            isPhoneNumberValid = false;
        }
        
        if (selectedDialCode != null && selectedDialCode.length() >= 3) {
            isSelectedCountryValid = true;
        } else {
            isSelectedCountryValid = false;
        }
        
        log("validateFields isSelectedCountryValid " + isSelectedCountryValid + " isPhoneNumberValid " + isPhoneNumberValid);
    }
    
    private void showCountryCodeValidationError() {
        if (!isSelectedCountryValid) {
            log("show invalid country error");
            errorInvalidCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FFFF333A"));
            divider1.setBackgroundColor(Color.parseColor("#FFFF333A"));
        }
    }
    
    private void showPhoneNumberValidationError(String errorMessage) {
        if (!isPhoneNumberValid) {
            log("show invalid phone number error");
            errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
            errorInvalidPhoneNumberIcon.setVisibility(View.VISIBLE);
            titlePhoneNumber.setVisibility(View.VISIBLE);
            titlePhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
            divider2.setBackgroundColor(Color.parseColor("#FFFF333A"));
            if (errorMessage != null) {
                errorInvalidPhoneNumber.setText(errorMessage);
            }
        }
    }
    
    private void hideError() {
        log("hide Errors");
        errorInvalidCountryCode.setVisibility(View.GONE);
        errorInvalidPhoneNumber.setVisibility(View.GONE);
        errorInvalidPhoneNumberIcon.setVisibility(View.GONE);
        titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
        titlePhoneNumber.setTextColor(Color.parseColor("#FF00BFA5"));
        divider1.setBackgroundColor(Color.parseColor("#8A000000"));
        divider2.setBackgroundColor(Color.parseColor("#8A000000"));
    }
    
    private void RequestTxt() {
        log("RequestTxt shouldDisableNextButton is " + shouldDisableNextButton);
        if(!shouldDisableNextButton){
            String phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumberInput.getText().toString(),selectedCountryCode);
            log(" RequestTxt phone number is " + phoneNumber);
            shouldDisableNextButton = true;
            nextButton.setTextColor(Color.RED);
            megaApi.sendSMSVerificationCode(phoneNumber,this,true);
        }
    }
    
    public static void log(String message) {
        //Util.log("SMSVerificationActivity",message);
        TL.log("SmsVerificationActivity","@#@",message);
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        shouldDisableNextButton = false;
        nextButton.setTextColor(Color.WHITE);
        if (request.getType() == MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE) {
            log("send phone number,get code");
            if (e.getErrorCode() == MegaError.API_OK) {
                log("will receive sms");
                String enteredPhoneNumber = phoneNumberInput.getText().toString();
                Intent intent = new Intent(this,SMSVerificationReceiveTxtActivity.class);
                intent.putExtra(SELECTED_COUNTRY_CODE,selectedDialCode);
                intent.putExtra(ENTERED_PHONE_NUMBER,enteredPhoneNumber);
                intent.putExtra(NAME_USER_LOCKED,isUserLocked);
                startActivityForResult(intent,REQUEST_CODE_VERIFY_CODE);
            } else if (e.getErrorCode() == MegaError.API_ETEMPUNAVAIL) {
                log("reached limitation.");
                errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
                errorInvalidPhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
                errorInvalidPhoneNumber.setText(R.string.verify_account_error_reach_limit);
            } else if (e.getErrorCode() == MegaError.API_EARGS) {
                log("Invalid phone number");
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_invalid_phone_number);
                showPhoneNumberValidationError(errorMessage);
            } else if (e.getErrorCode() == MegaError.API_EACCESS) {
                log("Phone number has been registered");
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_error_phone_number_register);
                showPhoneNumberValidationError(errorMessage);
            } else {
                log("sms TYPE_SEND_SMS_VERIFICATIONCODE " + e.getErrorString());
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_invalid_phone_number);
                showPhoneNumberValidationError(errorMessage);
            }
        }
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    
    }
}
