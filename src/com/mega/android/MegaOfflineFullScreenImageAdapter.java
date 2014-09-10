package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.mega.components.TouchImageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class MegaOfflineFullScreenImageAdapter extends PagerAdapter implements OnClickListener {
	
	private Activity activity;
	private MegaOfflineFullScreenImageAdapter megaFullScreenImageAdapter;
	private ArrayList<String> paths;
	private SparseArray<ViewHolderOfflineFullImage> visibleImgs = new SparseArray<ViewHolderOfflineFullImage>();
	private boolean aBshown = true;
	DatabaseHandler dbH = null;
	
	
	private class OfflinePreviewAsyncTask extends AsyncTask<String, Void, Bitmap>{
		
		ViewHolderOfflineFullImage holder;
		String currentPath;
		
		public OfflinePreviewAsyncTask(ViewHolderOfflineFullImage holder) {
			this.holder = holder;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			currentPath = params[0];
			File currentFile = new File(currentPath);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			
			ExifInterface exif;
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			try {
				exif = new ExifInterface(currentFile.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			} catch (IOException e) {}  
			
			// Calculate inSampleSize
		    options.inSampleSize = Util.calculateInSampleSize(options, 1000, 1000);
		    
		    // Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			if (preview != null){
				preview = Util.rotateBitmap(preview, orientation);
				long handle = holder.currentHandle;
				PreviewUtils.setPreviewCache(handle, preview);
				return preview;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap preview){
			if (preview != null){
				if (holder.currentPath.equals(currentPath)){
					holder.imgDisplay.setImageBitmap(preview);
				}
			}
		}
	}
		
	/*view holder class*/
    public class ViewHolderOfflineFullImage {
        TouchImageView imgDisplay;
        ProgressBar progressBar;
        ProgressBar downloadProgressBar;
        String currentPath;
        long currentHandle;
        int position;
    }
	
	// constructor
	public MegaOfflineFullScreenImageAdapter(Activity activity, ArrayList<String> paths) {
		this.activity = activity;
		this.paths = paths;
		this.megaFullScreenImageAdapter = this;
		dbH = new DatabaseHandler(activity);
	}

	@Override
	public int getCount() {
		return paths.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
        log ("INSTANTIATE POSITION " + position);
        
        File currentFile = new File (paths.get(position));
        
		ViewHolderOfflineFullImage holder = new ViewHolderOfflineFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setImageResource(MimeType.typeForName(currentFile.getName()).getIconResourceId());
		holder.imgDisplay.setOnClickListener(this);
		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.GONE);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		
		holder.currentPath = paths.get(position);
		
		visibleImgs.put(position, holder);
        
		Bitmap preview = null;
		Bitmap thumb = null;
		
		//Get the path of the file
		
		String pathFile = currentFile.getAbsolutePath();		
		String[] subPath=pathFile.split(Util.offlineDIR);		
		int index = subPath[1].lastIndexOf("/");		
		pathFile = subPath[1].substring(0, index+1);
		
		//Get the handle from the db
		MegaOffline mOff = dbH.findbyPathAndName(pathFile, currentFile.getName());
		
		long handle = Long.parseLong(mOff.getHandle());
		holder.currentHandle = handle;
		
		log("Handle gatito" + mOff.getHandle() );
		
		preview = PreviewUtils.getPreviewFromCache(handle);
		if (preview != null){
			holder.imgDisplay.setImageBitmap(preview);
		}
		else{
			thumb = ThumbnailUtils.getThumbnailFromCache(handle);
			if (thumb != null){
				holder.imgDisplay.setImageBitmap(thumb);
			}
			
			try{
				new OfflinePreviewAsyncTask(holder).execute(currentFile.getAbsolutePath());
			}
			catch(Exception e){
				//Too many AsyncTasks
			}
		}
		
        ((ViewPager) container).addView(viewLayout);
		
		return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
		visibleImgs.remove(position);
        ((ViewPager) container).removeView((RelativeLayout) object);
        System.gc();
        log ("DESTROY POSITION " + position + " SIZE SPARSE: " + visibleImgs.size());
 
    }
	
	public TouchImageView getVisibleImage(int position){
		return visibleImgs.get(position).imgDisplay;
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
		Util.log("MegaOfflineFullScreenImageAdapter", log);
	}
}
