package nz.mega.android.lollipop;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.PinUtil;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class PinLockActivityLollipop extends AppCompatActivity{
	
	public static String ACTION_SET_PIN_LOCK = "ACTION_SET";
	public static String ACTION_RESET_PIN_LOCK = "ACTION_RESET";
	
	final public static int SET = 0;
	final public static int UNLOCK = 1;
	final public static int RESET_UNLOCK = 2;
	final public static int RESET_SET = 3;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	MegaApiAndroid megaApi;
    LinearLayout fragmentContainer;
	TextView unlockText;
	EditText passFirstLetter;
	EditText passSecondLetter;
	EditText passThirdLetter;
	EditText passFourthLetter;
	final StringBuilder sbFirst=new StringBuilder();
	final StringBuilder sbSecond=new StringBuilder();
	
	int mode = UNLOCK;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
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
		
		fragmentContainer=(LinearLayout) findViewById(R.id.fragment_container_pin_lock);
		unlockText = (TextView) findViewById(R.id.unlock_text_view);
		unlockText.setText(R.string.unlock_pin_title);		
		unlockText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (24*scaleText));
		//Left margin
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)unlockText.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(100, outMetrics), 0, Util.scaleHeightPx(20, outMetrics)); 
		unlockText.setLayoutParams(textParams);
		
		//PIN
		passFirstLetter = (EditText) findViewById(R.id.pass_first);
		passFirstLetter.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = passFirstLetter.getLayoutParams();		
		paramsb1.width = Util.scaleWidthPx(40, outMetrics);		
		passFirstLetter.setLayoutParams(paramsb1);	
		//Margins
		textParams = (LinearLayout.LayoutParams)passFirstLetter.getLayoutParams();
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
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(passFirstLetter, InputMethodManager.SHOW_FORCED);
//		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

		//Add TextWatcher to first letter		
		passFirstLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sbFirst.length()==0 & passFirstLetter.length()==1)
                {
                	sbFirst.append(s);
                    log("onTextChanged1: sb: "+sbFirst+" s: "+s+"//");
//                    passFirstLetter.clearFocus();
                    passSecondLetter.requestFocus();
                    passSecondLetter.setCursorVisible(true);

                }
                else if(sbSecond.length()==0 & passFirstLetter.length()==1)
                {
                	sbSecond.append(s);
                    log("onTextChanged1: sb: "+sbSecond+" s: "+s+"//");
//                    passFirstLetter.clearFocus();
                    passSecondLetter.requestFocus();
                    passSecondLetter.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {}
        });	
		
		
		passSecondLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sbFirst.length()==1 & passSecondLetter.length()==1)
                {
                	sbFirst.append(s);
                    log("onTextChanged2: sb: "+sbFirst+" s: "+s+"//");
                    passSecondLetter.clearFocus();
                    passThirdLetter.requestFocus();
                    passThirdLetter.setCursorVisible(true);

                }    
                else if(sbSecond.length()==1 & passSecondLetter.length()==1)
                {
                	sbSecond.append(s);
                    log("onTextChanged2: sb: "+sbSecond+" s: "+s+"//");
                    passSecondLetter.clearFocus();
                    passThirdLetter.requestFocus();
                    passThirdLetter.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {}
        });	
		
		passThirdLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sbFirst.length()==2 & passThirdLetter.length()==1)
                {
                	sbFirst.append(s);
                    log("onTextChanged3: sb: "+sbFirst+" s: "+s+"//");
                    passThirdLetter.clearFocus();
                    
                    passFourthLetter.requestFocus();
                    passFourthLetter.setCursorVisible(true);

                }
                else if(sbSecond.length()==2 & passThirdLetter.length()==1)
                {
                	sbSecond.append(s);
                    log("onTextChanged3: sb: "+sbSecond+" s: "+s+"//");
                    passThirdLetter.clearFocus();
                    
                    passFourthLetter.requestFocus();
                    passFourthLetter.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {}
        });	
		
		passFourthLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sbFirst.length()==3 & passFourthLetter.length()==1)
                {
                	sbFirst.append(s);
                    log("onTextChanged4: sb: "+sbFirst+" s: "+s+"//");                    
                    
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
	                        passFirstLetter.setText("");
	                        passSecondLetter.setText("");
	                        passThirdLetter.setText("");
	                        passFourthLetter.setText("");
	                        
	                        passFirstLetter.requestFocus();
	                        passFirstLetter.setCursorVisible(true);
	                    	unlockText.setText(R.string.unlock_pin_title_2);
	                    	break;
	                    }
                    }      

                }
                else if(sbSecond.length()==3 & passFourthLetter.length()==1)
                {
                	log("SECOND TIME 4thletter");
                	sbSecond.append(s);
                    log("onTextChanged3: sb: "+sbSecond+" s: "+s+"//");

                    //Action to set/enter
                    log("El pin del primer intento: "+sbFirst+" el pin del segundo intento: "+sbSecond);
                    
                    if(sbFirst.toString().equals(sbSecond.toString())){
                    	log("PIN match - submit form");
                    	submitForm(sbSecond.toString());
                    }
                    else{
                    	log("PIN NOT match - show snackBar");
//                    	Snackbar.make(, , Snackbar.LENGTH_LONG).show();
                    	Snackbar snack = Snackbar.make(fragmentContainer, getString(R.string.pin_lock_not_match), Snackbar.LENGTH_LONG);
                    	View view = snack.getView();
                    	FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
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
                        if(getMode()==RESET_UNLOCK){
                        	unlockText.setText(R.string.reset_pin_title);
                        }
                        else{
                        	unlockText.setText(R.string.unlock_pin_title);
                        }       	
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void afterTextChanged(Editable s) {}
        });	
	}

	
	private void setPin(String pin){
		log("setPin");
		
		if ( pin != null){			
			dbH.setPinLockCode(pin);
		}
		PinUtil.update();
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
					finish();
				}
				else{
					log("PIN INCORRECT - show snackBar");
//		        	Snackbar.make(, , Snackbar.LENGTH_LONG).show();
		        	Snackbar snack = Snackbar.make(fragmentContainer, getString(R.string.pin_lock_incorrect), Snackbar.LENGTH_LONG);
		        	View view = snack.getView();
		        	FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
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
					log("PIN INCORRECT - show snackBar");
//		        	Snackbar.make(, , Snackbar.LENGTH_LONG).show();
		        	Snackbar snack = Snackbar.make(fragmentContainer, getString(R.string.pin_lock_incorrect), Snackbar.LENGTH_LONG);
		        	View view = snack.getView();
		        	FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
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
        moveTaskToBack(true);
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
}
