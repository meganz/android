package com.mega.android;

import java.util.ArrayList;

import com.mega.components.TouchImageView;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MegaFullScreenImageAdapter extends PagerAdapter implements OnClickListener  {
	
	private Activity _activity;
	private ArrayList<Integer> _imageIds;
	private ArrayList<String> _names;
	private LayoutInflater inflater;
	private ActionBar aB;
	TouchImageView imgDisplay;
	View viewLayout;
	private SparseArray<TouchImageView> visibleImgs = new SparseArray<TouchImageView>();
	private boolean aBshown = true;
	
	// constructor
	public MegaFullScreenImageAdapter(Activity activity, ArrayList<Integer> imageIds, ArrayList<String> names) {
		this._activity = activity;
		this._imageIds = imageIds;
		this._names = names;
		this.aB = ((ActionBarActivity)activity).getSupportActionBar();
	}

	@Override
	public int getCount() {
		return _imageIds.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
	
		inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
        
        imgDisplay.setImageResource(_imageIds.get(position));
        imgDisplay.setOnClickListener(this);
        
        ((ViewPager) container).addView(viewLayout);
        
        visibleImgs.put(position, imgDisplay);
		
		return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
		visibleImgs.remove(position);
        ((ViewPager) container).removeView((RelativeLayout) object);
 
    }
	
	public TouchImageView getVisibleImage(int position){
		return visibleImgs.get(position);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.full_screen_image_viewer_image:{
				
				Display display = _activity.getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics ();
			    display.getMetrics(outMetrics);
			    float density  = _activity.getResources().getDisplayMetrics().density;
				
			    float scaleW = Util.getScaleW(outMetrics, density);
			    float scaleH = Util.getScaleH(outMetrics, density);
			    
			    RelativeLayout bottomLayout = (RelativeLayout) _activity.findViewById(R.id.image_viewer_layout_bottom);
			    RelativeLayout topLayout = (RelativeLayout) _activity.findViewById(R.id.image_viewer_layout_top);
				if (aBshown){
					TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
					animBottom.setDuration(1000);
					animBottom.setFillAfter( true );
					bottomLayout.setAnimation(animBottom);
					
					TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
					animTop.setDuration(1000);
					animTop.setFillAfter( true );
					topLayout.setAnimation(animTop);
					
					aBshown = false;
				}
				else{					
					TranslateAnimation animBottom = new TranslateAnimation(0, 0, Util.px2dp(48, outMetrics), 0);
					animBottom.setDuration(1000);
					animBottom.setFillAfter( true );
					bottomLayout.setAnimation(animBottom);
					
					TranslateAnimation animTop = new TranslateAnimation(0, 0, Util.px2dp(-48, outMetrics), 0);
					animTop.setDuration(1000);
					animTop.setFillAfter( true );
					topLayout.setAnimation(animTop);
					
					aBshown = true;
				}
				
				RelativeLayout activityLayout = (RelativeLayout) _activity.findViewById(R.id.full_image_viewer_parent_layout);
				activityLayout.invalidate();
				
				break;
			}
		}
	}
}
