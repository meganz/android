package mega.privacy.android.app.lollipop;

import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.ManagerActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.PinUtil;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


@SuppressLint("NewApi")
public class PinLockActivityLollipop extends AppCompatActivity implements OnClickListener{
	
	public static String ACTION_SET_PIN_LOCK = "ACTION_SET";
	public static String ACTION_RESET_PIN_LOCK = "ACTION_RESET";
	
	final public static int MAX_ATTEMPS = 10;
	final public static int SET = 0;
	final public static int UNLOCK = 1;
	final public static int RESET_UNLOCK = 2;
	final public static int RESET_SET = 3;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	CoordinatorLayout coordinatorLayout;
	MegaApiAndroid megaApi;
	RelativeLayout fragmentContainer;
    LinearLayout pinLayout;
    LinearLayout warningLayout;
    RelativeLayout redLayout;
	TextView textLogout;
	TextView logoutButton;
	TextView unlockText;
	TextView warningText;
	EditText passFirstLetter;
	EditText passSecondLetter;
	EditText passThirdLetter;
	EditText passFourthLetter;
	final StringBuilder sbFirst=new StringBuilder();
	final StringBuilder sbSecond=new StringBuilder();
	boolean secondRound = false;
	InputMethodManager imm;
	
	int mode = UNLOCK;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	MegaAttributes att = null;
	
	int attemps = 0;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if ((intent != null) && (intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_SET_PIN_LOCK)){
				mode=SET;
			}
			else if(intent.getAction().equals(ACTION_RESET_PIN_LOCK)){
				mode=RESET_UNLOCK;
			}
		}

		setContentView(R.layout.activity_pin_lock);
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
	    float scaleText;
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
		
//		dbH = new DatabaseHandler(this);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		prefs = dbH.getPreferences();
		att = dbH.getAttributes();
		attemps = att.getAttemps();
		log("onCreate Attemps number: "+attemps);
		
		fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_pin_lock);
		coordinatorLayout = (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout);
		android.view.ViewGroup.LayoutParams coordinatorLayoutParams = coordinatorLayout.getLayoutParams();		
		coordinatorLayoutParams.height = Util.scaleHeightPx(70, outMetrics);
		coordinatorLayout.setLayoutParams(coordinatorLayoutParams);		
		
		redLayout = (RelativeLayout) findViewById(R.id.red_layout);
		redLayout.setVisibility(View.GONE);
		
		textLogout = (TextView) findViewById(R.id.alert_text);
		textLogout.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		//Margins
		RelativeLayout.LayoutParams textLogoutParams = (RelativeLayout.LayoutParams)textLogout.getLayoutParams();
		textLogoutParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics), 0); 
		textLogout.setLayoutParams(textLogoutParams);
		
		warningLayout = (LinearLayout) findViewById(R.id.warning_layout);
		//Margins
		RelativeLayout.LayoutParams warningParams = (RelativeLayout.LayoutParams)warningLayout.getLayoutParams();
		warningParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(10, outMetrics), 0); 
		warningLayout.setLayoutParams(warningParams);

		warningText = (TextView) findViewById(R.id.warning_text);
		//Margins
		LinearLayout.LayoutParams textWarningParams = (LinearLayout.LayoutParams)warningText.getLayoutParams();
		textWarningParams.setMargins(Util.scaleWidthPx(3, outMetrics), 0, Util.scaleWidthPx(3, outMetrics), 0); 
		warningText.setLayoutParams(textWarningParams);

		logoutButton = (TextView) findViewById(R.id.button_logout);
		logoutButton.setText(getString(R.string.action_logout).toUpperCase(Locale.getDefault()));
