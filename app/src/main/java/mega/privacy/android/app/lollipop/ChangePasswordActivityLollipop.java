package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
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
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


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
	
	private EditText newPassword1View, newPassword2View;
	private RelativeLayout newPassword1ErrorView, newPassword2ErrorView;
	private TextView newPassword1ErrorText, newPassword2ErrorText;
	private Button changePasswordButton;
    private RelativeLayout fragmentContainer;
	private TextView title;
	private String linkToReset;
	private String mk;

	// TOP for 'terms of password'
    private CheckBox chkTOP;

	private ActionBar aB;
	Toolbar tB;

	private Drawable newPassword_background;
	private Drawable newPassword2_background;

	private ImageView toggleButtonNewPasswd;
	private ImageView toggleButtonNewPasswd2;
	private boolean passwdVisibility;
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
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);

		title = (TextView) findViewById(R.id.title_change_pass);

		toggleButtonNewPasswd = (ImageView) findViewById(R.id.toggle_button_new_passwd);
		toggleButtonNewPasswd.setOnClickListener(this);
		toggleButtonNewPasswd2 = (ImageView) findViewById(R.id.toggle_button_new_passwd2);
		toggleButtonNewPasswd2.setOnClickListener(this);
		passwdVisibility = false;
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

		newPassword1View = (EditText) findViewById(R.id.change_password_newPassword1);
		newPassword1View.getBackground().clearColorFilter();
		newPassword_background = newPassword1View.getBackground().mutate().getConstantState().newDrawable();
		newPassword1ErrorView = (RelativeLayout) findViewById(R.id.login_newPassword1_text_error);
		newPassword1ErrorText = (TextView) findViewById(R.id.login_newPassword1_text_error_text);

		newPassword1View.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
				if (s != null){
					if (s.length() > 0) {
						String temp = s.toString();
						containerPasswdElements.setVisibility(View.VISIBLE);

						checkPasswordStrenght(temp.trim());
					}
					else{
						passwdValid = false;
						containerPasswdElements.setVisibility(View.GONE);
					}
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(newPassword1View);
			}
		});

		newPassword1View.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					toggleButtonNewPasswd.setVisibility(View.VISIBLE);
					toggleButtonNewPasswd.setImageDrawable(ContextCompat.getDrawable(changePasswordActivity, R.drawable.ic_b_shared_read));
				}
				else {
					toggleButtonNewPasswd.setVisibility(View.GONE);
					passwdVisibility = false;
					showHidePassword(R.id.toggle_button_new_passwd);
				}
			}
		});
		
		newPassword2View = (EditText) findViewById(R.id.change_password_newPassword2);
		newPassword2View.getBackground().clearColorFilter();
		newPassword2ErrorView = (RelativeLayout) findViewById(R.id.login_newPassword2_text_error);
		newPassword2ErrorText = (TextView) findViewById(R.id.login_newPassword2_text_error_text);

		newPassword2_background = newPassword2View.getBackground().mutate().getConstantState().newDrawable();

		newPassword2View.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(newPassword2View);
			}
		});

		newPassword2View.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					toggleButtonNewPasswd2.setVisibility(View.VISIBLE);
					toggleButtonNewPasswd2.setImageDrawable(ContextCompat.getDrawable(changePasswordActivity, R.drawable.ic_b_shared_read));
				}
				else {
					toggleButtonNewPasswd2.setVisibility(View.GONE);
					passwdVisibility = false;
					showHidePassword(R.id.toggle_button_new_passwd2);
				}
			}
		});

				
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
            log(e.getMessage());
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
			log("There is an intent!");
			if(intentReceived.getAction()!=null){
				if (getIntent().getAction().equals(Constants.ACTION_RESET_PASS_FROM_LINK)) {
					log("ACTION_RESET_PASS_FROM_LINK");
					changePassword=false;
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						log("link is NULL - close activity");
						finish();
					}
					mk = getIntent().getStringExtra("MK");
					if(mk==null){
						log("MK is NULL - close activity");
						Util.showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					}

					title.setText(getString(R.string.title_enter_new_password));
				}
				if (getIntent().getAction().equals(Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT)) {
					changePassword=false;
					log("ACTION_RESET_PASS_FROM_PARK_ACCOUNT");
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						log("link is NULL - close activity");
						Util.showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					}
					mk = null;

					title.setText(getString(R.string.title_enter_new_password));
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		log("onBackPressed");
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
					Util.hideKeyboard(changePasswordActivity, 0);

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
			paramsb1.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb1.width = Util.scaleWidthPx(25, outMetrics);
		}
		firstPin.setLayoutParams(paramsb1);
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)firstPin.getLayoutParams();
		textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
		firstPin.setLayoutParams(textParams);

		secondPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = secondPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb2.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb2.width = Util.scaleWidthPx(25, outMetrics);
		}
		secondPin.setLayoutParams(paramsb2);
		textParams = (LinearLayout.LayoutParams)secondPin.getLayoutParams();
		textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
		secondPin.setLayoutParams(textParams);
		secondPin.setEt(firstPin);

		thirdPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = thirdPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb3.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb3.width = Util.scaleWidthPx(25, outMetrics);
		}
		thirdPin.setLayoutParams(paramsb3);
		textParams = (LinearLayout.LayoutParams)thirdPin.getLayoutParams();
		textParams.setMargins(0, 0, Util.scaleWidthPx(25, outMetrics), 0);
		thirdPin.setLayoutParams(textParams);
		thirdPin.setEt(secondPin);

		fourthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = fourthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb4.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb4.width = Util.scaleWidthPx(25, outMetrics);
		}
		fourthPin.setLayoutParams(paramsb4);
		textParams = (LinearLayout.LayoutParams)fourthPin.getLayoutParams();
		textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
		fourthPin.setLayoutParams(textParams);
		fourthPin.setEt(thirdPin);

		fifthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb5 = fifthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb5.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb5.width = Util.scaleWidthPx(25, outMetrics);
		}
		fifthPin.setLayoutParams(paramsb5);
		textParams = (LinearLayout.LayoutParams)fifthPin.getLayoutParams();
		textParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
		fifthPin.setLayoutParams(textParams);
		fifthPin.setEt(fourthPin);

		sixthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb6 = sixthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb6.width = Util.scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb6.width = Util.scaleWidthPx(25, outMetrics);
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
		firstPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		secondPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		thirdPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		fourthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		fifthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		sixthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
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
		log("permitVerify");
		if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
			Util.hideKeyboard(changePasswordActivity, 0);
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
			log("PIN: "+pin);
			if (!isErrorShown && pin != null) {
				verify2faProgressBar.setVisibility(View.VISIBLE);
				changePassword(newPassword1View.getText().toString());
			}
		}
	}

	void pasteClipboard() {
		log("pasteClipboard");
		pinLongClick = false;
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clipData = clipboard.getPrimaryClip();
		if (clipData != null) {
			String code = clipData.getItemAt(0).getText().toString();
			log("code: "+code);
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

	void hidePasswordIfVisible () {
		if (passwdVisibility) {
			passwdVisibility = false;
			toggleButtonNewPasswd.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_shared_read));
			showHidePassword(R.id.toggle_button_new_passwd);
			toggleButtonNewPasswd2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_shared_read));
			showHidePassword(R.id.toggle_button_new_passwd2);
		}
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.action_change_password: {
				hidePasswordIfVisible();
				if (changePassword) {
					log("ok proceed to change");
					onChangePasswordClick();
				} else {
					log("reset pass on click");
					if (linkToReset == null) {
						log("link is NULL");
						Util.showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
					} else {
						if (mk == null) {
							log("proceed to park account");
							onResetPasswordClick(false);
						} else {
							log("ok proceed to reset");
							onResetPasswordClick(true);
						}
					}
				}
				break;
			}
			case R.id.toggle_button_new_passwd: {
				if (passwdVisibility) {
					toggleButtonNewPasswd.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_shared_read));
					passwdVisibility = false;
					showHidePassword(R.id.toggle_button_new_passwd);
				}
				else {
					toggleButtonNewPasswd.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_see));
					passwdVisibility = true;
					showHidePassword(R.id.toggle_button_new_passwd);
				}
				break;
			}
			case R.id.toggle_button_new_passwd2: {
				if (passwdVisibility) {
					toggleButtonNewPasswd2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_shared_read));
					passwdVisibility = false;
					showHidePassword(R.id.toggle_button_new_passwd2);
				}
				else {
					toggleButtonNewPasswd2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_see));
					passwdVisibility = true;
					showHidePassword(R.id.toggle_button_new_passwd2);
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
                log("Show top");
                hidePasswordIfVisible();
                try {
                    Intent openTermsIntent = new Intent(this, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(Constants.URL_E2EE));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(Constants.URL_E2EE));
                    startActivity(viewIntent);
                }

                break;
