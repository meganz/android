package com.mega;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class TourActivity extends Activity {
	
	private TourImageAdapter adapter;
	private ViewPager viewPager;
	private ImageView bar;
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tour);
		viewPager = (ViewPager) findViewById(R.id.pager);
		bar = (ImageView) findViewById(R.id.barTour);
		
		adapter = new TourImageAdapter(this);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(0);
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		int screenWidth = 0;
		int screenHeight = 0;
		
		//EL CAMBIO DE ORIENTACION NO FUNCIONA
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
        	display.getSize(size);
        	screenWidth = size.x;
        	screenHeight = size.y;
		}
		else{
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
		}
		
		viewPager.getLayoutParams().width = screenWidth;
		viewPager.getLayoutParams().height = screenWidth;

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
}
