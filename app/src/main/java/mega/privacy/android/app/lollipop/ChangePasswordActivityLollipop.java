package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


@SuppressLint("NewApi")
public class ChangePasswordActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface, View.OnFocusChangeListener, View.OnLongClickListener{
	
	ChangePasswordActivityLollipop changePasswordActivity = this;

	private ProgressDialog progress;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	boolean changePassword = true;
	
	private TextInputLayout newPassword1Layout;
	private AppCompatEditText newPassword1;
	private ImageView newPassword1Error;
	private TextInputLayout newPassword2Layout;
	private AppCompatEditText newPassword2;
	private ImageView newPassword2Error;
	private Button changePasswordButton;
    private RelativeLayout fragmentContainer;
	private TextView title;
	private String linkToReset;
	private String mk;

	// TOP for 'terms of password'
    private CheckBox chkTOP;

	private ActionBar aB;
	Toolbar tB;

	private LinearLayout containerPasswdElements;
	private ImageView firstShape;
	private ImageView secondShape;
	private ImageView tirdShape;
	private ImageView fourthShape;
	private ImageView fifthShape;
	private TextView passwdType;
	private TextView passwdAdvice;
	private boolean passwdValid;

	AlertDialog verify2FADialog;
	boolean verify2FADialogIsShown = false;
	InputMethodManager imm;
	private EditTextPIN firstPin;
	private EditTextPIN secondPin;
	private EditTextPIN thirdPin;
	private EditTextPIN fourthPin;
	private EditTextPIN fifthPin;
	private EditTextPIN sixthPin;
	private StringBuilder sb = new StringBuilder();
	private String pin = null;
	private TextView pinError;
	private ProgressBar verify2faProgressBar;
	private RelativeLayout lostYourDeviceButton;

