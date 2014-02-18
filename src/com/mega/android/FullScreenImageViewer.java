package com.mega.android;

import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;
import com.mega.components.TouchImageView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class FullScreenImageViewer extends ActionBarActivity implements OnPageChangeListener, OnClickListener{
	
	private ExtendedViewPager viewPager;
	private MegaFullScreenImageAdapter adapter;
//	private ActionBar aB;
	private int positionG;
	private ArrayList<String> names;
	private ArrayList<Integer> imageIds;
	
	private ImageView actionBarIcon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
				
//		aB = getSupportActionBar();
//		aB.hide();
//		aB.setHomeButtonEnabled(true);
//		aB.setDisplayShowTitleEnabled(false);
//		aB.setLogo(R.drawable.ic_action_navigation_accept);
	    
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);
		
		Intent i = getIntent();
		positionG = i.getIntExtra("position", 0);
		
		names = i.getStringArrayListExtra("names");
		imageIds = i.getIntegerArrayListExtra("imageIds");

		adapter = new MegaFullScreenImageAdapter(FullScreenImageViewer.this,imageIds, names);
		
		viewPager.setAdapter(adapter);
		
		viewPager.setCurrentItem(positionG);
		
		viewPager.setOnPageChangeListener(this);
		
		actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
		actionBarIcon.setOnClickListener(this);
	}

	@Override
	public void onPageSelected(int position) {
		return;
	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		return;
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;
				
				TouchImageView tIV = adapter.getVisibleImage(oldPosition);
				tIV.setZoom(1);
//				title.setText(names.get(positionG));
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.full_screen_image_view_menu, menu);
	    	    
	    return super.onCreateOptionsMenu(menu);
	}
	
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//	    switch (item.getItemId()) {
//	    // Respond to the action bar's Up/Home button
//		    case android.R.id.home:{
//		    	finish();
//		    	return true;
//		    }
//		}
//	    
//	    return super.onOptionsItemSelected(item);
//	}

	@Override
	public void onClick(View v) {

		switch (v.getId()){
			case R.id.full_image_viewer_icon:{
				finish();
				break;
			}
		}
	}
}
