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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class PinLockActivityLollipop extends AppCompatActivity implements OnClickListener{
	
	public static String ACTION_SET_PIN_LOCK = "ACTION_PICK_MOVE_FOLDER";
	
	public static int SET = 0;
	public static int UNLOCK = 1;
	
	MegaApiAndroid megaApi;

	Button enterPinButton;
	EditText pinCodeText;
	EditText passFirstLetter;
	EditText passSecondLetter;
	EditText passThirdLetter;
	EditText passFourthLetter;
	
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
		}
		
		setContentView(R.layout.activity_pin_lock);
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
//		dbH = new DatabaseHandler(this);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		prefs = dbH.getPreferences();

		//PIN
		passFirstLetter = (EditText) findViewById(R.id.pass_first);
		passSecondLetter = (EditText) findViewById(R.id.pass_second);
		passSecondLetter.setInputType(InputType.TYPE_CLASS_NUMBER);
//		passSecondLetter.setTransformationMethod(new PasswordTransformationMethod());
		
		passThirdLetter = (EditText) findViewById(R.id.pass_third);
		passFourthLetter = (EditText) findViewById(R.id.pass_fourth);
		
		passFirstLetter.requestFocus();
		
		//Add TextWatcher to first letter
		final StringBuilder sb=new StringBuilder();
		
		
		passFirstLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sb.length()==0 & passFirstLetter.length()==1)
                {
                    sb.append(s);
                    passFirstLetter.setText("X");
                    log("onTextChanged1: sb: "+sb+" s: "+s+"//");
//                    passFirstLetter.clearFocus();
                    passSecondLetter.requestFocus();
                    passSecondLetter.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

            public void afterTextChanged(Editable e) {
            	if (e.length() == 1) {
//            		passFirstLetter.clearFocus();
                   
                }

            }
        });	
		
		
		passSecondLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sb.length()==1 & passSecondLetter.length()==1)
                {
                    sb.append(s);
                    log("onTextChanged2: sb: "+sb+" s: "+s+"//");
                    passSecondLetter.setText("X");
                    passSecondLetter.clearFocus();
                    passThirdLetter.requestFocus();
                    passThirdLetter.setCursorVisible(true);

                }                
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {


            }

            public void afterTextChanged(Editable e) {

            		passSecondLetter.clearFocus();
                   
                


            }
        });	
		
		passThirdLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sb.length()==2 & passThirdLetter.length()==1)
                {
                    sb.append(s);
                    log("onTextChanged3: sb: "+sb+" s: "+s+"//");
                    passThirdLetter.setText("X");
                    passThirdLetter.clearFocus();
                    
                    passFourthLetter.requestFocus();
                    passFourthLetter.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

              
            }

            public void afterTextChanged(Editable s) {
//                if(sb.length()==0)
//                {
//
//                	passThirdLetter.requestFocus();
            	passThirdLetter.clearFocus();

            }
        });	
		
		passFourthLetter.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(sb.length()==3 & passFourthLetter.length()==1)
                {
                    sb.append(s);
                    log("onTextChanged4: sb: "+sb+" s: "+s+"//");
                    passFourthLetter.clearFocus();
                    //Action to set/enter

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

//                if(sb.length()==1)
//                {
//
//                    sb.deleteCharAt(0);
//
//                }

            }

            public void afterTextChanged(Editable s) {

            }
        });	
		
		
		
		enterPinButton = (Button) findViewById(R.id.pin_lock_enter_pin);
		enterPinButton.setOnClickListener(this);
		pinCodeText = (EditText) findViewById(R.id.pin_lock_pin_code);
		pinCodeText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					submitForm();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.pin_lock_enter_pin:{
				if(mode==UNLOCK){
					submitForm();
				}
				else if(mode==SET){
					setPin();
				}
				
				break;
			}
		}
	}
	
	private void setPin(){
		log("setPin");
		String pin = pinCodeText.getText().toString(); 
		if ( pin != null){
			log("El pin es... "+pinCodeText.getText());
			dbH.setPinLockCode(pin);
		}
		PinUtil.update();
		finish();
	}
	
	/*
	 * Validate Pin code
	 */
	private void submitForm() {
		String code = pinCodeText.getText().toString();
		
		String codePref = prefs.getPinLockCode();
		
		if (code.compareTo(codePref) == 0){
			PinUtil.update();
			finish();
		}
		else{
			pinCodeText.setError(getString(R.string.pin_lock_incorrect));
			pinCodeText.requestFocus();
		}
	}
	
	@Override
	public void onBackPressed() {
        moveTaskToBack(true);
	}
	
	public static void log(String message) {
		Util.log("PinLockActivityLollipop", message);
	}
}
