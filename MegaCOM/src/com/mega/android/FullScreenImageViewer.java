package com.mega.android;

import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

public class FullScreenImageViewer extends ActionBarActivity{
	
	private ExtendedViewPager viewPager;
	private MegaFullScreenImageAdapter adapter;
	ActionBar aB;
	
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
		int position = i.getIntExtra("position", 0);
		ArrayList<String> names = i.getStringArrayListExtra("names");
		ArrayList<Integer> imageIds = i.getIntegerArrayListExtra("imageIds");
//		Toast.makeText(this, position + "_" + name + "_" + imageIds.size(), Toast.LENGTH_SHORT).show();

		adapter = new MegaFullScreenImageAdapter(FullScreenImageViewer.this,imageIds, names);
		
		viewPager.setAdapter(adapter);
		
		viewPager.setCurrentItem(position);


	}

}
