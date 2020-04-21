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

public class PermissionsImageAdapter extends PagerAdapter {

    int[] mImages = new int[] {
            R.drawable.storage_space,
            R.drawable.speed,
            R.drawable.privacy_security,
    };

    Activity activity;
    Context context;

    public PermissionsImageAdapter (Activity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        String[] mTitles = new String[] {
                context.getString(R.string.allow_acces_media_title),
                context.getString(R.string.allow_acces_camera_title),
                context.getString(R.string.allow_acces_calls_title)
        };

        String[] mSubtitles =  new String[] {
                context.getString(R.string.allow_acces_media_subtitle),
                context.getString(R.string.allow_acces_camera_subtitle),
                context.getString(R.string.allow_acces_calls_subtitle_microphone)
        };

        ImageView imgDisplay;
        TextView titleDisplay;
        TextView subtitleDisplay;

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.permissions_image_layout, container,false);

        imgDisplay = (ImageView) viewLayout.findViewById(R.id.image_permissions);
        titleDisplay = (TextView) viewLayout.findViewById(R.id.title_permissions);
        subtitleDisplay = (TextView) viewLayout.findViewById(R.id.subtitle_permissions);

        imgDisplay.setImageResource(mImages[position]);
        titleDisplay.setText(mTitles[position]);
        subtitleDisplay.setText(mSubtitles[position]);

        ((ViewPager) container).addView(viewLayout);

        return viewLayout;
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
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((LinearLayout) object);
    }
}
