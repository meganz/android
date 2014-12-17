package nz.mega.android;

import java.io.File;

import nz.mega.android.utils.Util;
import nz.mega.components.LoopViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;


public class InitialCamSyncActivity extends Activity implements OnClickListener {
	
	
	private TourImageAdapter adapter;
	private LoopViewPager viewPager;
//	private ImageView bar;
	private Button bOK;
	private Button bSkip;
//	private LinearLayout camSyncWifiLayout;
	private RadioGroup camSyncRadioGroup;
	private RadioButton camSyncData;
	private RadioButton camSyncWifi;
	
	int heightGrey = 0;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cam_sync_initial);
//		viewPager = (LoopViewPager) findViewById(R.id.cam_sync_pager);
//		bar = (ImageView) findViewById(R.id.cam_sync_bar);
		bOK = (Button) findViewById(R.id.cam_sync_button_ok);
		bSkip = (Button) findViewById(R.id.cam_sync_button_skip);
		
		bOK.setOnClickListener(this);
		bSkip.setOnClickListener(this);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
//	    ((LinearLayout.LayoutParams)bOK.getLayoutParams()).width = Util.px2dp((150*scaleW), outMetrics);
//	    ((LinearLayout.LayoutParams)bSkip.getLayoutParams()).width = Util.px2dp((150*scaleW), outMetrics);
//		((LinearLayout.LayoutParams)bOK.getLayoutParams()).setMargins(Util.px2dp((20*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((20*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics));
//		((LinearLayout.LayoutParams)bSkip.getLayoutParams()).setMargins(Util.px2dp((0*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((20*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics));
		
		adapter = new TourImageAdapter(this);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(0);
		
	    Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
	    float dpHeight = outMetrics.heightPixels / density;
	    float dpWidth  = outMetrics.widthPixels / density;
	    
	    heightGrey = (int) (Util.percScreenLogin * outMetrics.heightPixels);
	    
	    viewPager.getLayoutParams().height = Util.px2dp((350*scaleH), outMetrics);
		
//		viewPager.getLayoutParams().width = screenWidth;
//		viewPager.getLayoutParams().height = screenWidth;

		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
													
													@Override
													public void onPageSelected (int position){
														int[] barImages = new int[] {
														        R.drawable.tour01_bar,
														        R.drawable.tour02_bar,
														        R.drawable.tour03_bar,
														        R.drawable.tour04_bar
														    };
//														bar.setImageResource(barImages[position]);
													}
												});
	    
//	    String cadena;
//	    cadena = "Density: " + density + "_Width: " + dpWidth + "_Height: " + dpHeight + "_heightPix" + outMetrics.heightPixels;
//	    Toast.makeText(this, cadena, Toast.LENGTH_LONG).show();
		
		camSyncRadioGroup = (RadioGroup) findViewById(R.id.cam_sync_radio_group);
		camSyncData = (RadioButton) findViewById(R.id.cam_sync_data);
		camSyncWifi = (RadioButton) findViewById(R.id.cam_sync_wifi);
		
//		((RadioGroup.LayoutParams)camSyncRadioGroup.getLayoutParams()).setMargins(Util.px2dp((20*scaleW), outMetrics), 0, 0, Util.px2dp((20*scaleH), outMetrics));
//		((RadioGroup.LayoutParams)camSyncData.getLayoutParams()).setMargins(0, Util.px2dp((10*scaleH), outMetrics), 0, Util.px2dp((15*scaleH), outMetrics));
		
//		camSyncWifiLayout = (LinearLayout) findViewById(R.id.cam_sync_wifi_layout);
//		camSyncWifiLayout.setPadding(0, Util.px2dp((10*scaleH), outMetrics), 0, Util.px2dp((20*scaleH), outMetrics));
	}	
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
//		Toast.makeText(this, "onWindow: HEIGHT: " + tourLoginCreate.getTop() +"____" + heightGrey, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onClick(View v) {

//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		Intent intent = new Intent(this,ManagerActivity.class);
		switch(v.getId()){
			case R.id.cam_sync_button_ok:{
				setInitialPreferences();
				dbH.setCamSyncEnabled(true);
				File localFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
				String localPath = localFile.getAbsolutePath();
				dbH.setCamSyncLocalPath(localPath);
				if (camSyncData.isChecked()){
					dbH.setCamSyncWifi(false);
				}
				else{
					dbH.setCamSyncWifi(true);
				}
				dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
				
				startService(new Intent(getApplicationContext(), CameraSyncService.class));
	
				startActivity(intent);
				finish();
				break;
			}
			case R.id.cam_sync_button_skip:{
				setInitialPreferences();
				dbH.setCamSyncEnabled(false);
				startActivity(intent);
				finish();
				break;
			}
		}
	}

	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void setInitialPreferences(){
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		dbH.setFirstTime(false);
		dbH.setStorageAskAlways(false);
		File defaultDownloadLocation = null;
		if (Environment.getExternalStorageDirectory() != null){
			defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
		}
		else{
			defaultDownloadLocation = getFilesDir();
		}
		
		defaultDownloadLocation.mkdirs();
		
		dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
		dbH.setPinLockEnabled(false);
		dbH.setPinLockCode("");
	}

	public static void log(String message) {
		Util.log("InitialCamSyncActivity", message);
	}
}
