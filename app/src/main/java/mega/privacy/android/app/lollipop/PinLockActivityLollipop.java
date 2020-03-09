package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.PinUtil;
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

import static mega.privacy.android.app.utils.LogUtil.*;

@SuppressLint("NewApi")
public class PinLockActivityLollipop extends BaseActivity implements OnClickListener, MegaRequestListenerInterface {

	public static String ACTION_SET_PIN_LOCK = "ACTION_SET";
	public static String ACTION_RESET_PIN_LOCK = "ACTION_RESET";

	final public static int MAX_ATTEMPS = 10;
	final public static int SET = 0;
	final public static int UNLOCK = 1;
	final public static int RESET_UNLOCK = 2;
	final public static int RESET_SET = 3;

	Handler handler;

	float density;
	DisplayMetrics outMetrics;
	Display display;

	String chosenTypePin;

	CoordinatorLayout coordinatorLayout;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	RelativeLayout fragmentContainer;
    LinearLayout sixPinLayout;
    LinearLayout fourPinLayout;
    LinearLayout alphanumericLayout;
    LinearLayout warningLayout;
	LinearLayout switchLayout;
    RelativeLayout redLayout;
    RelativeLayout buttonsLayout;
	TextView textLogout;
	Button enterButton;
	Button logoutButton;
	TextView unlockText;
	EditText passwordText;
	TextView warningText;
	EditTextPIN passFirstLetter;
	EditTextPIN passSecondLetter;
	EditTextPIN passThirdLetter;
	EditTextPIN passFourthLetter;
	EditTextPIN passFifthLetter;
	EditTextPIN passSixthLetter;
	SwitchCompat passwordSwitch;
	final StringBuilder sbFirst=new StringBuilder();
	final StringBuilder sbSecond=new StringBuilder();
	boolean secondRound = false;
	InputMethodManager imm;

	int mode = UNLOCK;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	MegaAttributes att = null;

	int attemps = 0;

	static PinLockActivityLollipop pinLockActivity = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		dbH = new DatabaseHandler(this);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		prefs = dbH.getPreferences();
		att = dbH.getAttributes();
		attemps = att.getAttemps();
		logDebug("Attemps number: " + attemps);

		if (savedInstanceState != null) {
			logDebug("Bundle is NOT NULL");
			mode = savedInstanceState.getInt("mode");
			chosenTypePin = savedInstanceState.getString("chosenTypePin");
			secondRound = savedInstanceState.getBoolean("isSecondRound");
			if (secondRound) {
				sbFirst.append(savedInstanceState.get("sbFirstString"));
			}
		}
		else {
			logDebug("Bundle is NULL");

			if (prefs != null) {
				chosenTypePin = prefs.getPinLockType();
			}

			Intent intent = getIntent();
			if ((intent != null) && (intent.getAction() != null)){
				if (intent.getAction().equals(ACTION_SET_PIN_LOCK)){
					mode=SET;
				}
				else if(intent.getAction().equals(ACTION_RESET_PIN_LOCK)){
					mode=RESET_UNLOCK;
				}
			}
		}

		setContentView(R.layout.activity_pin_lock);

		pinLockActivity = this;

		if(megaApi==null) {
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
		}

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_pin_lock);
		coordinatorLayout = (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout);
		android.view.ViewGroup.LayoutParams coordinatorLayoutParams = coordinatorLayout.getLayoutParams();
		coordinatorLayoutParams.height = Util.scaleHeightPx(75, outMetrics);
		coordinatorLayout.setLayoutParams(coordinatorLayoutParams);

		fourPinLayout = (LinearLayout) findViewById(R.id.four_pin_layout);
		sixPinLayout = (LinearLayout) findViewById(R.id.six_pin_layout);
		alphanumericLayout = (LinearLayout) findViewById(R.id.alphanumeric_pin_layout);

		switchLayout = (LinearLayout) findViewById(R.id.switch_layout);

		redLayout = (RelativeLayout) findViewById(R.id.red_layout);
		redLayout.setVisibility(View.GONE);

		textLogout = (TextView) findViewById(R.id.alert_text);
		textLogout.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20));
		//Margins
		RelativeLayout.LayoutParams textLogoutParams = (RelativeLayout.LayoutParams)textLogout.getLayoutParams();
		textLogoutParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics), 0);
		textLogout.setLayoutParams(textLogoutParams);

		warningLayout = (LinearLayout) findViewById(R.id.warning_layout);
		//Margins
//		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)warningLayout.getLayoutParams();
//		warningParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(10, outMetrics), Util.scaleHeightPx(20, outMetrics));
//		warningLayout.setLayoutParams(warningParams);

		warningText = (TextView) findViewById(R.id.warning_text);
		//Margins
		LinearLayout.LayoutParams textWarningParams = (LinearLayout.LayoutParams)warningText.getLayoutParams();
		textWarningParams.setMargins(Util.scaleWidthPx(3, outMetrics), 0, Util.scaleWidthPx(3, outMetrics), 0);
		warningText.setLayoutParams(textWarningParams);

		buttonsLayout = (RelativeLayout) findViewById(R.id.buttons_layout);

		logoutButton = (Button) findViewById(R.id.button_logout);
		logoutButton.setText(getString(R.string.action_logout));
		logoutButton.setOnClickListener(this);

		enterButton = (Button) findViewById(R.id.button_enter);
		enterButton.setText(getString(R.string.general_ok));
		enterButton.setOnClickListener(this);

		unlockText = (TextView) findViewById(R.id.unlock_text_view);