//			case R.id.cancel_change_password:{
//				changePasswordActivity.finish();
//				break;
//			}
		}
	}

	public void checkPasswordStrenght(String s) {

		if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_VERYWEAK || s.length() < 4){
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				firstShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_very_weak));
				secondShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
			} else{
				firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_very_weak));
				secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			}

			passwdType.setText(getString(R.string.pass_very_weak));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.login_warning));

			passwdAdvice.setText(getString(R.string.passwd_weak));

			passwdValid = false;
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_WEAK){
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				firstShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
				secondShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
				tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
			} else{
				firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
				secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_weak));
				tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			}

			passwdType.setText(getString(R.string.pass_weak));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.pass_weak));

			passwdAdvice.setText(getString(R.string.passwd_weak));

			passwdValid = true;
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_MEDIUM){
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				firstShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				secondShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
			} else{
				firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_medium));
				fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
				fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			}

			passwdType.setText(getString(R.string.pass_medium));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.green_unlocked_rewards));

			passwdAdvice.setText(getString(R.string.passwd_medium));

			passwdValid = true;
		}
		else if (megaApi.getPasswordStrength(s) == MegaApiJava.PASSWORD_STRENGTH_GOOD){
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				firstShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				secondShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shape_password));
			} else{
				firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_good));
				fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_password));
			}

			passwdType.setText(getString(R.string.pass_good));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.pass_good));

			passwdAdvice.setText(getString(R.string.passwd_good));

			passwdValid = true;
		}
		else {
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				firstShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				secondShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				tirdShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				fourthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				fifthShape.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			} else{
				firstShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				secondShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				tirdShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				fourthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
				fifthShape.setBackground(ContextCompat.getDrawable(this, R.drawable.passwd_strong));
			}

			passwdType.setText(getString(R.string.pass_strong));
			passwdType.setTextColor(ContextCompat.getColor(this, R.color.blue_unlocked_rewards));

			passwdAdvice.setText(getString(R.string.passwd_strong));

			passwdValid = true;
		}
	}

	public void showHidePassword (int type) {
		if(!passwdVisibility){
			switch (type) {
				case R.id.toggle_button_new_passwd: {
					newPassword1View.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					newPassword1View.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
					newPassword1View.setSelection(newPassword1View.getText().length());
					break;
				}
				case R.id.toggle_button_new_passwd2: {
					newPassword2View.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					newPassword2View.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
					newPassword2View.setSelection(newPassword2View.getText().length());
					break;
				}
			}
		}else{
			switch (type) {
				case R.id.toggle_button_new_passwd: {
					newPassword1View.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					newPassword1View.setSelection(newPassword1View.getText().length());
					break;
				}
				case R.id.toggle_button_new_passwd2: {
					newPassword2View.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					newPassword2View.setSelection(newPassword2View.getText().length());
					break;
				}
			}
		}
	}

	public void onResetPasswordClick(boolean hasMk){
		log("onResetPasswordClick");

		if(!Util.isOnline(this))
		{
			showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		if (!validateForm(false)) {
			return;
		}

		imm.hideSoftInputFromWindow(newPassword1View.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(newPassword2View.getWindowToken(), 0);

		String newPassword1 = newPassword1View.getText().toString();
		String newPassword2 = newPassword2View.getText().toString();

//		if (!newPassword1.equals(newPassword2)){
//			log("no new password repeat");
////			newPassword2View.setError(getString(R.string.my_account_change_password_dont_match));
//			setError(newPassword2View, getString(R.string.my_account_change_password_dont_match));
//			return;
//		}

		final String newPassword = newPassword1;

		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.show();

		if(hasMk){
			log("reset with mk");
			megaApi.confirmResetPassword(linkToReset, newPassword, mk, this);
		}
		else{
			megaApi.confirmResetPassword(linkToReset, newPassword, null, this);
		}
	}
	
	public void onChangePasswordClick(){
		log("onChangePasswordClick");
		if(!Util.isOnline(this))
		{
			showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		if (!validateForm(true)) {
			return;
		}

        imm.hideSoftInputFromWindow(newPassword1View.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(newPassword2View.getWindowToken(), 0);
		

		String newPassword1 = newPassword1View.getText().toString();
		String newPassword2 = newPassword2View.getText().toString();

//		if (!newPassword1.equals(newPassword2)){
//			log("no new password repeat");
////			newPassword2View.setError(getString(R.string.my_account_change_password_dont_match));
//			setError(newPassword2View, getString(R.string.my_account_change_password_dont_match));
//			return;
//		}
		
		megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
//		changePassword(newPassword1);
	}
	
//	private boolean checkPassword (String oldPassword, String newPassword1, String newPassword2){
//		log(newPassword1);
//		log(newPassword2);
//		if (!newPassword1.equals(newPassword2)){
//			log("no new password repeat");
//			return false;
//		}
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext()); 
//		UserCredentials cred = dbH.getCredentials();
//		String email = cred.getEmail();
//		new CheckTask();
//		String privateKey = megaApi.getBase64PwKey(oldPassword);
//		String publicKey = megaApi.getStringHash(privateKey, cred.getEmail());
//		
//		if (!privateKey.equals(cred.getPrivateKey()) || !publicKey.equals(cred.getPublicKey())){
//			log("no old password");
//			return false;
//		}
//			
//		return true;
//	}
	
	/*
	 * Validate old password and new passwords 
	 */
	private boolean validateForm(boolean withOldPass) {
		if(withOldPass){
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

//			newPassword1View.setError(newPassword1Error);
			setError(newPassword1View, newPassword1Error);
//			newPassword2View.setError(newPassword2Error);
			setError(newPassword2View, newPassword2Error);

			if(newPassword1Error != null) {
				newPassword1View.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2View.requestFocus();
				return false;
			}
		}
		else{
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

//			newPassword1View.setError(newPassword1Error);
			setError(newPassword1View, newPassword1Error);
//			newPassword2View.setError(newPassword2Error);
			setError(newPassword2View, newPassword2Error);

			if(newPassword1Error != null) {
				newPassword1View.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2View.requestFocus();
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
		String value = newPassword1View.getText().toString();
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
		String value = newPassword2View.getText().toString();
		String confirm = newPassword1View.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		else if (!value.equals(confirm)) {
			return getString(R.string.error_passwords_dont_match);
		}
		return null;
	}
	
	private void changePassword (String newPassword){
		log("changePassword");

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
		log("onRequestStart: " + request.getName());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");
		
		if (request.getType() == MegaRequest.TYPE_CHANGE_PW){
			log("TYPE_CHANGE_PW");
			if (e.getErrorCode() != MegaError.API_OK){
				log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

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
						log("Error, request: " + e.getErrorString());
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
						resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
						log("General Error");
						resetPassIntent.putExtra("RESULT", -1);
						startActivity(resetPassIntent);
						finish();
					}
				}
			}
			else{
				log("pass changed OK");
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
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", 0);
					startActivity(resetPassIntent);
					finish();
				}
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_RECOVERY_LINK){
			log("TYPE_CONFIRM_RECOVERY_LINK");
			if(megaApi.getRootNode()==null) {
				log("Not logged in");
				if (e.getErrorCode() != MegaError.API_OK){
					log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

					try{
						progress.dismiss();
					} catch(Exception ex) {};

					//Intent to Login
					Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
					resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);

					if(e.getErrorCode()==MegaError.API_EARGS){
						resetPassIntent.putExtra("RESULT", MegaError.API_EARGS);
					}
					else if(e.getErrorCode()==MegaError.API_EKEY){
						resetPassIntent.putExtra("RESULT", MegaError.API_EKEY);
					}
					else{
						resetPassIntent.putExtra("RESULT", -1);
					}

					startActivity(resetPassIntent);
					finish();
				}
				else{
					log("pass changed");
					try{
						progress.dismiss();
					} catch(Exception ex) {};

					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					//Intent to Login
					Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
					resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", 0);
					startActivity(resetPassIntent);
					finish();
				}
			}
			else {
				log("Logged IN");

				if (e.getErrorCode() != MegaError.API_OK){
					log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

					try{
						progress.dismiss();
					} catch(Exception ex) {};

					//Intent to Login
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", -1);
					startActivity(resetPassIntent);
					finish();
				}
				else{
					log("pass changed");
					try{
						progress.dismiss();
					} catch(Exception ex) {};

					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					//Intent to Login
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", 0);
					startActivity(resetPassIntent);
					finish();
				}

			}
		}
		else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK){
			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getFlag()){
					is2FAEnabled = true;
					showVerifyPin2FA();
				}
				else {
					is2FAEnabled = false;
					changePassword(newPassword1View.getText().toString());
				}
			}
		}
	}

	private void setError(final EditText editText, String error){
		log("setError");
		if(error == null || error.equals("")){
			return;
		}
		switch (editText.getId()){

			case R.id.change_password_newPassword1:{
				newPassword1ErrorView.setVisibility(View.VISIBLE);
				newPassword1ErrorText.setText(error);
				PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
				Drawable background = newPassword_background.mutate().getConstantState().newDrawable();
				background.setColorFilter(porterDuffColorFilter);
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					newPassword1View.setBackgroundDrawable(background);
				} else{
					newPassword1View.setBackground(background);
				}
				break;
			}
			case R.id.change_password_newPassword2:{
				newPassword2ErrorView.setVisibility(View.VISIBLE);
				newPassword2ErrorText.setText(error);
				PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
				Drawable background = newPassword2_background.mutate().getConstantState().newDrawable();
				background.setColorFilter(porterDuffColorFilter);
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					newPassword2View.setBackgroundDrawable(background);
				} else{
					newPassword2View.setBackground(background);
				}
				break;
			}
		}
	}

	private void quitError(EditText editText){
		switch (editText.getId()){
			case R.id.change_password_newPassword1:{
				if(newPassword1ErrorView.getVisibility() != View.GONE){
					newPassword1ErrorView.setVisibility(View.GONE);
					if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
						newPassword1View.setBackgroundDrawable(newPassword_background);
					} else{
						newPassword1View.setBackground(newPassword_background);
					}
				}
				break;
			}
			case R.id.change_password_newPassword2:{
				if(newPassword2ErrorView.getVisibility() != View.GONE){
					newPassword2ErrorView.setVisibility(View.GONE);
					if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
						newPassword2View.setBackgroundDrawable(newPassword2_background);
					} else{
						newPassword2View.setBackground(newPassword2_background);
					}
				}
				break;
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public static void log(String message) {
		Util.log("ChangePasswordActivityLollipop", message);
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_login));
		}
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
