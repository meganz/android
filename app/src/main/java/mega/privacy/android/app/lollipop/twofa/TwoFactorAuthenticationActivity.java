package mega.privacy.android.app.lollipop.twofa;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 28/05/18.
 */

public class TwoFactorAuthenticationActivity extends PinActivityLollipop implements View.OnClickListener{

    private Toolbar tB;
    private ActionBar aB;

    private ScrollView scrollContainer2FA;
    private RelativeLayout qrSeedContainer;
    private RelativeLayout confirmContainer;
    private Button setup2FAButton;

    InputMethodManager imm;
    private EditTextPIN firstPin;
    private EditTextPIN secondPin;
    private EditTextPIN thirdPin;
    private EditTextPIN fourthPin;
    private EditTextPIN fifthPin;
    private EditTextPIN sixthPin;
    private Button verifyButton;

    private boolean confirm2FAisShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_two_factor_authentication);

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.settings_2fa));
        setTitle(getString(R.string.settings_2fa));

        if (savedInstanceState != null){
            confirm2FAisShown = savedInstanceState.getBoolean("confirm2FAisShown", false);
        }
        else {
            confirm2FAisShown = false;
        }

        scrollContainer2FA = (ScrollView) findViewById(R.id.scroll_container_2fa);
        setup2FAButton = (Button) findViewById(R.id. button_enable_2fa);
        setup2FAButton.setOnClickListener(this);
        qrSeedContainer = (RelativeLayout) findViewById(R.id.container_qr_2fa);
        confirmContainer = (RelativeLayout) findViewById(R.id.container_confirm_2fa);

        if (confirm2FAisShown){
            scrollContainer2FA.setVisibility(View.GONE);
            qrSeedContainer.setVisibility(View.VISIBLE);
            confirmContainer.setVisibility(View.VISIBLE);
        }

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        firstPin = (EditTextPIN) findViewById(R.id.pass_first);
        imm.showSoftInput(firstPin, InputMethodManager.SHOW_FORCED);
        firstPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(firstPin.length() != 0){
                    secondPin.requestFocus();
                    secondPin.setCursorVisible(true);

                    secondPin.setText("");
                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        secondPin = (EditTextPIN) findViewById(R.id.pass_second);
        imm.showSoftInput(secondPin, InputMethodManager.SHOW_FORCED);
        secondPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (secondPin.length() != 0){
                    thirdPin.requestFocus();
                    thirdPin.setCursorVisible(true);

                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        thirdPin = (EditTextPIN) findViewById(R.id.pass_third);
        imm.showSoftInput(thirdPin, InputMethodManager.SHOW_FORCED);
        thirdPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (thirdPin.length()!= 0){
                    fourthPin.requestFocus();
                    fourthPin.setCursorVisible(true);

                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        fourthPin = (EditTextPIN) findViewById(R.id.pass_fourth);
        imm.showSoftInput(fourthPin, InputMethodManager.SHOW_FORCED);
        fourthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fourthPin.length()!=0){
                    fifthPin.requestFocus();
                    fifthPin.setCursorVisible(true);

                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        fifthPin = (EditTextPIN) findViewById(R.id.pass_fifth);
        imm.showSoftInput(fifthPin, InputMethodManager.SHOW_FORCED);
        fifthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fifthPin.length()!=0){
                    sixthPin.requestFocus();
                    sixthPin.setCursorVisible(true);

                    sixthPin.setText("");
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        sixthPin = (EditTextPIN) findViewById(R.id.pass_sixth);
        imm.showSoftInput(sixthPin, InputMethodManager.SHOW_FORCED);
        sixthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (sixthPin.length()!=0){
                    sixthPin.setCursorVisible(false);
                    verifyButton.setVisibility(View.VISIBLE);
                    hideKeyboard();
                }
                else {
                    verifyButton.setVisibility(View.GONE);
                }
            }
        });

        verifyButton = (Button) findViewById(R.id.button_verify_2fa);
        verifyButton.setOnClickListener(this);
    }

    void hideKeyboard(){

        View v = this.getCurrentFocus();
        if (v != null){
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("confirm2FAisShown", confirm2FAisShown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.button_enable_2fa: {
                confirm2FAisShown = true;
                scrollContainer2FA.setVisibility(View.GONE);
                qrSeedContainer.setVisibility(View.VISIBLE);
                confirmContainer.setVisibility(View.VISIBLE);
                break;
            }

            case R.id.button_verify_2fa: {

                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("TwoFactorAuthenticationActivity", message);
    }
}
