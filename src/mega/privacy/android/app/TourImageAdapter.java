package mega.privacy.android.app;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class TourImageAdapter extends PagerAdapter {

	private int[] mImages = new int[] {
	        R.drawable.storage_space,
	        R.drawable.speed,
	        R.drawable.privacy_security,
	        R.drawable.access
	    };	
	
	private Activity activity;
	private LayoutInflater inflater;
	
	// constructor
	public TourImageAdapter(Activity activity) {
		this.activity = activity;
	}
	
	@Override
	public int getCount() {
		return mImages.length;
	}

	@Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }
		
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imgDisplay;
 
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.tour_image_layout, container,false);
 
        imgDisplay = (ImageView) viewLayout.findViewById(R.id.imageTour);
        
        imgDisplay.setImageResource(mImages[position]);
        
        ((ViewPager) container).addView(viewLayout);
 
        return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((LinearLayout) object); 
    }

}
