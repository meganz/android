package mega.privacy.android.app.lollipop.adapters;

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
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;


public class MegaOfflineFullScreenImageAdapterLollipop extends PagerAdapter implements OnClickListener {
	
	private Activity activity;
	private MegaOfflineFullScreenImageAdapterLollipop megaFullScreenImageAdapter;
	private ArrayList<MegaOffline> mOffList;
	private ArrayList<String> paths;
	private SparseArray<ViewHolderOfflineFullImage> visibleImgs = new SparseArray<ViewHolderOfflineFullImage>();
	private boolean aBshown = true;
	DatabaseHandler dbH = null;
	private boolean zipImage = false;
    Context context;

    private class OfflinePreviewAsyncTask extends AsyncTask<String, Void, Bitmap>{
		
		ViewHolderOfflineFullImage holder;
		String currentPath;
		
		public OfflinePreviewAsyncTask(ViewHolderOfflineFullImage holder) {
			this.holder = holder;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			log("doInBackground OfflinePreviewAsyncTask: "+holder.currentPath);
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
			log("onPostExecute OfflinePreviewAsyncTask: "+holder.currentPath);
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
	public MegaOfflineFullScreenImageAdapterLollipop(Context context, Activity activity, ArrayList<MegaOffline> mOffList) {
		this.activity = activity;
		this.mOffList = mOffList;
		this.megaFullScreenImageAdapter = this;
//		dbH = new DatabaseHandler(activity);
		dbH = DatabaseHandler.getDbHandler(activity);
		this.zipImage = false;
        this.context = context;
    }
	
	// constructor
	public MegaOfflineFullScreenImageAdapterLollipop(Context context, Activity activity, ArrayList<String> paths, boolean zipImage) {
		this.activity = activity;
		this.paths = paths;
		this.megaFullScreenImageAdapter = this;
//			dbH = new DatabaseHandler(activity);
		dbH = DatabaseHandler.getDbHandler(activity);
		this.zipImage = zipImage;
        this.context = context;

    }

	@Override
	public int getCount() {
		if (zipImage){
			return paths.size();
		}
		else{
			return mOffList.size();
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
        log ("INSTANTIATE POSITION " + position);
        
        File currentFile;
        if (zipImage){
        	currentFile = new File (paths.get(position));
			if(currentFile!=null){
				log("Got zip Image!");
			}
			else{
				log("zip Image is NULL");
			}
        }
        else{
        	currentFile = new File (mOffList.get(position).getPath());
        }
        
		ViewHolderOfflineFullImage holder = new ViewHolderOfflineFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setImageResource(MimeTypeMime.typeForName(currentFile.getName()).getIconResourceId());
		holder.imgDisplay.setOnClickListener(this);
		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.GONE);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		
		Bitmap preview = null;
		Bitmap thumb = null;
		
		if (zipImage){
			holder.currentPath = paths.get(position);
			log("ZIP holder.currentPath: "+holder.currentPath);
		}
		else{
			holder.currentPath = mOffList.get(position).getPath();
			log("holder.currentPath: "+holder.currentPath);
			try{
				holder.currentHandle = Long.parseLong(mOffList.get(position).getHandle());
				
				thumb = ThumbnailUtils.getThumbnailFromCache(holder.currentHandle);
				if (thumb != null){
					holder.imgDisplay.setImageBitmap(thumb);
				}
				else{
					preview = PreviewUtils.getPreviewFromCache(holder.currentHandle);
					if (preview != null){
						holder.imgDisplay.setImageBitmap(preview);
					}
					
					try{
						new OfflinePreviewAsyncTask(holder).execute(currentFile.getAbsolutePath());
					}
					catch(Exception e){
						//Too many AsyncTasks
					}
				}
			}
			catch(Exception e){}
		}
		
		visibleImgs.put(position, holder);
		
        if (zipImage){
			log("isZipImage");
			try{
				new OfflinePreviewAsyncTask(holder).execute(currentFile.getAbsolutePath());
			}
			catch(Exception e){
				//Too many AsyncTasks
				log("OfflinePreviewAsyncTask EXCEPTION");
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

                ((FullScreenImageViewerLollipop) context).touchImage();


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
		Util.log("MegaOfflineFullScreenImageAdapterLollipop", log);
	}
}
