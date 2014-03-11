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
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class MegaFullScreenImageAdapter extends PagerAdapter implements OnClickListener  {
	
	private Activity activity;
	private ArrayList<Long> imageHandles;
	private LayoutInflater inflater;
	View viewLayout;
	private SparseArray<TouchImageView> visibleImgs = new SparseArray<TouchImageView>();
	private boolean aBshown = true;
	
	MegaApiAndroid megaApi;
	
	/*public static view holder class*/
    public class ViewHolderFullImage {
        TouchImageView imgDisplay;
        ProgressBar progressBar;
        long document;
    }
	
	// constructor
	public MegaFullScreenImageAdapter(Activity activity, ArrayList<Long> imageHandles, MegaApiAndroid megaApi) {
		this.activity = activity;
		this.imageHandles = imageHandles;
		this.megaApi = megaApi;
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
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
		
        holder.document = imageHandles.get(position);
		Bitmap preview = null;
		Bitmap thumb = null;
		
		//First load the thumbnail and show it and then the preview
		if (node.hasThumbnail()){
			thumb = ThumbnailUtils.getThumbnailFromCache(node);
			if (thumb != null){
				holder.imgDisplay.setImageBitmap(thumb);
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromFolder(node, activity);
				if (thumb != null){
					holder.imgDisplay.setImageBitmap(thumb);
				}
				else{ 
					try{
						thumb = ThumbnailUtils.getThumbnailFromMegaFull(node, activity, holder, megaApi, this);
					}
					catch(Exception e){} //Too many AsyncTasks
					
					if (thumb != null){
						holder.imgDisplay.setImageBitmap(thumb);
					}
				}
			}
		}
		
		//Then, load the preview
		if (node.hasPreview()){
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				holder.imgDisplay.setImageBitmap(preview);
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, activity);
				if (preview != null){
					holder.imgDisplay.setImageBitmap(preview);
				}
				else{
					try{
						preview = PreviewUtils.getPreviewFromMega(node, activity, holder, megaApi, this);
						
//						//Posible manera de subir varias previews. Aunque el adapter ya pre-carga la de antes y la de despues  
//						public View getView(int position, View convertView, ViewGroup parent) {
//							  int limit = Math.min(position + 4, getCount());
//							  for (int i = position; i < limit; i++) {
//							    AsyncImageLoader.prefetchImage(getItem(i).getImageUrl());
//							  }
//							}
					}
					catch(Exception e){} //Too many AsyncTasks
					
					if (preview != null){
						holder.imgDisplay.setImageBitmap(preview);
					}					
				}
			}
			

		}
        //imgDisplay.setImageResource(imageIds.get(position));
		holder.imgDisplay.setOnClickListener(this);
        
        ((ViewPager) container).addView(viewLayout);
        
        visibleImgs.put(position, holder.imgDisplay);
		
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
