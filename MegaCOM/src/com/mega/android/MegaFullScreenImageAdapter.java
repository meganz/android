package com.mega.android;

import java.util.ArrayList;

import com.mega.components.TouchImageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MegaFullScreenImageAdapter extends PagerAdapter {
	
	private Activity _activity;
	private ArrayList<Integer> _imageIds;
	private ArrayList<String> _names;
	private LayoutInflater inflater;
	
	// constructor
	public MegaFullScreenImageAdapter(Activity activity, ArrayList<Integer> imageIds, ArrayList<String> names) {
		this._activity = activity;
		this._imageIds = imageIds;
		this._names = names;
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
		TouchImageView imgDisplay;
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

        ((ViewPager) container).addView(viewLayout);
		
		return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);
 
    }
}
