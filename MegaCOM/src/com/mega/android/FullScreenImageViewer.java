package com.mega.android;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

public class FullScreenImageViewer extends Activity{
	
	private ViewPager viewPager;
	private MegaFullScreenImageAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		viewPager = (ViewPager) findViewById(R.id.image_viewer_pager);
		
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