	private boolean isFirstTime = true;
	private boolean isErrorShown = false;
	private boolean is2FAEnabled = false;
	private boolean pinLongClick = false;

	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_password);
		
        fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_change_pass);
		megaApi = ((MegaApplication)getApplication()).getMegaApi();

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = getScaleW(outMetrics, density);
	    scaleH = getScaleH(outMetrics, density);

		title = (TextView) findViewById(R.id.title_change_pass);

		passwdValid = false;

		containerPasswdElements = (LinearLayout) findViewById(R.id.container_passwd_elements);
		containerPasswdElements.setVisibility(View.GONE);
		firstShape = (ImageView) findViewById(R.id.shape_passwd_first);
		secondShape = (ImageView) findViewById(R.id.shape_passwd_second);
		tirdShape = (ImageView) findViewById(R.id.shape_passwd_third);
		fourthShape = (ImageView) findViewById(R.id.shape_passwd_fourth);
		fifthShape = (ImageView) findViewById(R.id.shape_passwd_fifth);
		passwdType = (TextView) findViewById(R.id.password_type);
		passwdAdvice = (TextView) findViewById(R.id.password_advice_text);

		newPassword1Layout = findViewById(R.id.change_password_newPassword1_layout);
		newPassword1 = findViewById(R.id.change_password_newPassword1);
		newPassword1Error = findViewById(R.id.change_password_newPassword1_error_icon);
		newPassword1Error.setVisibility(View.GONE);

		newPassword1.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				logDebug("Text changed: " + s.toString() + "_ " + start + "__" + before + "__" + count);
				if (s != null){
					if (s.length() > 0) {
						String temp = s.toString();
						containerPasswdElements.setVisibility(View.VISIBLE);

						checkPasswordStrength(temp.trim());
					}
					else{
						passwdValid = false;
						containerPasswdElements.setVisibility(View.GONE);
					}
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (editable.toString().isEmpty()) {
					quitError(newPassword1);
				}
			}
		});

		newPassword1.setOnFocusChangeListener((v, hasFocus) -> setPasswordToggle(newPassword1Layout, hasFocus));

		newPassword2Layout = findViewById(R.id.change_password_newPassword2_layout);
		newPassword2 = findViewById(R.id.change_password_newPassword2);
		newPassword2Error = findViewById(R.id.change_password_newPassword2_error_icon);
		newPassword2Error.setVisibility(View.GONE);

		newPassword2.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(newPassword2);
			}
		});

		newPassword2.setOnFocusChangeListener((v, hasFocus) -> setPasswordToggle(newPassword2Layout, hasFocus));

		changePasswordButton = (Button) findViewById(R.id.action_change_password);
		changePasswordButton.setOnClickListener(this);

        TextView top = findViewById(R.id.top);

        String textToShowTOP = getString(R.string.top);
        try {
            textToShowTOP = textToShowTOP.replace("[B]", "<font color=\'#00BFA5\'>")
                    .replace("[/B]", "</font>")
                    .replace("[A]", "<u>")
                    .replace("[/A]", "</u>");
        } catch (Exception e) {
            logError("Exception formatting string", e);
        }

        Spanned resultTOP;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultTOP = Html.fromHtml(textToShowTOP,Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultTOP = Html.fromHtml(textToShowTOP);
        }

        top.setText(resultTOP);

        top.setOnClickListener(this);

        chkTOP = findViewById(R.id.chk_top);
        chkTOP.setOnClickListener(this);
		
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.setCancelable(false);
		progress.setCanceledOnTouchOutside(false);

		tB  =(Toolbar) findViewById(R.id.toolbar);
		hideAB();

		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		if (savedInstanceState != null) {
			verify2FADialogIsShown =  savedInstanceState.getBoolean("verify2FADialogIsShown", verify2FADialogIsShown);
			if (verify2FADialogIsShown) {
				showVerifyPin2FA();
			}
		}

		Intent intentReceived = getIntent();
		if (intentReceived != null) {
			logDebug("There is an intent!");
			if(intentReceived.getAction()!=null){
				if (getIntent().getAction().equals(ACTION_RESET_PASS_FROM_LINK)) {
					logDebug("ACTION_RESET_PASS_FROM_LINK");
					changePassword=false;
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						logWarning("link is NULL - close activity");
						finish();
					}
					mk = getIntent().getStringExtra("MK");
					if(mk==null){
						logWarning("MK is NULL - close activity");
						showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					}

					title.setText(getString(R.string.title_enter_new_password));
				}
				if (getIntent().getAction().equals(ACTION_RESET_PASS_FROM_PARK_ACCOUNT)) {
					changePassword=false;
					logDebug("ACTION_RESET_PASS_FROM_PARK_ACCOUNT");
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						logWarning("link is NULL - close activity");
						showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					}
					mk = null;

					title.setText(getString(R.string.title_enter_new_password));
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");
		if (getIntent() != null && getIntent().getBooleanExtra("logout", false)) {
			Intent intent = new Intent(this, TestPasswordActivity.class);
			intent.putExtra("logout", getIntent().getBooleanExtra("logout", false));
			startActivity(intent);
		}
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean("verify2FADialogIsShown", verify2FADialogIsShown);
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		switch (v.getId()) {
			case R.id.pin_first_verify:{
				if (hasFocus) {
					firstPin.setText("");
				}
				break;
			}
			case R.id.pin_second_verify:{
				if (hasFocus) {
					secondPin.setText("");
				}
				break;
			}
			case R.id.pin_third_verify:{
				if (hasFocus) {
					thirdPin.setText("");
				}
				break;
			}
			case R.id.pin_fouth_verify:{
				if (hasFocus) {
					fourthPin.setText("");
				}
				break;
			}
			case R.id.pin_fifth_verify:{
				if (hasFocus) {
					fifthPin.setText("");
				}
				break;
			}
			case R.id.pin_sixth_verify:{
				if (hasFocus) {
					sixthPin.setText("");
				}
				break;
			}
		}
	}

	public void showVerifyPin2FA(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_verify_2fa, null);
		builder.setView(v);

		TextView titleDialog = (TextView) v.findViewById(R.id.title_dialog_verify);
		titleDialog.setText(getString(R.string.change_password_verification));

		pinError = (TextView) v.findViewById(R.id.pin_2fa_error_verify);
		pinError.setVisibility(View.GONE);

		lostYourDeviceButton = (RelativeLayout) v.findViewById(R.id.lost_authentication_device);
		lostYourDeviceButton.setOnClickListener(this);
		verify2faProgressBar = (ProgressBar) v.findViewById(R.id.progressbar_verify_2fa);

		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		firstPin = (EditTextPIN) v.findViewById(R.id.pin_first_verify);
		firstPin.setOnLongClickListener(this);
		firstPin.setOnFocusChangeListener(this);
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

					if (isFirstTime && !pinLongClick){
						secondPin.setText("");
						thirdPin.setText("");
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		secondPin = (EditTextPIN) v.findViewById(R.id.pin_second_verify);
		secondPin.setOnLongClickListener(this);
		secondPin.setOnFocusChangeListener(this);
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

					if (isFirstTime && !pinLongClick) {
						thirdPin.setText("");
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		thirdPin = (EditTextPIN) v.findViewById(R.id.pin_third_verify);
		thirdPin.setOnLongClickListener(this);
		thirdPin.setOnFocusChangeListener(this);
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

					if (isFirstTime && !pinLongClick) {
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		fourthPin = (EditTextPIN) v.findViewById(R.id.pin_fouth_verify);
		fourthPin.setOnLongClickListener(this);
		fourthPin.setOnFocusChangeListener(this);
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

					if (isFirstTime && !pinLongClick) {
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		fifthPin = (EditTextPIN) v.findViewById(R.id.pin_fifth_verify);
		fifthPin.setOnLongClickListener(this);
		fifthPin.setOnFocusChangeListener(this);
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

					if (isFirstTime && !pinLongClick) {
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		sixthPin = (EditTextPIN) v.findViewById(R.id.pin_sixth_verify);
		sixthPin.setOnLongClickListener(this);
		sixthPin.setOnFocusChangeListener(this);
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
					sixthPin.setCursorVisible(true);
					hideKeyboard(changePasswordActivity, 0);

					if (pinLongClick) {
						pasteClipboard();
					}
					else {
						permitVerify();
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		firstPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = firstPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb1.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb1.width = scaleWidthPx(25, outMetrics);
		}
		firstPin.setLayoutParams(paramsb1);
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)firstPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		firstPin.setLayoutParams(textParams);

		secondPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = secondPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb2.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb2.width = scaleWidthPx(25, outMetrics);
		}
		secondPin.setLayoutParams(paramsb2);
		textParams = (LinearLayout.LayoutParams)secondPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		secondPin.setLayoutParams(textParams);
		secondPin.setEt(firstPin);

		thirdPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = thirdPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb3.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb3.width = scaleWidthPx(25, outMetrics);
		}
		thirdPin.setLayoutParams(paramsb3);
		textParams = (LinearLayout.LayoutParams)thirdPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(25, outMetrics), 0);
		thirdPin.setLayoutParams(textParams);
		thirdPin.setEt(secondPin);

		fourthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = fourthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb4.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb4.width = scaleWidthPx(25, outMetrics);
		}
		fourthPin.setLayoutParams(paramsb4);
		textParams = (LinearLayout.LayoutParams)fourthPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		fourthPin.setLayoutParams(textParams);
		fourthPin.setEt(thirdPin);

		fifthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb5 = fifthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb5.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb5.width = scaleWidthPx(25, outMetrics);
		}
		fifthPin.setLayoutParams(paramsb5);
		textParams = (LinearLayout.LayoutParams)fifthPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		fifthPin.setLayoutParams(textParams);
		fifthPin.setEt(fourthPin);

		sixthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb6 = sixthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb6.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb6.width = scaleWidthPx(25, outMetrics);
		}
		sixthPin.setLayoutParams(paramsb6);
		textParams = (LinearLayout.LayoutParams)sixthPin.getLayoutParams();
		textParams.setMargins(0, 0, 0, 0);
		sixthPin.setLayoutParams(textParams);
		sixthPin.setEt(fifthPin);

		verify2FADialog = builder.create();
		verify2FADialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				verify2FADialogIsShown = false;
			}
		});
		verify2FADialog.show();
		verify2FADialogIsShown = true;
		imm.showSoftInput(this.getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
	}

	void verifyQuitError(){
		isErrorShown = false;
		pinError.setVisibility(View.GONE);
		firstPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
		secondPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
		thirdPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
		fourthPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
		fifthPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
		sixthPin.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
	}

	void verifyShowError(){
		isFirstTime = false;
		isErrorShown = true;
		pinError.setVisibility(View.VISIBLE);
		firstPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		secondPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		thirdPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		fourthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		fifthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		sixthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
	}

	void permitVerify(){
		logDebug("permitVerify");
		if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
			hideKeyboard(changePasswordActivity, 0);
			if (sb.length()>0) {
				sb.delete(0, sb.length());
			}
			sb.append(firstPin.getText());
			sb.append(secondPin.getText());
			sb.append(thirdPin.getText());
			sb.append(fourthPin.getText());
			sb.append(fifthPin.getText());
			sb.append(sixthPin.getText());
			pin = sb.toString();
			if (!isErrorShown && pin != null) {
				verify2faProgressBar.setVisibility(View.VISIBLE);
				changePassword(newPassword1.getText().toString());
			}
		}
	}

	void pasteClipboard() {
		logDebug("pasteClipboard");
		pinLongClick = false;
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clipData = clipboard.getPrimaryClip();
		if (clipData != null) {
			String code = clipData.getItemAt(0).getText().toString();
			if (code != null && code.length() == 6) {
				boolean areDigits = true;
				for (int i=0; i<6; i++) {
					if (!Character.isDigit(code.charAt(i))) {
						areDigits = false;
						break;
					}
				}
				if (areDigits) {
					firstPin.setText(""+code.charAt(0));
					secondPin.setText(""+code.charAt(1));
					thirdPin.setText(""+code.charAt(2));
					fourthPin.setText(""+code.charAt(3));
					fifthPin.setText(""+code.charAt(4));
					sixthPin.setText(""+code.charAt(5));
				}
				else {
					firstPin.setText("");
					secondPin.setText("");
					thirdPin.setText("");
					fourthPin.setText("");
					fifthPin.setText("");
					sixthPin.setText("");
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
					finish();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");
		switch(v.getId()){
			case R.id.action_change_password: {
				if (changePassword) {
					logDebug("Ok proceed to change");
					onChangePasswordClick();
				} else {
					logDebug("Reset pass on click");
					if (linkToReset == null) {
						logWarning("link is NULL");
						showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					} else {
						if (mk == null) {
							logDebug("Proceed to park account");
							onResetPasswordClick(false);
						} else {
							logDebug("Ok proceed to reset");
							onResetPasswordClick(true);
						}
					}
				}
				break;
			}
			case R.id.lost_authentication_device: {
				try {
					String url = "https://mega.nz/recovery";
					Intent openTermsIntent = new Intent(this, WebViewActivityLollipop.class);
					openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openTermsIntent.setData(Uri.parse(url));
					startActivity(openTermsIntent);
				}
				catch (Exception e){
					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
					viewIntent.setData(Uri.parse("https://mega.nz/recovery"));
					startActivity(viewIntent);
				}
				break;
			}
            case R.id.top:
                logDebug("Show top");
                try {
                    Intent openTermsIntent = new Intent(this, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(URL_E2EE));
                    startActivity(viewIntent);
                }

                break;
//			case R.id.cancel_change_password:{
//				changePasswordActivity.finish();
//				break;
//			}
		}
	}

	public void checkPasswordStrength(String s) {
		newPassword1Layout.setErrorEnabled(false);

		if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK || s.length() < 4){
			firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_very_weak));
			secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

			passwdType.setText(getString(R.string.pass_very_weak));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.login_warning));

			passwdAdvice.setText(getString(R.string.passwd_weak));

			passwdValid = false;

			newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceVeryWeak);
			newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceVeryWeak);
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK){
			firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
			secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
			tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

			passwdType.setText(getString(R.string.pass_weak));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.pass_weak));

			passwdAdvice.setText(getString(R.string.passwd_weak));

			passwdValid = true;

			newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceWeak);
			newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceWeak);
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM){
			firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
			secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
			tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
			fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

			passwdType.setText(getString(R.string.pass_medium));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.green_unlocked_rewards));

			passwdAdvice.setText(getString(R.string.passwd_medium));

			passwdValid = true;

			newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceMedium);
			newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceMedium);
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD){
			firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
			secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
			tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
			fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
			fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));

			passwdType.setText(getString(R.string.pass_good));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.pass_good));

			passwdAdvice.setText(getString(R.string.passwd_good));

			passwdValid = true;

			newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceGood);
			newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceGood);
		}
		else {
			firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));

			passwdType.setText(getString(R.string.pass_strong));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.blue_unlocked_rewards));

			passwdAdvice.setText(getString(R.string.passwd_strong));

			passwdValid = true;

			newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceStrong);
			newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceStrong);
		}

		newPassword1Error.setVisibility(View.GONE);
		newPassword1Layout.setError(" ");
		newPassword1Layout.setErrorEnabled(true);
	}

	public void onResetPasswordClick(boolean hasMk){
		logDebug("hasMk: " + hasMk);

		if(!isOnline(this))
		{
			showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		if (!validateForm(false)) {
			return;
		}

		imm.hideSoftInputFromWindow(newPassword1.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(newPassword2.getWindowToken(), 0);

		final String newPass1 = newPassword1.getText().toString();

		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.show();

		if(hasMk){
			logDebug("reset with mk");
			megaApi.confirmResetPassword(linkToReset, newPass1, mk, this);
		}
		else{
			megaApi.confirmResetPassword(linkToReset, newPass1, null, this);
		}
	}
	
	public void onChangePasswordClick(){
		logDebug("onChangePasswordClick");
		if(!isOnline(this))
		{
			showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		if (!validateForm(true)) {
			return;
		}

        imm.hideSoftInputFromWindow(newPassword1.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(newPassword2.getWindowToken(), 0);
		
		megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
	}
	
	/*
	 * Validate old password and new passwords 
	 */
	private boolean validateForm(boolean withOldPass) {
		if(withOldPass){
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

			setError(newPassword1, newPassword1Error);
			setError(newPassword2, newPassword2Error);

			if(newPassword1Error != null) {
				newPassword1.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2.requestFocus();
				return false;
			}
		}
		else{
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

			setError(newPassword1, newPassword1Error);
			setError(newPassword2, newPassword2Error);

			if(newPassword1Error != null) {
				newPassword1.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2.requestFocus();
				return false;
			}
		}
		if(!chkTOP.isChecked()) {
            showSnackbar(getString(R.string.create_account_no_top));
            return false;
        }
		return true;
	}

	/*
	 * Validate new password1
	 */
	private String getNewPassword1Error() {
		String value = newPassword1.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		else if (!passwdValid){
			containerPasswdElements.setVisibility(View.GONE);
			return getString(R.string.error_password);
		}
		return null;
	}
	
	/*
	 * Validate new password2
	 */
	private String getNewPassword2Error() {
		String value = newPassword2.getText().toString();
		String confirm = newPassword1.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		else if (!value.equals(confirm)) {
			return getString(R.string.error_passwords_dont_match);
		}
		return null;
	}
	
	private void changePassword (String newPassword){
		logDebug("changePassword");

		if (is2FAEnabled){
			megaApi.multiFactorAuthChangePassword(null, newPassword, pin, this);
		}
		else {
			megaApi.changePassword(null, newPassword, this);
			progress.setMessage(getString(R.string.my_account_changing_password));
			progress.show();
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getName());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");
		
		if (request.getType() == MegaRequest.TYPE_CHANGE_PW){
			logDebug("TYPE_CHANGE_PW");
			if (e.getErrorCode() != MegaError.API_OK){
				logWarning("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

				if (!is2FAEnabled) {
					try {
						progress.dismiss();
					} catch (Exception ex) {}
				}

				if (e.getErrorCode() == MegaError.API_EFAILED || e.getErrorCode() == MegaError.API_EEXPIRED) {
					if (verify2faProgressBar != null) {
						verify2faProgressBar.setVisibility(View.GONE);
					}
					verifyShowError();
				}
				else {
					//Intent to MyAccount
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					if (e.getErrorCode() != MegaError.API_OK) {
						logWarning("Error, request: " + e.getErrorString());
						if (e.getErrorCode() == MegaError.API_EFAILED || e.getErrorCode() == MegaError.API_EEXPIRED) {
							if (is2FAEnabled) {
								verifyShowError();
							}
						}
						else {
							showSnackbar(getString(R.string.general_text_error));
						}
					}
					else {
						resetPassIntent.setAction(ACTION_PASS_CHANGED);
						logWarning("General Error");
						resetPassIntent.putExtra(RESULT, e.getErrorCode());
						startActivity(resetPassIntent);
						finish();
					}
				}
			}
			else{
				logDebug("Pass changed OK");
				try{ 
					progress.dismiss();
				} catch(Exception ex) {};
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				if (getIntent() != null && getIntent().getBooleanExtra("logout", false)) {
					AccountController ac = new AccountController(this);
					ac.logout(this, megaApi);
				}
				else {
					//Intent to MyAccount
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					resetPassIntent.setAction(ACTION_PASS_CHANGED);
					resetPassIntent.putExtra(RESULT, e.getErrorCode());
					startActivity(resetPassIntent);
					finish();
				}
			}
		} else if (request.getType() == MegaRequest.TYPE_CONFIRM_RECOVERY_LINK) {
			logDebug("TYPE_CONFIRM_RECOVERY_LINK");

			try {
				progress.dismiss();
			} catch (Exception ex) {
				logWarning("Exception dismissing progress dialog", ex);
			}

			if (e.getErrorCode() != MegaError.API_OK) {
				logWarning("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());
			} else {
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			}

			Intent resetPassIntent;

			if (megaApi.getRootNode() == null) {
				logDebug("Not logged in");

				//Intent to Login
				resetPassIntent = new Intent(this, LoginActivityLollipop.class);
				resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			} else {
				logDebug("Logged IN");

				resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
			}

			resetPassIntent.setAction(ACTION_PASS_CHANGED);
			resetPassIntent.putExtra(RESULT, e.getErrorCode());
			startActivity(resetPassIntent);
			finish();
		} else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK) {
			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getFlag()){
					is2FAEnabled = true;
					showVerifyPin2FA();
				}
				else {
					is2FAEnabled = false;
					changePassword(newPassword1.getText().toString());
				}
			}
		}
	}

	private void setError(final EditText editText, String error){
		logDebug("setError");
		if(error == null || error.equals("")){
			return;
		}
		switch (editText.getId()){

			case R.id.change_password_newPassword1:{
				newPassword1Layout.setError(error);
				newPassword1Layout.setHintTextAppearance(R.style.InputTextAppearanceError);
				newPassword1Layout.setErrorTextAppearance(R.style.InputTextAppearanceError);
				newPassword1Error.setVisibility(View.VISIBLE);
				break;
			}
			case R.id.change_password_newPassword2:{
				newPassword2Layout.setError(error);
				newPassword2Layout.setHintTextAppearance(R.style.InputTextAppearanceError);
				newPassword2Error.setVisibility(View.VISIBLE);
				break;
			}
		}
	}

	private void quitError(EditText editText){
		switch (editText.getId()){
			case R.id.change_password_newPassword1:{
				newPassword1Layout.setError(null);
				newPassword1Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
				newPassword1Error.setVisibility(View.GONE);
				break;
			}
			case R.id.change_password_newPassword2:{
				newPassword2Layout.setError(null);
				newPassword2Layout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
				newPassword2Error.setVisibility(View.GONE);
				break;
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getName());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}

	public void showSnackbar(String s){
		showSnackbar(fragmentContainer, s);
	}

	void hideAB(){
		if (aB != null){
			aB.hide();
		}

		changeStatusBarColor(this, this.getWindow(), R.color.dark_primary_color);
	}

	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()){
			case R.id.pin_first_verify:
			case R.id.pin_second_verify:
			case R.id.pin_third_verify:
			case R.id.pin_fouth_verify:
			case R.id.pin_fifth_verify:
			case R.id.pin_sixth_verify: {
				pinLongClick = true;
				v.requestFocus();
			}
		}
		return false;
	}
}
