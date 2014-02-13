package com.mega.android;

import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;
import com.mega.components.TouchImageView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import android.support.v4.view.ViewPager.OnPageChangeListener;

public class FullScreenImageViewer extends ActionBarActivity implements OnPageChangeListener{
	
	private ExtendedViewPager viewPager;
	private MegaFullScreenImageAdapter adapter;
	private ActionBar aB;
	private int positionG;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		aB = getSupportActionBar();
		aB.hide();
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
//		setContentView(viewPager);
		viewPager.setPageMargin(40);
		
		Intent i = getIntent();
		positionG = i.getIntExtra("position", 0);
		
		ArrayList<String> names = i.getStringArrayListExtra("names");
		ArrayList<Integer> imageIds = i.getIntegerArrayListExtra("imageIds");
//		Toast.makeText(this, position + "_" + name + "_" + imageIds.size(), Toast.LENGTH_SHORT).show();

		adapter = new MegaFullScreenImageAdapter(FullScreenImageViewer.this,imageIds, names);
		
		viewPager.setAdapter(adapter);
		
		viewPager.setCurrentItem(positionG);
		
		viewPager.setOnPageChangeListener(this);
	}

	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		// TODO Auto-generated method stub
		
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
			}
		}
	}
}
