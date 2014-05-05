package com.mega.android;

import com.mega.components.LoopViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TourActivity extends Activity implements OnClickListener {
	
	
	private TourImageAdapter adapter;
	private LoopViewPager viewPager;
	private ImageView bar;
	private Button bRegister;
	private Button bLogin;
	private LinearLayout tourLoginCreate;
	int heightGrey = 0;
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tour);
		viewPager = (LoopViewPager) findViewById(R.id.pager);
		bar = (ImageView) findViewById(R.id.barTour);
		bRegister = (Button) findViewById(R.id.button_register_tour);
		bLogin = (Button) findViewById(R.id.button_login_tour);
		
		bRegister.setOnClickListener(this);
		bLogin.setOnClickListener(this);
		
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
														bar.setImageResource(barImages[position]);
													}
												});
	    
//	    String cadena;
//	    cadena = "Density: " + density + "_Width: " + dpWidth + "_Height: " + dpHeight + "_heightPix" + outMetrics.heightPixels;
//	    Toast.makeText(this, cadena, Toast.LENGTH_LONG).show();
	    
	    tourLoginCreate = (LinearLayout) findViewById(R.id.tour_login_create);
	}	
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
//		Toast.makeText(this, "onWindow: HEIGHT: " + tourLoginCreate.getTop() +"____" + heightGrey, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
		case R.id.button_register_tour:
			onRegisterClick(v);
			break;
		case R.id.button_login_tour:
			onLoginClick(v);
			break;
		}
	}

	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onLoginClick(View v){
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	public static void log(String message) {
		Util.log("TourActivity", message);
	}
}
