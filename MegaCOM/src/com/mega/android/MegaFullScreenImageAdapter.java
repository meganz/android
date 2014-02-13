package com.mega.android;

import java.util.ArrayList;

import com.mega.components.TouchImageView;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaFullScreenImageAdapter extends PagerAdapter {
	
	private Activity _activity;
	private ArrayList<Integer> _imageIds;
	private ArrayList<String> _names;
	private LayoutInflater inflater;
	private ActionBar aB;
	TouchImageView imgDisplay;
	private SparseArray<TouchImageView> visibleImgs = new SparseArray<TouchImageView>();

	
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
        Button btnClose;
        TextView title;
		
		inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		btnClose = (Button) viewLayout.findViewById(R.id.full_screen_image_viewer_close_button);
        title = (TextView) viewLayout.findViewById(R.id.full_screen_image_viewer_title);
        
        imgDisplay.setImageResource(_imageIds.get(position));
        
        // close button click event
        btnClose.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				_activity.finish();
			}
		}); 
        
        title.setText(_names.get(position));
        imgDisplay.setOnClickListener(new OnClickListener() {
			
				@Override
				public void onClick(View v) {
					
				}
			});

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
}