//		unlockText.setGravity(Gravity.CENTER_HORIZONTAL); //NOT WORKING!!!

		if (mode == RESET_SET) {
			unlockText.setText(secondRound ? R.string.reset_pin_title_2 : R.string.reset_pin_title);
		}
		else {
			unlockText.setText(secondRound ? R.string.unlock_pin_title_2 : R.string.unlock_pin_title);
		}

		if(mode!=SET){
			logoutButton.setVisibility(View.VISIBLE);
		}
		else{
			logoutButton.setVisibility(View.INVISIBLE);
		}

		logDebug("ATTEMPS value: " + attemps);
		if (attemps==MAX_ATTEMPS-1){
			//Last intent available!!
			logDebug("Last intent: " + attemps);
		}
		else if(attemps==MAX_ATTEMPS){
			if(attemps==10){
				//Log out!!
				logWarning("INTENTS==10 - LOGOUT");
				redLayout.setVisibility(View.VISIBLE);
				textLogout.setText(getString(R.string.incorrect_pin_activity, 5));

				hideKeyboard(this);

				CountDownTimer cDT = new CountDownTimer(6000, 1000) {

					public void onTick(long millisUntilFinished) {
						redLayout.setVisibility(View.VISIBLE);
						textLogout.setText(getString(R.string.incorrect_pin_activity, millisUntilFinished / 1000));
					}

					public void onFinish() {
						logWarning("Logout!!!");
						attemps=0;
						att.setAttemps(attemps);
//						dbH.setAttributes(att);
						dbH.setAttrAttemps(attemps);
						AccountController accountController = new AccountController(getApplicationContext());
						accountController.logout(pinLockActivity, megaApi);
					}
				}.start();
			}
		}
		else if(attemps>=5){
			//Show alert
			logDebug("Attemps less than 5: " + attemps);
			warningLayout.setVisibility(View.VISIBLE);
		}
		else{
			//Hide alert
			logDebug("Number of attemps: " + attemps);
			warningLayout.setVisibility(View.INVISIBLE);
		}

		if (prefs != null){
			if (chosenTypePin != null){
				if(chosenTypePin.equals(Constants.PIN_4)){
					logDebug("4 PIN");
					add4DigitsPin();
				}
				else if(chosenTypePin.equals(Constants.PIN_6)){
					add6DigitsPin();
				}
				else{
					addAlphanumericPin();
				}
			}
			else{
				logWarning("Pin lock type is NULL");

				String code = prefs.getPinLockCode();
				if(code!=null){
					boolean atleastOneAlpha = code.matches(".*[a-zA-Z]+.*");
					if (atleastOneAlpha) {
						logDebug("Alphanumeric");
						prefs.setPinLockType(Constants.PIN_ALPHANUMERIC);
						dbH.setPinLockType(Constants.PIN_ALPHANUMERIC);
						addAlphanumericPin();
					}
					else{
						if(code.length()==4){
							logDebug("FOUR PIN detected");
							prefs.setPinLockType(Constants.PIN_4);
							dbH.setPinLockType(Constants.PIN_4);
							add4DigitsPin();
						}
						else if(code.length()==6){
							logDebug("SIX PIN detected");
							prefs.setPinLockType(Constants.PIN_6);
							dbH.setPinLockType(Constants.PIN_6);
							add6DigitsPin();
						}
						else{
							logDebug("DEFAULT FOUR PIN");
							prefs.setPinLockType(Constants.PIN_4);
							dbH.setPinLockType(Constants.PIN_4);
							add4DigitsPin();
						}
					}
				}
			}
		}
	}

	private void addAlphanumericPin(){
		logDebug("addAlphanumericPin");

		fourPinLayout.setVisibility(View.GONE);
		sixPinLayout.setVisibility(View.GONE);
		alphanumericLayout.setVisibility(View.VISIBLE);
		switchLayout.setVisibility(View.VISIBLE);

		passwordSwitch = (SwitchCompat) findViewById(R.id.switch_pin);
		passwordSwitch.setChecked(false);

		buttonsLayout.getLayoutParams().width = Util.scaleWidthPx(240, outMetrics);

		//Margins
		RelativeLayout.LayoutParams pinParams = (RelativeLayout.LayoutParams)alphanumericLayout.getLayoutParams();
		pinParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(20, outMetrics));
		alphanumericLayout.setLayoutParams(pinParams);
		alphanumericLayout.setVisibility(View.VISIBLE);

		passwordText = (EditText) findViewById(R.id.alphanumeric_text);
		android.view.ViewGroup.LayoutParams paramsPassword = passwordText.getLayoutParams();
		paramsPassword.width = Util.scaleWidthPx(250, outMetrics);
		passwordText.setLayoutParams(paramsPassword);

		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)buttonsLayout.getLayoutParams();
		warningParams.addRule(RelativeLayout.BELOW, switchLayout.getId());

		passwordText.requestFocus();

		passwordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(!isChecked){
					passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					passwordText.setSelection(passwordText.getText().length());
				}else{
					passwordText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					passwordText.setSelection(passwordText.getText().length());
				}
			}
		});

		enterButton.setVisibility(View.VISIBLE);

		//Margins warningLayout
