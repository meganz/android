package nz.mega.android.lollipop;

import nz.mega.android.ManagerActivity;
import nz.mega.android.MegaApplication;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IncorrectPinActivityLollipop extends Activity{

	Handler handler = new Handler();
	MegaApiAndroid megaApi;
	TextView text;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
		
		setContentView(R.layout.activity_incorrect_pin);	
		text = (TextView) findViewById(R.id.alert_text);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		//Margins
		LinearLayout.LayoutParams textWarningParams = (LinearLayout.LayoutParams)text.getLayoutParams();
		textWarningParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics), 0); 
		text.setLayoutParams(textWarningParams);
		
	    handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Logout!!!");
				ManagerActivity.logout(getApplication(), megaApi, false);
				finish();
			}
		}, 5 * 1000);
	
	}
	
	public static void log(String message) {
		Util.log("IncorrectPinActivityLollipop", message);
	}
}
