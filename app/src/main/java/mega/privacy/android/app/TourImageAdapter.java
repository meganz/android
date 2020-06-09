package mega.privacy.android.app;

import android.app.Activity;
import android.content.Context;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TourImageAdapter extends PagerAdapter {

	private int[] mImages = new int[] {
			R.drawable.tour1,
			R.drawable.tour2,
			R.drawable.tour3,
			R.drawable.tour4
	};

	int[] barTitles = new int[] {
			R.string.title_tour_one,
			R.string.title_tour_two,
			R.string.title_tour_three,
			R.string.title_tour_four
	};

	int[] barTexts = new int[] {
			R.string.content_tour_one,
			R.string.content_tour_two,
			R.string.content_tour_three,
			R.string.content_tour_four
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

        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.tour_image_layout, container,false);
 
        ImageView imgDisplay = (ImageView) viewLayout.findViewById(R.id.imageTour);
        imgDisplay.setImageResource(mImages[position]);

		TextView text1 = (TextView) viewLayout.findViewById(R.id.tour_text_1);
		text1.setText(barTitles[position]);
		TextView text2 = (TextView) viewLayout.findViewById(R.id.tour_text_2);
		text2.setText(barTexts[position]);

        ((ViewPager) container).addView(viewLayout);
 
        return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((LinearLayout) object); 
    }

}
