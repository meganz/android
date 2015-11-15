package mega.privacy.android.app;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    
    ArrayList<Long> nodeHandles;

    public ImageAdapter(Context c, ArrayList<Long> nodeHandles) {
        this.context = c;
        this.nodeHandles = nodeHandles;
    }

    public int getCount() {
        return nodeHandles.size();
    }

    public Object getItem(int position) {
        return nodeHandles.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(270, 270));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(R.drawable.ic_image_list);
        return imageView;
    }
}