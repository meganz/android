package com.mega.android;

import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;
import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class FullScreenImageViewer extends ActionBarActivity implements OnPageChangeListener, OnClickListener{
	
	private Display display;
	private DisplayMetrics outMetrics;
	private float density;
	private float scaleW;
	private float scaleH;
	
	private boolean aBshown = true;
	

	private MegaFullScreenImageAdapter adapter;
	private int positionG;
	private ArrayList<String> names;
	private ArrayList<Integer> imageIds;
	private ArrayList<Long> imageHandles;
	
	private ImageView actionBarIcon;
	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
	private ExtendedViewPager viewPager;	
	
	static FullScreenImageViewer fullScreenImageViewer;
    private MegaApiAndroid megaApi;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fullScreenImageViewer = this;
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);
		
		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);
		
		names = intent.getStringArrayListExtra("names");
		imageIds = intent.getIntegerArrayListExtra("imageIds");
		
		imageHandles = new ArrayList<Long>();
		long parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);
		MegaNode parentNode;
		if (parentNodeHandle == -1){
			parentNode = megaApi.getRootNode();
		}
		else{
			parentNode = megaApi.getNodeByHandle(parentNodeHandle);
		}
		
		NodeList nodes = megaApi.getChildren(parentNode);
		for (int i=0;i<nodes.size();i++){
			MegaNode n = nodes.get(i);
			if (MimeType.typeForName(n.getName()).isImage()){
				imageHandles.add(n.getHandle());
			}
		}
		Toast.makeText(this, ""+parentNode.getName() + "_" + imageHandles.size(), Toast.LENGTH_LONG).show();
			
		adapter = new MegaFullScreenImageAdapter(fullScreenImageViewer,imageIds, names, imageHandles, megaApi);
		
		viewPager.setAdapter(adapter);
		
		viewPager.setCurrentItem(positionG);

		viewPager.setOnPageChangeListener(this);
		
		actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
		actionBarIcon.setOnClickListener(this);
		
		bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
	    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		
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

	@Override
	public void onClick(View v) {

		switch (v.getId()){
			case R.id.full_image_viewer_icon:{
				finish();
				break;
			}
		}
	}
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putBoolean("aBshown", adapter.isaBshown());
	}
	
	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		aBshown = savedInstanceState.getBoolean("aBshown");
		adapter.setaBshown(aBshown);
		
		if (!aBshown){
			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
			animBottom.setDuration(0);
			animBottom.setFillAfter( true );
			bottomLayout.setAnimation(animBottom);
			
			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
			animTop.setDuration(0);
			animTop.setFillAfter( true );
			topLayout.setAnimation(animTop);
		}
	}
}
