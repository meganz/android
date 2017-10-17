package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;

import mega.privacy.android.app.lollipop.ChatFullScreenImageViewer;



public class MegaChatFullScreenImageAdapter extends PagerAdapter implements OnClickListener, MegaRequestListenerInterface  {

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

	/*view holder class*/
    public class ViewHolderFullImage {
    	public TouchImageView imgDisplay;
    	public ProgressBar progressBar;
    	public ProgressBar downloadProgressBar;
    	public long document;
    	public int position;
    }

    private class PreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer>{

    	MegaNode node;
    	Bitmap preview;

		@Override
		protected Integer doInBackground(MegaNode... params){
			node = params[0];
			preview = PreviewUtils.getPreviewFromFolder(node, activity);

			if (preview != null){
				return 0;
			}
			else{
				if (pendingPreviews.contains(node.getHandle())){
					log("the preview is already downloaded or added to the list");
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
					PreviewUtils.previewCache.put(node.getHandle(), preview);
					visibleImgs.get(position).imgDisplay.setImageBitmap(preview);
					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
				}
			}
			else if(param == 2){
				File previewFile = new File(PreviewUtils.getPreviewFolder(activity), node.getBase64Handle()+".jpg");
				log("GET PREVIEW OF HANDLE: " + node.getHandle());
				pendingPreviews.add(node.getHandle());
				megaApi.getPreview(node,  previewFile.getAbsolutePath(), megaFullScreenImageAdapter);
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
			preview = PreviewUtils.getPreviewFromFolder(node, activity);

			if (preview != null){
				return 0;
			}
			else{
				if (activity.getExternalCacheDir() != null){
					cacheDir = activity.getExternalCacheDir();
				}
				else{
					cacheDir = activity.getCacheDir();
				}
				destination = new File(cacheDir, node.getName());

				if (destination.exists()){
					if (destination.length() == node.getSize()){
						File previewDir = PreviewUtils.getPreviewFolder(activity);
						File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
						log("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
						boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);

						if (previewCreated){
							preview = PreviewUtils.getBitmapForCache(previewFile, activity);
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
					log("the image is already downloaded or added to the list - return 1");
					return 1;
				}
				else{
					log("return code 2");
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
				log("There is no preview for this node");
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
        log ("INSTANTIATE POSITION " + position);

		MegaNode node = messages.get(position).getMegaNodeList().get(0);
		
		ViewHolderFullImage holder = new ViewHolderFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		if (node == null){
			Intent intent = new Intent(activity, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. TOUR_FRAGMENT);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        activity.startActivity(intent);
	        activity.finish();
	        return viewLayout;
		}
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setImageResource(MimeTypeMime.typeForName(node.getName()).getIconResourceId());
		holder.imgDisplay.setOnClickListener(this);

		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.progressBar.setVisibility(View.GONE);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		holder.document = messages.get(position).getMegaNodeList().get(0).getHandle();
		
		visibleImgs.put(position, holder);
        
		Bitmap preview = null;
		Bitmap thumb = null;
		
		thumb = ThumbnailUtils.getThumbnailFromCache(node);
		if (thumb != null){
			holder.imgDisplay.setImageBitmap(thumb);
		}
		else{
			thumb = ThumbnailUtils.getThumbnailFromFolder(node, activity);
			if (thumb != null){
				holder.imgDisplay.setImageBitmap(thumb);
			}
		}
		
		if (node.hasPreview()){
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				PreviewUtils.previewCache.put(node.getHandle(), preview);
				holder.imgDisplay.setImageBitmap(preview);
			}
			else{
				try{
					new PreviewAsyncTask().execute(node);
				}
				catch(Exception ex){
					//Too many AsyncTasks
					log("Too many AsyncTasks");
				} 
			}
		}
		else{
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				PreviewUtils.previewCache.put(node.getHandle(), preview);
				holder.imgDisplay.setImageBitmap(preview);
			}
			else{
				try{
					new PreviewDownloadAsyncTask().execute(node);
				}
				catch(Exception ex){
					//Too many AsyncTasks
					log("Too many AsyncTasks");
				}
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
        log ("DESTROY POSITION " + position + " visibleImgs.size(): " + visibleImgs.size());
 
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
	
	private static void log(String log) {
		Util.log("MegaChatFullScreenImageAdapter", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
		log("onRequestStart: Node: " + request.getNodeHandle());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		
		log("onRequestFinish: " + request.getRequestString());
		log("onRequestFinish: Node: " + request.getNodeHandle() + "_" + request.getName());

		long handle = request.getNodeHandle();
		String handleBase64 = MegaApiJava.handleToBase64(handle);

		pendingPreviews.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			File previewDir = PreviewUtils.getPreviewFolder(activity);
			File preview = new File(previewDir, handleBase64+".jpg");
			
			if (preview.exists()) {
				if (preview.length() > 0) {
					log("GET PREVIEW FINISHED. HANDLE: " + handle + " visibleImgs.size(): " + visibleImgs.size());
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
						Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, activity);
						PreviewUtils.previewCache.put(handle, bitmap);						
						visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
						visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					}
				}
			}
		}
		else{
			log("ERROR FINISH: " + e.getErrorCode() + "_" + e.getErrorString());
			try{
//				new PreviewDownloadAsyncTask().execute(node);
			}
			catch(Exception ex){
				//Too many AsyncTasks
				log("Too many AsyncTasks");
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
		log("Node: " + request.getNodeHandle() + "_" + request.getName());
		log("ERROR: " + e.getErrorCode() + "_" + e.getErrorString());
	}


	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		
	}
}
