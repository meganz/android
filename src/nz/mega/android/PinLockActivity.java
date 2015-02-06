package nz.mega.android;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class PinLockActivity extends ActionBarActivity implements OnClickListener{
	
	MegaApiAndroid megaApi;
	
	Button logoutButton;
	Button enterPinButton;
	EditText pinCodeText;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_pin_lock);
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
//		dbH = new DatabaseHandler(this);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		prefs = dbH.getPreferences();
		
		logoutButton = (Button) findViewById(R.id.pin_lock_logout);
		logoutButton.setOnClickListener(this);
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
				submitForm();
				break;
			}
			case R.id.pin_lock_logout:{
				ManagerActivity.logout(this, megaApi, false);
				break;
			}
		}
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
		Util.log("PinLockActivity", message);
	}
}
