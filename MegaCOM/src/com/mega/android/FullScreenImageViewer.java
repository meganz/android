package com.mega.android;

import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;
import com.mega.components.TouchImageView;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class FullScreenImageViewer extends ActionBarActivity implements OnPageChangeListener{
	
	private ExtendedViewPager viewPager;
	private MegaFullScreenImageAdapter adapter;
	private ActionBar aB;
	private int positionG;
//	private TextView title; 
//	private ImageView btnClose;
	private ArrayList<String> names;
	private ArrayList<Integer> imageIds;
	
	
	private MenuItem searchMenuItem;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		aB = getSupportActionBar();
//		aB.setDisplayHomeAsUpEnabled(true);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(false);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
//		if (android.os.Build.VERSION _)
//		ImageView logo = (ImageView) findViewById(R.id.homeAsUp);
//	    logo.setImageResource(R.drawable.ic_action_navigation_accept);
	    
		
//		aB.hide();
		
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
		
//		title = (TextView) findViewById(R.id.full_screen_image_viewer_title);
//        title.setText(names.get(positionG));
//        
//		btnClose = (ImageView) findViewById(R.id.full_screen_image_close);
//		btnClose.setOnClickListener(this);
		

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
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.full_screen_image_view_menu, menu);
	    	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		}
	    
	    return super.onOptionsItemSelected(item);
	}

//	@Override
//	public void onClick(View v) {
//		
//		switch (v.getId()) {
//			case R.id.full_screen_image_close:{
//				onClickClose();
//				break;
//			}
//			case R.id.image_viewer_pager:{
//				onClickViewPager();
//				break;
//			}
//		}
//	}
//	
//	public void onClickClose(){
//		finish();
//	}
//	
//	public void onClickViewPager(){
//		btnClose.setVisibility(View.GONE);		
//	}
}
