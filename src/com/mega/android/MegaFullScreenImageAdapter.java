package com.mega.android;

import java.util.ArrayList;

import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaFullScreenImageAdapter extends PagerAdapter implements OnClickListener  {
	
	private Activity activity;
	private ArrayList<Integer> imageIds;
	private ArrayList<String> names;
	private ArrayList<Long> imageHandles;
	private LayoutInflater inflater;
	private ActionBar aB;
	TouchImageView imgDisplay;
	View viewLayout;
	private SparseArray<TouchImageView> visibleImgs = new SparseArray<TouchImageView>();
	private boolean aBshown = true;
	
	MegaApiAndroid megaApi;
	
	/*public static view holder class*/
    public class ViewHolderFullImage {
        TouchImageView imgDisplay;
        long document;
    }
	
	// constructor
	public MegaFullScreenImageAdapter(Activity activity, ArrayList<Integer> imageIds, ArrayList<String> names, ArrayList<Long> imageHandles, MegaApiAndroid megaApi) {
		this.activity = activity;
		this.imageIds = imageIds;
		this.names = names;
		this.imageHandles = imageHandles;
		this.megaApi = megaApi;
		this.aB = ((ActionBarActivity)activity).getSupportActionBar();
	}

	@Override
	public int getCount() {
		return imageHandles.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
		
		ViewHolderFullImage holder = new ViewHolderFullImage();
	
		MegaNode node = megaApi.getNodeByHandle(imageHandles.get(position));
		
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		imgDisplay.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
		
        holder.imgDisplay = imgDisplay;
        holder.document = imageHandles.get(position);
		Bitmap preview = null;
		if (node.hasPreview()){
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				imgDisplay.setImageBitmap(preview);
				Toast.makeText(activity, "HAS PREVIEW YES CACHE", Toast.LENGTH_LONG).show();
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, activity);
				if (preview != null){
					imgDisplay.setImageBitmap(preview);
					Toast.makeText(activity, "HAS PREVIEW NOT CACHE YES FOLDER", Toast.LENGTH_LONG).show();
				}
				else{
					try{
						log ("Descargando preview (" + position + ")");
						preview = PreviewUtils.getPreviewFromMega(node, activity, holder, megaApi, this);
					}
					catch(Exception e){} //Too many AsyncTasks
					
					if (preview != null){
						imgDisplay.setImageBitmap(preview);
					}					
				}
			}
			

		}
        //imgDisplay.setImageResource(imageIds.get(position));
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
				
				Display display = activity.getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics ();
			    display.getMetrics(outMetrics);
			    float density  = activity.getResources().getDisplayMetrics().density;
				
			    float scaleW = Util.getScaleW(outMetrics, density);
			    float scaleH = Util.getScaleH(outMetrics, density);
			    
			    RelativeLayout bottomLayout = (RelativeLayout) activity.findViewById(R.id.image_viewer_layout_bottom);
			    RelativeLayout topLayout = (RelativeLayout) activity.findViewById(R.id.image_viewer_layout_top);
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
				
				RelativeLayout activityLayout = (RelativeLayout) activity.findViewById(R.id.full_image_viewer_parent_layout);
				activityLayout.invalidate();
				
				break;
			}
		}
	}

	public boolean isaBshown() {
		return aBshown;
	}

	public void setaBshown(boolean aBshown) {
		this.aBshown = aBshown;
	}
	private static void log(String log) {
		Util.log("MegaFullScreenImageAdapter", log);
	}
}