//		android.view.ViewGroup.LayoutParams paramsbLogin = logoutButton.getLayoutParams();		
//		paramsbLogin.height = Util.scaleHeightPx(48, outMetrics);
//		paramsbLogin.width = Util.scaleWidthPx(63, outMetrics);
//		logoutButton.setLayoutParams(paramsbLogin);
		//Margin
		RelativeLayout.LayoutParams textParamsLogin = (RelativeLayout.LayoutParams)logoutButton.getLayoutParams();
		textParamsLogin.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(40, outMetrics), 0, 0); 
		logoutButton.setLayoutParams(textParamsLogin);
		
		logoutButton.setOnClickListener(this);
		
		if(mode!=SET){
			logoutButton.setVisibility(View.VISIBLE);
		}
		else{
			logoutButton.setVisibility(View.GONE);
		}
		
		if (attemps==MAX_ATTEMPS-1){
			//Last intent available!!
			log("last intent: "+attemps);
		}
		else if(attemps>=5){
			//Show alert
			log("attemps less than 5: "+attemps);
			warningLayout.setVisibility(View.VISIBLE);
		}
		else{
			//Hide alert
			log("number of attemps: "+attemps);
			warningLayout.setVisibility(View.INVISIBLE);
		}	
		
		pinLayout = (LinearLayout) findViewById(R.id.pin_layout);
		//Margins
		RelativeLayout.LayoutParams pinParams = (RelativeLayout.LayoutParams)pinLayout.getLayoutParams();
		pinParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(20, outMetrics)); 
		pinLayout.setLayoutParams(pinParams);
		
		unlockText = (TextView) findViewById(R.id.unlock_text_view);
//		unlockText.setGravity(Gravity.CENTER_HORIZONTAL); //NOT WORKING!!!
		unlockText.setText(R.string.unlock_pin_title);		
		unlockText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (24*scaleText));
		//Margins
		RelativeLayout.LayoutParams unlockParams = (RelativeLayout.LayoutParams)unlockText.getLayoutParams();
		unlockParams.setMargins(0, Util.scaleHeightPx(70, outMetrics), 0, Util.scaleHeightPx(20, outMetrics)); 
		unlockText.setLayoutParams(unlockParams);
		
		//PIN
		passFirstLetter = (EditText) findViewById(R.id.pass_first);
		passFirstLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = passFirstLetter.getLayoutParams();		
		paramsb1.width = Util.scaleWidthPx(40, outMetrics);		
		passFirstLetter.setLayoutParams(paramsb1);	
		//Margins
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)passFirstLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(0, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics)); 
		passFirstLetter.setLayoutParams(textParams);
		
		passSecondLetter = (EditText) findViewById(R.id.pass_second);
		passSecondLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = passSecondLetter.getLayoutParams();		
		paramsb2.width = Util.scaleWidthPx(40, outMetrics);		
		passSecondLetter.setLayoutParams(paramsb2);
		//Margins
		textParams = (LinearLayout.LayoutParams)passSecondLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics)); 
		passSecondLetter.setLayoutParams(textParams);
		
		passThirdLetter = (EditText) findViewById(R.id.pass_third);
		passThirdLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = passThirdLetter.getLayoutParams();		
		paramsb3.width = Util.scaleWidthPx(40, outMetrics);		
		passThirdLetter.setLayoutParams(paramsb3);
		//Margins
		textParams = (LinearLayout.LayoutParams)passThirdLetter.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics)); 
		passThirdLetter.setLayoutParams(textParams);
		
		passFourthLetter = (EditText) findViewById(R.id.pass_fourth);
		passFourthLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = passFourthLetter.getLayoutParams();		
		paramsb4.width = Util.scaleWidthPx(40, outMetrics);		
		passFourthLetter.setLayoutParams(paramsb4);
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
            	if(passThirdLetter.length()!=0){
                	passFourthLetter.requestFocus();
                	passFourthLetter.setCursorVisible(true);
                    
                    passFourthLetter.setText("");                    
                }
            }
        });	
		
		passFourthLetter.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
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
                        log("sbFirst: "+sbFirst);
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
    	                    	log("Default CASE");
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
                    	log("SECOND TIME 4thletter");
                    	if(passFirstLetter.length()==1 & passSecondLetter.length()==1 & passThirdLetter.length()==1 & passFourthLetter.length()==1){
                    		sbSecond.append(passFirstLetter.getText());
                    		sbSecond.append(passSecondLetter.getText());
                    		sbSecond.append(passThirdLetter.getText());
                    		sbSecond.append(passFourthLetter.getText());	                    		
                    	}
                        log("sbFirst "+sbFirst);
                        log("sbSecond "+sbSecond);