//		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)warningLayout.getLayoutParams();
//		warningParams.addRule(RelativeLayout.BELOW, switchLayout.getId());
	}

	private void add6DigitsPin(){
		logDebug("add6DigitsPin");

		fourPinLayout.setVisibility(View.GONE);
		alphanumericLayout.setVisibility(View.GONE);
		switchLayout.setVisibility(View.GONE);

		buttonsLayout.getLayoutParams().width = Util.scaleWidthPx(280, outMetrics);

		//Margins
		RelativeLayout.LayoutParams pinParams = (RelativeLayout.LayoutParams)sixPinLayout.getLayoutParams();
		pinParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(20, outMetrics));
		sixPinLayout.setLayoutParams(pinParams);
		sixPinLayout.setVisibility(View.VISIBLE);

		enterButton.setVisibility(View.GONE);

		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)buttonsLayout.getLayoutParams();
		warningParams.addRule(RelativeLayout.BELOW, sixPinLayout.getId());

		//PIN
		passFirstLetter = (EditTextPIN) findViewById(R.id.six_pass_first);
		passFirstLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = passFirstLetter.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(40, outMetrics);
		passFirstLetter.setLayoutParams(paramsb1);
		//Margins
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)passFirstLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(0, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passFirstLetter.setLayoutParams(textParams);

		passSecondLetter = (EditTextPIN) findViewById(R.id.six_pass_second);
		passSecondLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = passSecondLetter.getLayoutParams();
		paramsb2.width = Util.scaleWidthPx(40, outMetrics);
		passSecondLetter.setLayoutParams(paramsb2);
		//Margins
		textParams = (LinearLayout.LayoutParams)passSecondLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passSecondLetter.setLayoutParams(textParams);
		passSecondLetter.setEt(passFirstLetter);

		passThirdLetter = (EditTextPIN) findViewById(R.id.six_pass_third);
		passThirdLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = passThirdLetter.getLayoutParams();
		paramsb3.width = Util.scaleWidthPx(40, outMetrics);
		passThirdLetter.setLayoutParams(paramsb3);
		//Margins
		textParams = (LinearLayout.LayoutParams)passThirdLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passThirdLetter.setLayoutParams(textParams);
		passThirdLetter.setEt(passSecondLetter);

		passFourthLetter = (EditTextPIN) findViewById(R.id.six_pass_fourth);
		passFourthLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = passFourthLetter.getLayoutParams();
		paramsb4.width = Util.scaleWidthPx(40, outMetrics);
		passFourthLetter.setLayoutParams(paramsb4);
		//Margins
		textParams = (LinearLayout.LayoutParams)passFourthLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passFourthLetter.setLayoutParams(textParams);
		passFourthLetter.setEt(passThirdLetter);

		passFifthLetter = (EditTextPIN) findViewById(R.id.six_pass_fifth);
		passFifthLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb5 = passFifthLetter.getLayoutParams();
		paramsb5.width = Util.scaleWidthPx(40, outMetrics);
		passFifthLetter.setLayoutParams(paramsb5);
		//Margins
		textParams = (LinearLayout.LayoutParams)passFifthLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passFifthLetter.setLayoutParams(textParams);
		passFifthLetter.setEt(passFourthLetter);

		passSixthLetter = (EditTextPIN) findViewById(R.id.six_pass_sixth);
		passSixthLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb6 = passSixthLetter.getLayoutParams();
		paramsb6.width = Util.scaleWidthPx(40, outMetrics);
		passSixthLetter.setLayoutParams(paramsb6);
		//Margins
		textParams = (LinearLayout.LayoutParams)passSixthLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passSixthLetter.setLayoutParams(textParams);
		passSixthLetter.setEt(passFifthLetter);

		passFirstLetter.requestFocus();
		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(passFirstLetter, InputMethodManager.SHOW_FORCED);
//		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

		//Add TextWatcher to first letter
		passFirstLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("6passFirstLetter: afterTextChanged");
            	if(passFirstLetter.length()!=0){
                	passSecondLetter.requestFocus();
                    passSecondLetter.setCursorVisible(true);

                    passSecondLetter.setText("");
                    passThirdLetter.setText("");
                    passFourthLetter.setText("");
                }
            }
        });


		passSecondLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("6passSecondLetter: afterTextChanged");
            	 if(passSecondLetter.length()!=0){
                     passThirdLetter.requestFocus();
                     passThirdLetter.setCursorVisible(true);

                     passThirdLetter.setText("");
                     passFourthLetter.setText("");
                 }
            }
        });

		passThirdLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("6passThirdLetter: afterTextChanged");
            	if(passThirdLetter.length()!=0){
                	passFourthLetter.requestFocus();
                	passFourthLetter.setCursorVisible(true);

                    passFourthLetter.setText("");
                }
            }
        });

		passFourthLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("6passFourthLetter: afterTextChanged");
            	if(passFourthLetter.length()!=0){
                	passFifthLetter.requestFocus();
                	passFifthLetter.setCursorVisible(true);

                	passFifthLetter.setText("");
                }
            }
        });

		passFifthLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("6passFifthLetter: afterTextChanged");
            	if(passFifthLetter.length()!=0){
                	passSixthLetter.requestFocus();
                	passSixthLetter.setCursorVisible(true);

                	passSixthLetter.setText("");
                }
            }
        });

		passSixthLetter.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				logDebug("6passSixthLetter: afterTextChanged");
            	if(passSixthLetter.length()!=0){
            		passFirstLetter.setCursorVisible(false);
            		passFirstLetter.requestFocus();
                    if(!secondRound)
                    {
                    	if(passFirstLetter.length()==1 & passSecondLetter.length()==1 & passThirdLetter.length()==1 & passFourthLetter.length()==1 & passFifthLetter.length()==1 & passSixthLetter.length()==1){
                    		sbFirst.append(passFirstLetter.getText());
                    		sbFirst.append(passSecondLetter.getText());
                    		sbFirst.append(passThirdLetter.getText());
                    		sbFirst.append(passFourthLetter.getText());
                    		sbFirst.append(passFifthLetter.getText());
                    		sbFirst.append(passSixthLetter.getText());
                    	}

                        switch(mode){
    	                    case RESET_SET:
    	                    {
    	                    	//Re-enter pass
    	                        passFirstLetter.setText("");
    	                        passSecondLetter.setText("");
    	                        passThirdLetter.setText("");
    	                        passFourthLetter.setText("");
    	                        passFifthLetter.setText("");
    	                        passSixthLetter.setText("");

    	                        passFirstLetter.requestFocus();
    	                        passFirstLetter.setCursorVisible(true);
    	                    	unlockText.setText(R.string.reset_pin_title_2);
    	                    	secondRound=true;
    	                    	break;
    	                    }
    	                    case UNLOCK:
    	                    case RESET_UNLOCK:
    	                    {
    	                    	submitForm(sbFirst.toString());
    	                    	break;
    	                    }
    	                    default:
    	                    {
    	                    	//Re-enter pass
								logDebug("Default CASE");
    	                        passFirstLetter.setText("");
    	                        passSecondLetter.setText("");
    	                        passThirdLetter.setText("");
    	                        passFourthLetter.setText("");
    	                        passFifthLetter.setText("");
    	                        passSixthLetter.setText("");

    	                        passFirstLetter.requestFocus();
    	                        passFirstLetter.setCursorVisible(true);
    	                    	unlockText.setText(R.string.unlock_pin_title_2);
    	                    	secondRound=true;
    	                    	break;
    	                    }
                        }

                    }
                    else if(secondRound)
                    {
						logDebug("SECOND TIME 4thletter");
                    	if(passFirstLetter.length()==1 & passSecondLetter.length()==1 & passThirdLetter.length()==1 & passFourthLetter.length()==1){
                    		sbSecond.append(passFirstLetter.getText());
                    		sbSecond.append(passSecondLetter.getText());
                    		sbSecond.append(passThirdLetter.getText());
                    		sbSecond.append(passFourthLetter.getText());
                    		sbSecond.append(passFifthLetter.getText());
                    		sbSecond.append(passSixthLetter.getText());
                    	}
//                    	submitForm(sbSecond.toString());
                        if(sbFirst.toString().equals(sbSecond.toString())){
							logDebug("PIN match - submit form");
                        	submitForm(sbSecond.toString());
                        }
                        else{
							logWarning("PIN NOT match - show snackBar");
                        	secondRound = false;
							showSnackbar(getString(R.string.pin_lock_not_match));

                            //Re-enter pass
                            passFirstLetter.setText("");
                            passSecondLetter.setText("");
                            passThirdLetter.setText("");
                            passFourthLetter.setText("");
                            passFifthLetter.setText("");
	                        passSixthLetter.setText("");

                            passFirstLetter.requestFocus();
                            passFirstLetter.setCursorVisible(true);
                            sbFirst.setLength(0);
                            sbSecond.setLength(0);
                            if(getMode()==RESET_SET){
                            	unlockText.setText(R.string.reset_pin_title);
                            }
                            else{
                            	unlockText.setText(R.string.unlock_pin_title);
                            }
                        }
                    }
            	}
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
	}

	void showSnackbar (String s) {
		showSnackbar(coordinatorLayout, s);
	}

	private void add4DigitsPin(){
		logDebug("add4DigitsPin");
		//Margins
		RelativeLayout.LayoutParams pinParams = (RelativeLayout.LayoutParams)fourPinLayout.getLayoutParams();
		pinParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(20, outMetrics));
		fourPinLayout.setLayoutParams(pinParams);
		fourPinLayout.setVisibility(View.VISIBLE);

		enterButton.setVisibility(View.GONE);
		alphanumericLayout.setVisibility(View.GONE);
		switchLayout.setVisibility(View.GONE);
		sixPinLayout.setVisibility(View.GONE);

		buttonsLayout.getLayoutParams().width = Util.scaleWidthPx(210, outMetrics);

		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)buttonsLayout.getLayoutParams();
		warningParams.addRule(RelativeLayout.BELOW, fourPinLayout.getId());

		//PIN
		passFirstLetter = (EditTextPIN) findViewById(R.id.pass_first);
		passFirstLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = passFirstLetter.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(40, outMetrics);
		passFirstLetter.setLayoutParams(paramsb1);
		//Margins
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)passFirstLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(0, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passFirstLetter.setLayoutParams(textParams);

		passSecondLetter = (EditTextPIN) findViewById(R.id.pass_second);
		passSecondLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = passSecondLetter.getLayoutParams();
		paramsb2.width = Util.scaleWidthPx(40, outMetrics);
		passSecondLetter.setLayoutParams(paramsb2);
		//Margins
		textParams = (LinearLayout.LayoutParams)passSecondLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passSecondLetter.setLayoutParams(textParams);
		passSecondLetter.setEt(passFirstLetter);

		passThirdLetter = (EditTextPIN) findViewById(R.id.pass_third);
		passThirdLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = passThirdLetter.getLayoutParams();
		paramsb3.width = Util.scaleWidthPx(40, outMetrics);
		passThirdLetter.setLayoutParams(paramsb3);
		//Margins
		textParams = (LinearLayout.LayoutParams)passThirdLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passThirdLetter.setLayoutParams(textParams);
		passThirdLetter.setEt(passSecondLetter);

		passFourthLetter = (EditTextPIN) findViewById(R.id.pass_fourth);
		passFourthLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = passFourthLetter.getLayoutParams();
		paramsb4.width = Util.scaleWidthPx(40, outMetrics);
		passFourthLetter.setLayoutParams(paramsb4);
		passFourthLetter.setEt(passThirdLetter);
		//Margins
		textParams = (LinearLayout.LayoutParams)passFourthLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		passFourthLetter.setLayoutParams(textParams);

		passFirstLetter.requestFocus();
		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(passFirstLetter, InputMethodManager.SHOW_FORCED);
//		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

		//Add TextWatcher to first letter		
		passFirstLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("4passFirstLetter: afterTextChanged");
            	if(passFirstLetter.length()!=0){
                	passSecondLetter.requestFocus();
                    passSecondLetter.setCursorVisible(true);

                    passSecondLetter.setText("");
                    passThirdLetter.setText("");
                    passFourthLetter.setText("");
                }
            }
        });

		passSecondLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("4passSecondLetter: afterTextChanged");
            	 if(passSecondLetter.length()!=0){
                     passThirdLetter.requestFocus();
                     passThirdLetter.setCursorVisible(true);

                     passThirdLetter.setText("");
                     passFourthLetter.setText("");
                 }
            }
        });

		passThirdLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {
				logDebug("4passThirdLetter: afterTextChanged");
            	if(passThirdLetter.length()!=0){
                	passFourthLetter.requestFocus();
                	passFourthLetter.setCursorVisible(true);

                    passFourthLetter.setText("");
                }
            }
        });

		passFourthLetter.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				logDebug("4passFourthLetter: afterTextChanged");
            	if(passFourthLetter.length()!=0){
            		passFirstLetter.setCursorVisible(false);
            		passFirstLetter.requestFocus();
                    if(!secondRound)
                    {
                    	if(passFirstLetter.length()==1 & passSecondLetter.length()==1 & passThirdLetter.length()==1 & passFourthLetter.length()==1){
                    		sbFirst.append(passFirstLetter.getText());
                    		sbFirst.append(passSecondLetter.getText());
                    		sbFirst.append(passThirdLetter.getText());
                    		sbFirst.append(passFourthLetter.getText());
                    	}

                        switch(mode){
    	                    case RESET_SET:
    	                    {
    	                    	//Re-enter pass
    	                        passFirstLetter.setText("");
    	                        passSecondLetter.setText("");
    	                        passThirdLetter.setText("");
    	                        passFourthLetter.setText("");

    	                        passFirstLetter.requestFocus();
    	                        passFirstLetter.setCursorVisible(true);
    	                    	unlockText.setText(R.string.reset_pin_title_2);
    	                    	secondRound=true;
    	                    	break;
    	                    }
    	                    case UNLOCK:
    	                    case RESET_UNLOCK:
    	                    {
    	                    	submitForm(sbFirst.toString());
    	                    	break;
    	                    }
    	                    default:
    	                    {
    	                    	//Re-enter pass
								logDebug("Default CASE");
    	                        passFirstLetter.setText("");
    	                        passSecondLetter.setText("");
    	                        passThirdLetter.setText("");
    	                        passFourthLetter.setText("");

    	                        passFirstLetter.requestFocus();
    	                        passFirstLetter.setCursorVisible(true);
    	                    	unlockText.setText(R.string.unlock_pin_title_2);
    	                    	secondRound=true;
    	                    	break;
    	                    }
                        }

                    }
                    else if(secondRound)
                    {
						logDebug("SECOND TIME 4thletter");
                    	if(passFirstLetter.length()==1 & passSecondLetter.length()==1 & passThirdLetter.length()==1 & passFourthLetter.length()==1){
                    		sbSecond.append(passFirstLetter.getText());
                    		sbSecond.append(passSecondLetter.getText());
                    		sbSecond.append(passThirdLetter.getText());
                    		sbSecond.append(passFourthLetter.getText());
                    	}

//                    	submitForm(sbSecond.toString());
                        if(sbFirst.toString().equals(sbSecond.toString())){
							logDebug("PIN match - submit form");
                        	submitForm(sbSecond.toString());
                        }
                        else{
							logWarning("PIN NOT match - show snackBar");
                        	secondRound = false;
							showSnackbar(getString(R.string.pin_lock_not_match));

                            //Re-enter pass
                            passFirstLetter.setText("");
                            passSecondLetter.setText("");
                            passThirdLetter.setText("");
                            passFourthLetter.setText("");

                            passFirstLetter.requestFocus();
                            passFirstLetter.setCursorVisible(true);
                            sbFirst.setLength(0);
                            sbSecond.setLength(0);
                            if(getMode()==RESET_SET){
                            	unlockText.setText(R.string.reset_pin_title);
                            }
                            else{
                            	unlockText.setText(R.string.unlock_pin_title);
                            }
                        }
                    }
            	}
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
	}

	private void setPin(String pin){
		logDebug("setPin");

		if ( pin != null){
			dbH.setPinLockCode(pin);
			if(chosenTypePin!=null){
				dbH.setPinLockType(chosenTypePin);
			}
		}
		PinUtil.update();
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		logDebug("finish!");
		finish();
	}

	/*
	 * Validate Pin code
	 */
	private void submitForm(String code) {
//		String code = sbSecond

		switch(mode){
			case UNLOCK:{
				String codePref = prefs.getPinLockCode();

				if (code.compareTo(codePref) == 0){
					PinUtil.update();
					attemps=0;
					att.setAttemps(attemps);
					dbH.setAttrAttemps(attemps);
					finish();
				}
				else{
					logDebug("PIN INCORRECT RESET_UNLOCK - show snackBar");
					attemps=attemps+1;
					att.setAttemps(attemps);
//						dbH.setAttributes(att);
					dbH.setAttrAttemps(attemps);
					if(attemps==10){
						//Log out!!
						logDebug("INTENTS==10 - LOGOUT");
				    	redLayout.setVisibility(View.VISIBLE);
				    	textLogout.setText(getString(R.string.incorrect_pin_activity, 5));

				    	if(passSixthLetter!=null){
				    		passSixthLetter.setCursorVisible(false);
				    	}
				    	else{
				    		passFourthLetter.setCursorVisible(false);
				    	}

						imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
//						imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
						//						Intent intent = new Intent(this, IncorrectPinActivityLollipop.class);
						//						startActivity(intent);
						//						finish();

						CountDownTimer cDT = new CountDownTimer(6000, 1000) {

						     public void onTick(long millisUntilFinished) {
						    	 redLayout.setVisibility(View.VISIBLE);
						    	 textLogout.setText(getString(R.string.incorrect_pin_activity, millisUntilFinished / 1000));
						     }

						     public void onFinish() {
								 logWarning("Logout!!!");
								 attemps=0;
								 att.setAttemps(attemps);
//						dbH.setAttributes(att);
								 dbH.setAttrAttemps(attemps);
								 AccountController aC = new AccountController(getApplicationContext());
								 aC.logout(pinLockActivity, megaApi);
						     }
						  }.start();
					}
					else{

						att.setAttemps(attemps);
//						dbH.setAttributes(att);
						dbH.setAttrAttemps(attemps);

						String message = null;
		            	if(attemps<5){
		            		message = getString(R.string.pin_lock_incorrect);
		            		warningLayout.setVisibility(View.INVISIBLE);
		            	}
		            	else{
							message = getResources().getQuantityString(R.plurals.pin_lock_incorrect_alert,
									MAX_ATTEMPS-attemps, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}
		            	showSnackbar(message);

			            //Re-enter pass
			            passFirstLetter.setText("");
			            passSecondLetter.setText("");
			            passThirdLetter.setText("");
			            passFourthLetter.setText("");

			            if(passFifthLetter!=null){
			            	passFifthLetter.setText("");
			            }
			            if(passSixthLetter!=null){
			            	passSixthLetter.setText("");
			            }

			            passFirstLetter.requestFocus();
			            passFirstLetter.setCursorVisible(true);
			            sbFirst.setLength(0);
			            sbSecond.setLength(0);
			            unlockText.setText(R.string.unlock_pin_title);
					}
				}
				break;
			}
			case SET:{
				setPin(code);
				break;
			}
			case RESET_UNLOCK:{
				logDebug("case RESET_UNLOCK");
				String codePref = prefs.getPinLockCode();

				if (code.compareTo(codePref) == 0){
					//Old PIN OK
					PinUtil.update();

					attemps = 0;
					att.setAttemps(attemps);
					dbH.setAttrAttemps(attemps);

					//Ask for code type
					this.choosePinLockType();

				}
				else{
					logWarning("PIN INCORRECT RESET_UNLOCK - show snackBar");
					attemps=attemps+1;
					att.setAttemps(attemps);
//						dbH.setAttributes(att);
					dbH.setAttrAttemps(attemps);
					if(attemps==10){
						//Log out!!
						logDebug("INTENTS==9 - LOGOUT");
						passFirstLetter.setCursorVisible(false);

						if(passSixthLetter!=null){
				    		passSixthLetter.setCursorVisible(false);
				    	}
				    	else{
				    		passFourthLetter.setCursorVisible(false);
				    	}

						imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
						//						Intent intent = new Intent(this, IncorrectPinActivityLollipop.class);
						//						startActivity(intent);
						//						finish();
						CountDownTimer cDT = new CountDownTimer(6000, 1000) {

						     public void onTick(long millisUntilFinished) {
//						         mTextField.setText("seconds remaining: " + );
						    	 redLayout.setVisibility(View.VISIBLE);
						    	 textLogout.setText(getString(R.string.incorrect_pin_activity, millisUntilFinished / 1000));
						     }

						     public void onFinish() {
								 logWarning("Logout!!!");
								 attemps=0;
								 att.setAttemps(attemps);
//						dbH.setAttributes(att);
								 dbH.setAttrAttemps(attemps);
								 AccountController accountController = new AccountController(getApplicationContext());
								 accountController.logout(pinLockActivity, megaApi);
						     }
						  }.start();
					}
					else{

						att.setAttemps(attemps);
						dbH.setAttrAttemps(attemps);

		            	String message = null;

		            	if(attemps<5){
		            		message = getString(R.string.pin_lock_incorrect);
		            		warningLayout.setVisibility(View.INVISIBLE);
		            	}
		            	else{
		            		message = getResources().getQuantityString(R.plurals.pin_lock_incorrect_alert,
									MAX_ATTEMPS-attemps, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}
			        	showSnackbar(message);

			            //Re-enter pass
			            passFirstLetter.setText("");
			            passSecondLetter.setText("");
			            passThirdLetter.setText("");
			            passFourthLetter.setText("");

			            if(passFifthLetter!=null){
			            	passFifthLetter.setText("");
			            }
			            if(passSixthLetter!=null){
			            	passSixthLetter.setText("");
			            }

			            passFirstLetter.requestFocus();
			            passFirstLetter.setCursorVisible(true);
			            sbFirst.setLength(0);
			            sbSecond.setLength(0);
			            unlockText.setText(R.string.unlock_pin_title);
					}
				}
				break;
			}
			case RESET_SET:{
				logDebug("case RESET_SET");
				setPin(code);
				break;
			}
		}

	}

	/*
	 * Validate Pin code
	 */
	private void submitFormAlphanumeric(String code) {
//		String code = sbSecond
		chosenTypePin = Constants.PIN_ALPHANUMERIC;
		switch(mode){
			case UNLOCK:{
				String codePref = prefs.getPinLockCode();

				if (code.compareTo(codePref) == 0){
					PinUtil.update();
					attemps=0;
					att.setAttemps(attemps);
					dbH.setAttrAttemps(attemps);
					finish();
				}
				else{
					logWarning("PIN INCORRECT RESET_UNLOCK - show snackBar");
					attemps=attemps+1;
					att.setAttemps(attemps);
//						dbH.setAttributes(att);
					dbH.setAttrAttemps(attemps);
					if(attemps==10){
						//Log out!!
						logDebug("INTENTS==10 - LOGOUT");
				    	redLayout.setVisibility(View.VISIBLE);
				    	textLogout.setText(getString(R.string.incorrect_pin_activity, 5));

						passwordText.setCursorVisible(false);
						passwordText.clearFocus();
						hideKeyboard(this);

						CountDownTimer cDT = new CountDownTimer(6000, 1000) {

						     public void onTick(long millisUntilFinished) {
						    	 redLayout.setVisibility(View.VISIBLE);
						    	 textLogout.setText(getString(R.string.incorrect_pin_activity, millisUntilFinished / 1000));
						     }

						     public void onFinish() {
								 logWarning("Logout!!!");
								 attemps=0;
								 att.setAttemps(attemps);
//						dbH.setAttributes(att);
								 dbH.setAttrAttemps(attemps);
								 AccountController accountController = new AccountController(getApplicationContext());
								 accountController.logout(pinLockActivity, megaApi);
						     }
						  }.start();
					}
					else{

						att.setAttemps(attemps);
//						dbH.setAttributes(att);
						dbH.setAttrAttemps(attemps);

						String message = null;
		            	if(attemps<5){
		            		message = getString(R.string.pin_lock_incorrect);
		            		warningLayout.setVisibility(View.INVISIBLE);
		            	}
		            	else{
		            		message = getResources().getQuantityString(R.plurals.pin_lock_incorrect_alert,
									MAX_ATTEMPS-attemps, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}

			        	showSnackbar(message);

			            //Re-enter pass
			        	passwordText.setText("");

			        	passwordText.requestFocus();
			        	passwordText.setCursorVisible(true);
			            sbFirst.setLength(0);
			            sbSecond.setLength(0);
			            unlockText.setText(R.string.unlock_pin_title);
					}
				}
				break;
			}
			case SET:{
				setPin(code);
				break;
			}
			case RESET_UNLOCK:{
				logDebug("case RESET_UNLOCK");
				String codePref = prefs.getPinLockCode();

				if (code.compareTo(codePref) == 0){
					//Old PIN OK
					PinUtil.update();

					attemps = 0;
					att.setAttemps(attemps);
					dbH.setAttrAttemps(attemps);

					//Ask for code type
					this.choosePinLockType();

				}
				else{
					logWarning("PIN INCORRECT RESET_UNLOCK - show snackBar");
					attemps=attemps+1;
					att.setAttemps(attemps);
//						dbH.setAttributes(att);
					dbH.setAttrAttemps(attemps);
					if(attemps==10){
						//Log out!!
						logDebug("INTENTS==9 - LOGOUT");

						//						Intent intent = new Intent(this, IncorrectPinActivityLollipop.class);
						//						startActivity(intent);
						//						finish();

						passwordText.setCursorVisible(false);
						passwordText.clearFocus();
						hideKeyboard(this);

						CountDownTimer cDT = new CountDownTimer(6000, 1000) {

						     public void onTick(long millisUntilFinished) {
//						         mTextField.setText("seconds remaining: " + );
						    	 redLayout.setVisibility(View.VISIBLE);
						    	 textLogout.setText(getString(R.string.incorrect_pin_activity, millisUntilFinished / 1000));
						     }

						     public void onFinish() {
								 logWarning("Logout!!!");
								 attemps=0;
								 att.setAttemps(attemps);
//						dbH.setAttributes(att);
								 dbH.setAttrAttemps(attemps);
								 AccountController accountController = new AccountController(getApplicationContext());
								 accountController.logout(pinLockActivity, megaApi);
						     }
						  }.start();
					}
					else{

						att.setAttemps(attemps);
						dbH.setAttrAttemps(attemps);

		            	String message = null;

		            	if(attemps<5){
		            		message = getString(R.string.pin_lock_incorrect);
		            		warningLayout.setVisibility(View.INVISIBLE);
		            	}
		            	else{
		            		message = getResources().getQuantityString(R.plurals.pin_lock_incorrect_alert,
									MAX_ATTEMPS-attemps, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}
			        	showSnackbar(message);

			            //Re-enter pass
			        	passwordText.setText("");

			        	passwordText.requestFocus();
			        	passwordText.setCursorVisible(true);
			            sbFirst.setLength(0);
			            sbSecond.setLength(0);
			            unlockText.setText(R.string.unlock_pin_title);
					}
				}
				break;
			}
			case RESET_SET:{
				logDebug("case RESET_SET");
				setPin(code);
				break;
			}
		}

	}

	private void choosePinLockType(){
		logDebug("setPinLock");

		AlertDialog setPinDialog;
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.choose_pin_type_dialog, null);

		final CheckedTextView pin4Check = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_4_check);
		pin4Check.setText(getString(R.string.four_pin_lock));
		pin4Check.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
		pin4Check.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pin4MLP = (ViewGroup.MarginLayoutParams) pin4Check.getLayoutParams();
		pin4MLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
		pin4Check.setChecked(true);

		final CheckedTextView pin6Check = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_6_check);
		pin6Check.setText(getString(R.string.six_pin_lock));
		pin6Check.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
		pin6Check.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pin6MLP = (ViewGroup.MarginLayoutParams) pin6Check.getLayoutParams();
		pin6MLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView pinANCheck = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_alphaN_check);
		pinANCheck.setText(getString(R.string.AN_pin_lock));
		pinANCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
		pinANCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pinANMLP = (ViewGroup.MarginLayoutParams) pinANCheck.getLayoutParams();
		pinANMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setView(dialoglayout);
		builder.setTitle(getString(R.string.pin_lock_type));

		setPinDialog = builder.create();
		setPinDialog.show();

		final AlertDialog dialog = setPinDialog;

		pin4Check.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				dbH.setPinLockType(Constants.PIN_4);
    			if (dialog != null){
    				dialog.dismiss();
    			}
    			modeSetResetOn(Constants.PIN_4);
			}
		});

		pin6Check.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				pin6Check.setChecked(true);
				pin4Check.setChecked(false);

