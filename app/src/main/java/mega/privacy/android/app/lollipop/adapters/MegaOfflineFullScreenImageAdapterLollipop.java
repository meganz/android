package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.utils.OfflineUtils;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaOfflineFullScreenImageAdapterLollipop extends PagerAdapter implements OnClickListener{
	
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
			logDebug("doInBackground OfflinePreviewAsyncTask: " + holder.currentPath);
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
				preview = rotateBitmap(preview, orientation);
				long handle = holder.currentHandle;
				setPreviewCache(handle, preview);
				return preview;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap preview){
			logDebug("onPostExecute OfflinePreviewAsyncTask: " + holder.currentPath);
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
        ImageView gifImgDisplay;
        ProgressBar progressBar;
        ProgressBar downloadProgressBar;
        String currentPath;
        long currentHandle;
        int position;
        boolean isGIF;
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
		logDebug("INSTANTIATE POSITION " + position);
        
        File currentFile;
        if (zipImage){
        	currentFile = new File (paths.get(position));
			if(currentFile!=null){
				logDebug("Got zip Image!");
			}
			else{
				logWarning("zip Image is NULL");
			}
        }
        else{
        	currentFile = new File (mOffList.get(position).getPath());
        }
        
		ViewHolderOfflineFullImage holder = new ViewHolderOfflineFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);

		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.GONE);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setOnClickListener(this);
		holder.gifImgDisplay = (ImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_gif);
		holder.gifImgDisplay.setOnClickListener(this);

		boolean isGIF;
		if (zipImage) {
			isGIF = isGIF(paths.get(position));
		}
		else {
			isGIF = isGIF(mOffList.get(position).getName());
		}

		if (isGIF){
			logDebug("isGIF");
			holder.isGIF = true;
			holder.imgDisplay.setVisibility(View.GONE);
			holder.gifImgDisplay.setVisibility(View.VISIBLE);
			if (zipImage) {
				holder.currentPath = paths.get(position);
			}
			else {
				holder.currentPath = mOffList.get(position).getPath();
			}
			holder.progressBar.setVisibility(View.VISIBLE);

			Bitmap thumb = null;
			Bitmap preview = null;
			if (!zipImage) {
				try {
					holder.currentHandle = Long.parseLong(mOffList.get(position).getHandle());

					thumb = getThumbnailFromCache(holder.currentHandle);
					preview = getPreviewFromCache(holder.currentHandle);
				} catch (Exception e) {
				}
			}

			File file;
			if (zipImage) {
				file = currentFile;
			}
			else {
				file = getOfflineFile(position);
			}

			Drawable drawable = null;
			if (preview != null){
				drawable = new BitmapDrawable(context.getResources(), preview);
			}
			else if (thumb != null){
				drawable = new BitmapDrawable(context.getResources(), thumb);
			}
			if (drawable == null) {
				drawable = ContextCompat.getDrawable(context, MimeTypeThumbnail.typeForName(file.getName()).getIconResourceId());
			}

			if (file != null){
				final ProgressBar pb = holder.progressBar;

				if (drawable != null){
					Glide.with(context).load(file).listener(new RequestListener<File, GlideDrawable>() {
						@Override
						public boolean onException(Exception e, File model, Target<GlideDrawable> target, boolean isFirstResource) {
							return false;
						}

						@Override
						public boolean onResourceReady(GlideDrawable resource, File model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
							pb.setVisibility(View.GONE);
							return false;
						}
					}).placeholder(drawable).diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade().into(holder.gifImgDisplay);
				}
				else {
					Glide.with(context).load(file).listener(new RequestListener<File, GlideDrawable>() {
						@Override
						public boolean onException(Exception e, File model, Target<GlideDrawable> target, boolean isFirstResource) {
							return false;
						}

						@Override
						public boolean onResourceReady(GlideDrawable resource, File model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
							pb.setVisibility(View.GONE);
							return false;
						}
					}).diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade().into(holder.gifImgDisplay);
				}
			}
		}
		else {
			holder.isGIF = false;
			holder.imgDisplay.setVisibility(View.VISIBLE);
			holder.gifImgDisplay.setVisibility(View.GONE);
			holder.imgDisplay.setImageResource(MimeTypeThumbnail.typeForName(currentFile.getName()).getIconResourceId());

			if (zipImage){
				holder.currentPath = paths.get(position);
				logDebug("ZIP holder.currentPath: " + holder.currentPath);
			}
			else{
				holder.currentPath = mOffList.get(position).getPath();
				logDebug("holder.currentPath: " + holder.currentPath);
				try{
					holder.currentHandle = Long.parseLong(mOffList.get(position).getHandle());

					Bitmap thumb = getThumbnailFromCache(holder.currentHandle);
					if (thumb != null){
						holder.imgDisplay.setImageBitmap(thumb);

					}

					Bitmap preview = getPreviewFromCache(holder.currentHandle);
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
				catch(Exception e){}
			}
		}
		
		visibleImgs.put(position, holder);
		
        if (zipImage){
			logDebug("isZipImage");
			try{
				new OfflinePreviewAsyncTask(holder).execute(currentFile.getAbsolutePath());
			}
			catch(Exception e){
				//Too many AsyncTasks
				logError("OfflinePreviewAsyncTask EXCEPTION", e);
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
		logDebug ("DESTROY POSITION " + position + " SIZE SPARSE: " + visibleImgs.size());
 
    }

	public ImageView getVisibleImage(int position){
    	if (visibleImgs.get(position).isGIF) {
			return visibleImgs.get(position).gifImgDisplay;
		}
		else {
			return visibleImgs.get(position).imgDisplay;
		}
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch(v.getId()){
			case R.id.full_screen_image_viewer_gif:
			case R.id.full_screen_image_viewer_image:{
				
				Display display = activity.getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics ();
			    display.getMetrics(outMetrics);
			    float density  = activity.getResources().getDisplayMetrics().density;
				
			    float scaleW = getScaleW(outMetrics, density);
			    float scaleH = getScaleH(outMetrics, density);

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

	public boolean isGIF(String name){

		String s[] = name.split("\\.");

		if (s != null){
			if (s[s.length-1].equals("gif")){
				return true;
			}
		}

		return false;
	}

	public File getOfflineFile (int position){
		MegaOffline checkOffline = mOffList.get(position);

		return OfflineUtils.getOfflineFile(context, checkOffline);
	}
}