//                    	submitForm(sbSecond.toString());
                        if(sbFirst.toString().equals(sbSecond.toString())){
                        	log("PIN match - submit form");
                        	submitForm(sbSecond.toString());
                        }
                        else{
                        	log("PIN NOT match - show snackBar");
//                        	Snackbar.make(, , Snackbar.LENGTH_LONG).show();
                        	Snackbar snack = Snackbar.make(coordinatorLayout, getString(R.string.pin_lock_not_match), Snackbar.LENGTH_LONG);
                        	View view = snack.getView();
    			        	CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)view.getLayoutParams();
                        	params.gravity = Gravity.TOP;
                        	view.setLayoutParams(params);
                        	snack.show();
                        	
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
		log("setPin");
		
		if ( pin != null){			
			dbH.setPinLockCode(pin);
		}
		PinUtil.update();
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		log("finish!");
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
					log("PIN INCORRECT RESET_UNLOCK - show snackBar");
					attemps=attemps+1;
					if(attemps==10){
						//Log out!!
						log("INTENTS==10 - LOGOUT");
				    	redLayout.setVisibility(View.VISIBLE);
				    	textLogout.setText(getString(R.string.incorrect_pin_activity, 5));
						passFourthLetter.setCursorVisible(false);
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
						    	 log("Logout!!!");
									ManagerActivity.logout(getApplication(), megaApi, false);
									finish();
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
		            		message = getString(R.string.pin_lock_incorrect_alert, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}
		            	
			        	Snackbar snack = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
			        	View view = snack.getView();
			        	CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)view.getLayoutParams();
			        	params.gravity = Gravity.TOP;
			        	view.setLayoutParams(params);
			        	snack.show();
			        	
			            //Re-enter pass
			            passFirstLetter.setText("");
			            passSecondLetter.setText("");
			            passThirdLetter.setText("");
			            passFourthLetter.setText("");
			            
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
				log("case RESET_UNLOCK");
				String codePref = prefs.getPinLockCode();
				
				if (code.compareTo(codePref) == 0){
					//Old PIN OK
					PinUtil.update();
					
					attemps = 0;
					att.setAttemps(attemps);
					dbH.setAttrAttemps(attemps);
					
					mode=RESET_SET;
					//Enter new PIN
		            passFirstLetter.setText("");
		            passSecondLetter.setText("");
		            passThirdLetter.setText("");
		            passFourthLetter.setText("");
		            
		            passFirstLetter.requestFocus();
		            passFirstLetter.setCursorVisible(true);
		            sbFirst.setLength(0);
		            sbSecond.setLength(0);
		            unlockText.setText(R.string.reset_pin_title);
					
				}
				else{
					log("PIN INCORRECT RESET_UNLOCK - show snackBar");
//		        	Snackbar.make(, , Snackbar.LENGTH_LONG).show();
					attemps=attemps+1;
					if(attemps==10){
						//Log out!!
						log("INTENTS==9 - LOGOUT");
						passFirstLetter.setCursorVisible(false);
						passFourthLetter.setCursorVisible(false);
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
						    	 log("Logout!!!");
									ManagerActivity.logout(getApplication(), megaApi, false);
									finish();
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
		            		message = getString(R.string.pin_lock_incorrect_alert, MAX_ATTEMPS-attemps);
		            		warningLayout.setVisibility(View.VISIBLE);
		            	}
			        	Snackbar snack = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
			        	View view = snack.getView();
			        	CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)view.getLayoutParams();
			        	params.gravity = Gravity.TOP;
			        	view.setLayoutParams(params);
			        	snack.show();
			        	
			            //Re-enter pass
			            passFirstLetter.setText("");
			            passSecondLetter.setText("");
			            passThirdLetter.setText("");
			            passFourthLetter.setText("");
			            
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
				log("case RESET_SET");
				setPin(code);
				break;
			}
		}

	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed");
		if(attemps<10){
			log("attemps<10");
			switch(mode){
				case UNLOCK:{
					moveTaskToBack(true);
					break;
				}
				default:
					finish();
			}
		}
		else{
			log("attemps MORE 10");
			moveTaskToBack(false);
		}
	}
	
	public static void log(String message) {
		Util.log("PinLockActivityLollipop", message);
	}


	public int getMode() {
		return mode;
	}


	public void setMode(int mode) {
		this.mode = mode;
	}


	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.button_logout:
				ManagerActivityLollipop.logout(getApplication(), megaApi, false);
//				Intent intent = new Intent(this, TourActivityLollipop.class);
//				startActivity(intent);
				finish();
				break;
		}
		
	}
}