//				dbH.setPinLockType(Constants.PIN_6);
    			if (dialog != null){
    				dialog.dismiss();
    			}
    			modeSetResetOn(Constants.PIN_6);
			}
		});

		pinANCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				pinANCheck.setChecked(true);
				pin4Check.setChecked(false);
//				dbH.setPinLockType(Constants.PIN_ALPHANUMERIC);
    			if (dialog != null){
    				dialog.dismiss();
    			}
    			modeSetResetOn(Constants.PIN_ALPHANUMERIC);
			}
		});

	}

	public void modeSetResetOn(String type)	{
		logDebug("modeSetResetOn");
		chosenTypePin = type;
		mode=RESET_SET;

		if(type.equals(Constants.PIN_4)){
			logDebug("4 PIN");
			if(sixPinLayout!=null){
				sixPinLayout.setVisibility(View.GONE);
			}
			add4DigitsPin();

			//Re-enter pass
	        passFirstLetter.setText("");
	        passSecondLetter.setText("");
	        passThirdLetter.setText("");
	        passFourthLetter.setText("");

	        passFirstLetter.requestFocus();
	        passFirstLetter.setCursorVisible(true);
		}
		else if(type.equals(Constants.PIN_6)){
			logDebug("6 PIN");
			add6DigitsPin();

			//Re-enter pass
	        passFirstLetter.setText("");
	        passSecondLetter.setText("");
	        passThirdLetter.setText("");
	        passFourthLetter.setText("");

	        if(passFifthLetter!=null){
	        	passFifthLetter.setText("");
	        }
	        if(passSixthLetter!=null){
	        	passSixthLetter.setText("");
	        }

	        passFirstLetter.requestFocus();
	        passFirstLetter.setCursorVisible(true);
		}
		else{
			logDebug("AN PIN");
			addAlphanumericPin();

			if(passwordText!=null){
				passwordText.setText("");
	        }
		}

        sbFirst.setLength(0);
        sbSecond.setLength(0);
        unlockText.setText(R.string.reset_pin_title);
	}


	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");
		if(attemps<10){
			logDebug("attemps<10");
			switch(mode){
				case UNLOCK:{
					moveTaskToBack(true);
					break;
				}
				case RESET_UNLOCK:
				case RESET_SET:
					MegaApplication.setShowPinScreen(false);
				default:
					finish();
			}
		}
		else{
			logWarning("attemps MORE 10");
			moveTaskToBack(false);
		}
	}

	@Override
	protected void onUserLeaveHint() {
		if (mode != UNLOCK) {
			finish();
		}
	}


	public int getMode() {
		return mode;
	}


	public void setMode(int mode) {
		this.mode = mode;
	}


	@Override
	public void onClick(View v) {
		logDebug("onClick");
		switch(v.getId()){
			case R.id.button_logout:{
				attemps=0;
				att.setAttemps(attemps);
//						dbH.setAttributes(att);
				dbH.setAttrAttemps(attemps);
				AccountController aC = new AccountController(this);
				aC.logout(this, megaApi);
				break;
			}
			case R.id.button_enter:{
				checkPasswordText();
				break;
			}
		}
	}

	public void checkPasswordText() {
		logDebug("checkPasswordText");

    	if(passwordText.length()!=0){
    		passwordText.setCursorVisible(false);
    		passwordText.requestFocus();
            if(!secondRound)
            {
            	sbFirst.append(passwordText.getText());
				switch(mode){
                    case RESET_SET:
                    {
                    	//Re-enter pass
                    	passwordText.setText("");

                    	passwordText.requestFocus();
                    	passwordText.setCursorVisible(true);
                    	unlockText.setText(R.string.reset_pin_title_2);
                    	secondRound=true;
                    	break;
                    }
                    case UNLOCK:
                    case RESET_UNLOCK:
                    {
                    	submitFormAlphanumeric(sbFirst.toString());
                    	break;
                    }
                    default:
                    {
                    	//Re-enter pass
						logDebug("Alphanumeric Default CASE");
                    	passwordText.setText("");

                        passwordText.requestFocus();
                        passwordText.setCursorVisible(true);
                    	unlockText.setText(R.string.unlock_pin_title_2);
                    	secondRound=true;
                    	break;
                    }
                }

            }
            else if(secondRound)
            {
				logDebug("SECOND TIME alphanumeric");
            	sbSecond.append(passwordText.getText());

                submitForm(sbSecond.toString());
                if(sbFirst.toString().equals(sbSecond.toString())){
					logDebug("Alphanumeric PIN match - submit form");
                	submitFormAlphanumeric(sbSecond.toString());
                }
                else{
					logWarning("Alphanumeric PIN NOT match - show snackBar");
                	secondRound = false;
                	showSnackbar(getString(R.string.pin_lock_not_match));

                    //Re-enter pass
                	passwordText.setText("");

                    passwordText.requestFocus();
                    passwordText.setCursorVisible(true);
                    sbFirst.setLength(0);
                    sbSecond.setLength(0);
                    if(getMode()==RESET_SET){
                    	unlockText.setText(R.string.reset_pin_title);
                    }
                    else{
                    	unlockText.setText(R.string.unlock_pin_title);
                    }
                }
            }
    	}
    }

	public void hideKeyboard(Activity activity) {

		enterButton.requestFocus();
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = activity.getCurrentFocus();
		if(view!=null){
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	protected void onPause() {
		logDebug("onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		logDebug("onResume");
		Util.setAppFontSize(this);
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		logDebug("onDestroy");

		MegaApplication.setShowPinScreen(isFinishing() ? true : false);

		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		logDebug("onSaveInstanceState");
		super.onSaveInstanceState(outState);

		outState.putInt("mode", mode);
		outState.putString("chosenTypePin", chosenTypePin);
		outState.putBoolean("isSecondRound", secondRound);
		if (secondRound) {
			outState.putString("sbFirstString", sbFirst.toString());
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
		if (request.getType() == MegaRequest.TYPE_LOGOUT) {
			logDebug("END logout sdk request - wait chat logout");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}
}
