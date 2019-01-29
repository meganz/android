package mega.privacy.android.app;

import android.content.Intent;
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

import mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_CODE;
import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_NAME;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_VERIFY_CODE;

public class SMSVerificationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {
    
    public static final String SELECTED_COUNTRY_CODE = "COUNTRY_CODE";
    public static final String ENTERED_PHONE_NUMBER = "ENTERED_PHONE_NUMBER";
    private TextView helperText, selectedCountry,errorInvalidCountryCode, errorInvalidPhoneNumber, titleCountryCode, titlePhoneNumber;
    private View divider1, divider2;
    private ImageView errorInvalidPhoneNumberIcon;
    private RelativeLayout countrySelector;
    private EditText phoneNumberInput;
    private Button nextButton;
    private boolean isSelectedCountryValid;
    private boolean isPhoneNumberValid;
    private String selectedCountryCode;
    private String selectedCountryName;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("SMSVerificationActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);
        
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
                //todo
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
            selectedCountryCode = data.getStringExtra(COUNTRY_CODE);
            selectedCountryName = data.getStringExtra(COUNTRY_NAME);
            
            String label = selectedCountryName + " (" + selectedCountryCode + ")";
            selectedCountry.setText(label);
            errorInvalidCountryCode.setVisibility(View.GONE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
            selectedCountry.setTextColor(Color.parseColor("#DE000000"));
            divider1.setBackgroundColor(Color.parseColor("#8A000000"));
        }else if(requestCode == Constants.REQUEST_CODE_VERIFY_CODE && resultCode == RESULT_OK){
            finish();
        }
    }
    
    private void launchCountryPicker(){
        startActivityForResult(new Intent(getApplicationContext(),CountryCodePickerActivityLollipop.class), Constants.REQUEST_CODE_COUNTRY_PICKER);
    }
    
    private void nextButtonClicked() {
        log("nextButtonClicked");
        Util.hideKeyboard(this);
        hideError();
        validateFields();
        if (isPhoneNumberValid && isSelectedCountryValid) {
            hideError();
            RequestTxt();
        } else {
            showError();
        }
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
            divider1.setBackgroundColor(Color.parseColor("#FFFF333A"));
        }
        
        if (!isPhoneNumberValid) {
            log("show invalid phone number error");
            errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
            errorInvalidPhoneNumberIcon.setVisibility(View.VISIBLE);
            titlePhoneNumber.setVisibility(View.VISIBLE);
            titlePhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
            divider2.setBackgroundColor(Color.parseColor("#FFFF333A"));
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
        //todo request txt and launch next activity
        String phoneNumber = "+64210404525";//selectedCountryCode + phoneNumberInput.getText().toString();
        log(" RequestTxt phone number is " + phoneNumber);
        megaApi.sendSMSVerificationCode(phoneNumber,this,true);
    }
    
    public static void log(String message) {
        //Util.log("SMSVerificationActivity",message);
        Log.d("click","yuan " + message);
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        if(request.getType() == MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE) {
            log("send phone number,get code");
            if(e.getErrorCode() == MegaError.API_ETEMPUNAVAIL) {
                log("reached limitation.");
                showError();
            }
            if(e.getErrorCode() == MegaError.API_OK) {
                log("will receive sms");
                String enteredPhoneNumber = phoneNumberInput.getText().toString();
                Intent intent = new Intent(this, SMSVerificationReceiveTxtActivity.class);
                intent.putExtra(SELECTED_COUNTRY_CODE,selectedCountryCode);
                intent.putExtra(ENTERED_PHONE_NUMBER, enteredPhoneNumber);
                startActivityForResult(intent,REQUEST_CODE_VERIFY_CODE);
            }
            if(e.getErrorCode() == MegaError.API_EARGS) {
                log("wrong number");
                showError();
            }
            if(e.getErrorCode() == MegaError.API_EACCESS) {
                log("API_EACCESS");
                showError();
            }
        }
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    
    }
}
