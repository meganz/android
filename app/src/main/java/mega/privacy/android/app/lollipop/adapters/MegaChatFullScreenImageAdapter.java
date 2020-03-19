package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaChatFullScreenImageAdapter extends PagerAdapter implements OnClickListener, MegaRequestListenerInterface {

	private Activity activity;
	private MegaChatFullScreenImageAdapter megaFullScreenImageAdapter;
	private ArrayList<MegaChatMessage> messages;
	private SparseArray<ViewHolderFullImage> visibleImgs = new SparseArray<ViewHolderFullImage>();
	private boolean aBshown = true;
	private boolean menuVisible = false;

	private ArrayList<Long> pendingPreviews = new ArrayList<Long>();
	private ArrayList<Long> pendingFullImages = new ArrayList<Long>();

	MegaApiAndroid megaApi;
	Context context;

	String downloadLocationDefaultPath;
	DatabaseHandler dbH;
	MegaPreferences prefs;

	/*view holder class*/
    public class ViewHolderFullImage {
    	public TouchImageView imgDisplay;
    	public ImageView gifImgDisplay;
    	public ProgressBar progressBar;
    	public ProgressBar downloadProgressBar;
    	public long document;
    	public int position;
    	public boolean isGIF;
    }

    private class PreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer>{

    	MegaNode node;
    	Bitmap preview;

		@Override
		protected Integer doInBackground(MegaNode... params){
			node = params[0];
			preview = getPreviewFromFolder(node, activity);

			if (preview != null){
				return 0;
			}
			else{
				if (pendingPreviews.contains(node.getHandle())){
					logDebug("The preview is already downloaded or added to the list");
					return 1;
				}
				else{
					return 2;
				}
			}
		}

		@Override
		protected void onPostExecute(Integer param){
			if (param == 0){
				int position = 0;
				boolean holderIsVisible = false;
				for(int i = 0; i < visibleImgs.size(); i++) {
					position = visibleImgs.keyAt(i);
					ViewHolderFullImage holder = visibleImgs.get(position);
					if (holder.document == node.getHandle()){
						holderIsVisible = true;
						break;
					}
				}

				if (holderIsVisible){
					previewCache.put(node.getHandle(), preview);
					visibleImgs.get(position).imgDisplay.setImageBitmap(preview);
					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
				}
			}
			else if(param == 2){
				if(megaApi!=null){
					File previewFile = new File(getPreviewFolder(activity), node.getBase64Handle()+".jpg");
					logDebug("GET PREVIEW OF HANDLE: " + node.getHandle());
					pendingPreviews.add(node.getHandle());
					megaApi.getPreview(node,  previewFile.getAbsolutePath(), megaFullScreenImageAdapter);
				}
			}
		}
	}

	private class PreviewDownloadAsyncTask extends AsyncTask<MegaNode, Void, Integer>{

		MegaNode node;
    	Bitmap preview;
    	File cacheDir;
    	File destination;

		@Override
		protected Integer doInBackground(MegaNode... params){
			node = params[0];

			if (node == null){
				return 3;
			}
			preview = getPreviewFromFolder(node, activity);

			if (preview != null){
				return 0;
			}
			else{
				destination = buildPreviewFile(activity, node.getName());

				if (isFileAvailable(destination)){
					if (destination.length() == node.getSize()){
						File previewDir = getPreviewFolder(activity);
						File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
						logDebug("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
						boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);

						if (previewCreated){
							preview = getBitmapForCache(previewFile, activity);
							destination.delete();
							return 0;
						}
						else{
							return 1;
						}
					}
					else{
						destination.delete();
						return 1;
					}
				}

				if (pendingFullImages.contains(node.getHandle())){
					logDebug("The image is already downloaded or added to the list - return 1");
					return 1;
				}
				else{
					logDebug("return code 2");
					return 2;
				}
			}
		}

		@Override
		protected void onPostExecute(Integer param){
			if (param == 0 || param == 1){
				int position = 0;
				boolean holderIsVisible = false;
				for(int i = 0; i < visibleImgs.size(); i++) {
					position = visibleImgs.keyAt(i);
					ViewHolderFullImage holder = visibleImgs.get(position);
					if (holder.document == node.getHandle()){
						holderIsVisible = true;
						break;
					}
				}

				if (holderIsVisible){
					if(param == 0)
					{
						visibleImgs.get(position).imgDisplay.setImageBitmap(preview);
					}
					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
				}
			}
			else if (param == 2){
				logWarning("There is no preview for this node");
			}
		}
	}

	// constructor
	public MegaChatFullScreenImageAdapter(Context context, Activity activity, ArrayList<MegaChatMessage> messages, MegaApiAndroid megaApi) {
		this.activity = activity;
		this.megaApi = megaApi;
		this.messages = messages;
		this.megaFullScreenImageAdapter = this;
		this.context = context;


		dbH = DatabaseHandler.getDbHandler(context);

		prefs = dbH.getPreferences();

		downloadLocationDefaultPath = getDownloadLocation(context);
	}

	@Override
	public int getCount() {
		return messages.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
		logDebug ("INSTANTIATE POSITION: " + position);

		MegaNode node = messages.get(position).getMegaNodeList().get(0);
		
		ViewHolderFullImage holder = new ViewHolderFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		if (node == null){
			Intent intent = new Intent(activity, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        activity.startActivity(intent);
	        activity.finish();
	        return viewLayout;
		}

		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.GONE);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		holder.document = messages.get(position).getMegaNodeList().get(0).getHandle();

		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setOnClickListener(this);
		holder.gifImgDisplay = (ImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_gif);
		holder.gifImgDisplay.setOnClickListener(this);
		
		visibleImgs.put(position, holder);

		Bitmap preview = null;
		Bitmap thumb = null;

		final ProgressBar pb = holder.progressBar;

		if (MimeTypeList.typeForName(node.getName()).isGIF()){
			holder.isGIF = true;
			holder.imgDisplay.setVisibility(View.GONE);
			holder.gifImgDisplay.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.VISIBLE);

			Bitmap resource = setImageResource(node, holder);
			Drawable drawable = null;
			if (resource != null){
				drawable = new BitmapDrawable(context.getResources(), resource);
			}
			if (drawable == null) {
				drawable = ContextCompat.getDrawable(context, MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
			}

			String localPath = getLocalFile(context, node.getName(), node.getSize());

			if (localPath != null){
				if (drawable != null){
					Glide.with(context)
							.load(new File(localPath))
							.listener(new RequestListener<Drawable>() {
								@Override
								public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
									return false;
								}

								@Override
								public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
									pb.setVisibility(View.GONE);
									return false;
								}
							})
							.placeholder(drawable)
							.diskCacheStrategy(DiskCacheStrategy.ALL)
							.transition(withCrossFade())
							.into(holder.gifImgDisplay);
				}
			}
			else {
				holder.progressBar.setVisibility(View.VISIBLE);

				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if(mi.totalMem>BUFFER_COMP){
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				}
				else{
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}

				String url = megaApi.httpServerGetLocalLink(node);
				if (url != null){
					if (drawable != null){
						Glide.with(context)
								.load(Uri.parse(url))
								.listener(new RequestListener<Drawable>() {
									@Override
									public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
										return false;
									}

									@Override
									public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
										pb.setVisibility(View.GONE);
										return false;
									}
								})
								.placeholder(drawable)
								.diskCacheStrategy(DiskCacheStrategy.ALL)
								.transition(withCrossFade())
								.into(holder.gifImgDisplay);
					}
					else {
						Glide.with(context)
								.load(Uri.parse(url))
								.listener(new RequestListener<Drawable>() {
									@Override
									public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
										return false;
									}

									@Override
									public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
										pb.setVisibility(View.GONE);
										return false;
									}
								})
								.diskCacheStrategy(DiskCacheStrategy.ALL)
								.transition(withCrossFade())
								.into(holder.gifImgDisplay);
					}
				}
			}
		}
		else {
			holder.isGIF = false;
			holder.imgDisplay.setVisibility(View.VISIBLE);
			holder.gifImgDisplay.setVisibility(View.GONE);
			holder.imgDisplay.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

			thumb = getThumbnailFromCache(node);
			if (thumb != null){
				holder.imgDisplay.setImageBitmap(thumb);
			}
			else{
				thumb = getThumbnailFromFolder(node, activity);
				if (thumb != null){
					holder.imgDisplay.setImageBitmap(thumb);
				}
			}

			if (node.hasPreview()){
				preview = getPreviewFromCache(node);
				if (preview != null){
					previewCache.put(node.getHandle(), preview);
					holder.imgDisplay.setImageBitmap(preview);
				}
				else{
					try{
						new PreviewAsyncTask().execute(node);
					}
					catch(Exception ex){
						//Too many AsyncTasks
						logError("Too many AsyncTasks", ex);
					}
				}
			}
			else{
				preview = getPreviewFromCache(node);
				if (preview != null){
					previewCache.put(node.getHandle(), preview);
					holder.imgDisplay.setImageBitmap(preview);
				}
				else{
					try{
						new PreviewDownloadAsyncTask().execute(node);
					}
					catch(Exception ex){
						//Too many AsyncTasks
						logError("Too many AsyncTasks", ex);
					}
				}
			}
		}
		
        ((ViewPager) container).addView(viewLayout);
		
		return viewLayout;
	}

	public Bitmap setImageResource(MegaNode node, ViewHolderFullImage holder){

		Bitmap preview;
		Bitmap thumb = getThumbnailFromCache(node);
		if (thumb == null){
			thumb = getThumbnailFromFolder(node, activity);
		}

		if (node.hasPreview()){
			preview = getPreviewFromCache(node);
			if (preview != null){
				previewCache.put(node.getHandle(), preview);
			}
		}
		else{
			preview = getPreviewFromCache(node);
			if (preview != null){
				previewCache.put(node.getHandle(), preview);
			}
		}

		if (preview != null){
			return preview;
		}
		else if (thumb != null){
			return thumb;
		}
		else {
			return null;
		}
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
		visibleImgs.remove(position);
        ((ViewPager) container).removeView((RelativeLayout) object);
        System.gc();
		logDebug ("DESTROY POSITION " + position + " visibleImgs.size(): " + visibleImgs.size());
 
    }
	
	public ImageView getVisibleImage(int position){

    	if (visibleImgs.get(position).isGIF){
			return visibleImgs.get(position).gifImgDisplay;
		}
    	else {
			return visibleImgs.get(position).imgDisplay;
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.full_screen_image_viewer_gif:
			case R.id.full_screen_image_viewer_image:{
				
				Display display = activity.getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics ();
			    display.getMetrics(outMetrics);
			    float density  = activity.getResources().getDisplayMetrics().density;
				
			    float scaleW = getScaleW(outMetrics, density);
			    float scaleH = getScaleH(outMetrics, density);

				((ChatFullScreenImageViewer) context).touchImage();

				RelativeLayout activityLayout = (RelativeLayout) activity.findViewById(R.id.chat_full_image_viewer_parent_layout);
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
	
	public boolean isMenuVisible() {
		return menuVisible;
	}

	public void setMenuVisible(boolean menuVisible) {
		this.menuVisible = menuVisible;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
		logDebug("Node Handle: " + request.getNodeHandle());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

		logDebug("onRequestFinish: " + request.getRequestString());
		logDebug("Node Handle: " + request.getNodeHandle());

		long handle = request.getNodeHandle();
		String handleBase64 = MegaApiJava.handleToBase64(handle);

		pendingPreviews.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			File previewDir = getPreviewFolder(activity);
			File preview = new File(previewDir, handleBase64+".jpg");
			
			if (preview.exists()) {
				if (preview.length() > 0) {
					logDebug("GET PREVIEW FINISHED. HANDLE: " + handle + " visibleImgs.size(): " + visibleImgs.size());
					int position = 0;
					boolean holderIsVisible = false;
					for(int i = 0; i < visibleImgs.size(); i++) {
						position = visibleImgs.keyAt(i);
						ViewHolderFullImage holder = visibleImgs.get(position);
						if (holder.document == handle){
							holderIsVisible = true;
							break;
						}
					}
					
					if (holderIsVisible){
						Bitmap bitmap = getBitmapForCache(preview, activity);
						previewCache.put(handle, bitmap);						
						visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
						visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					}
				}
			}
		}
		else{
			logError("ERROR FINISH: " + e.getErrorCode() + "_" + e.getErrorString());
			try{
//				new PreviewDownloadAsyncTask().execute(node);
			}
			catch(Exception ex){
				//Too many AsyncTasks
				logError("Too many AsyncTasks", ex);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
		logWarning("Node Handle: " + request.getNodeHandle());
		logError("ERROR: " + e.getErrorCode() + "_" + e.getErrorString());
	}


	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		
	}
}
