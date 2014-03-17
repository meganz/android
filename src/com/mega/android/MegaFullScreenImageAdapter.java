package com.mega.android;

import java.io.File;
import java.util.ArrayList;

import com.mega.android.PreviewUtils.AttachPreviewTask;
import com.mega.android.PreviewUtils.ResizerParams;
import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.MegaUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import android.widget.Toast;

public class MegaFullScreenImageAdapter extends PagerAdapter implements OnClickListener, MegaRequestListenerInterface, MegaTransferListenerInterface  {
	
	private Activity activity;
	private ArrayList<Long> imageHandles;
	private SparseArray<ViewHolderFullImage> visibleImgs = new SparseArray<ViewHolderFullImage>();
	private boolean aBshown = true;
	
	private ArrayList<Long> pendingPreviews = new ArrayList<Long>();
	private ArrayList<Long> pendingFullImages = new ArrayList<Long>();
	
	MegaApiAndroid megaApi;
	
	/*public static view holder class*/
    public class ViewHolderFullImage {
        TouchImageView imgDisplay;
        ProgressBar progressBar;
        ProgressBar downloadProgressBar;
        long document;
        int position;
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
        log ("INSTANTIATE POSITION " + position);

		MegaNode node = megaApi.getNodeByHandle(imageHandles.get(position));
				
		ViewHolderFullImage holder = new ViewHolderFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		holder = new ViewHolderFullImage();			
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		holder.document = imageHandles.get(position);
		
		visibleImgs.put(position, holder);
        
		Bitmap preview = null;
		Bitmap thumb = null;
		
//		if(!Util.isOnline(activity)){
//			Util.showErrorAlertDialog(activity.getString(R.string.error_server_connection_problem),	false, activity);
//			return viewLayout;
//		}
		
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
				holder.imgDisplay.setImageBitmap(preview);
				holder.progressBar.setVisibility(View.GONE);
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, activity);
				if (preview != null){
					holder.imgDisplay.setImageBitmap(preview);
					holder.progressBar.setVisibility(View.GONE);
				}
				else{
					if (pendingPreviews.contains(node.getHandle())){
						log("the preview is already downloaded or added to the list");
					}
					else{
						File previewFile = new File(PreviewUtils.getPreviewFolder(activity), node.getBase64Handle());
						log("INICIA LA DESCARGA DE " + node.getHandle());
						pendingPreviews.add(node.getHandle());
						megaApi.getPreview(node,  previewFile.getAbsolutePath(), this);   
					}
				}
			}
			holder.imgDisplay.setOnClickListener(this);
		}
		else{
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				holder.imgDisplay.setImageBitmap(preview);
				holder.progressBar.setVisibility(View.GONE);
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, activity);
				if (preview != null){
					holder.imgDisplay.setImageBitmap(preview);
					holder.progressBar.setVisibility(View.GONE);
				}
				else{
					//Download the image and create the preview
					File cacheDir = activity.getCacheDir();
					File dir = new File (cacheDir, node.getBase64Handle());
					dir.mkdir();
					dir.setReadable(true, false);
					dir.setExecutable(true, false);
					dir.setLastModified(System.currentTimeMillis());
					File destination = new File(dir, node.getName());
					
					if (destination.exists()){
						if (destination.length() == node.getSize()){
							
							try{
								new AttachPreviewTask().execute(node.getHandle()+"", destination.getAbsolutePath());
							}
							catch(Exception ex){
								//Too many AsyncTasks
								log("Too many AsyncTasks");
							}
							
//							File previewDir = PreviewUtils.getPreviewFolder(activity);
//							File previewFile = new File(previewDir, node.getBase64Handle());
//							log("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
//							boolean previewCreated = MegaUtils.createPreview(destination, previewFile);
//							
//							if (previewCreated){
//								Bitmap bitmap = PreviewUtils.getBitmapForCache(previewFile, activity);
//								visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
//								visibleImgs.get(position).progressBar.setVisibility(View.GONE);
//							}
						}
						else{
							destination.delete();
						}
					}
					else{
						if (pendingFullImages.contains(node.getHandle())){
							log("the image is already downloaded or added to the list");
						}
						else{
							pendingFullImages.add(node.getHandle());
							log("Dir: " + dir);
							log("document.name: " +  node.getName() + "_handle: " + node.getHandle());
							log("destination.getabsolutepath: " + destination.getAbsolutePath());
							megaApi.startDownload(node, dir.getAbsolutePath() + "/", this);
						}
					}
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
		Util.log("MegaFullScreenImageAdapter", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {

		long handle = request.getNodeHandle();
		MegaNode node = api.getNodeByHandle(handle);
		
		pendingPreviews.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			File previewDir = PreviewUtils.getPreviewFolder(activity);
			File preview = new File(previewDir, node.getBase64Handle());
			
			if (preview.exists()) {
				if (preview.length() > 0) {
					log("DESCARGADO EL HANDLE: " + handle + " SPARSE SIZE: " + visibleImgs.size());
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
						visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
						visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		log("Download started : " + transfer.getFileName() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
		long handle = transfer.getNodeHandle();
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
			visibleImgs.get(position).downloadProgressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		long handle = transfer.getNodeHandle();
		
		pendingFullImages.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			log ("Download finished OK: " + transfer.getFileName() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
			
			try{
				new AttachPreviewTask().execute(handle+"", transfer.getPath());
			}
			catch(Exception ex){
				//Too many AsyncTasks
				log("Too many AsyncTasks");
			}
			
//			int position = 0;
//			boolean holderIsVisible = false;
//			for(int i = 0; i < visibleImgs.size(); i++) {
//				position = visibleImgs.keyAt(i);
//				ViewHolderFullImage holder = visibleImgs.get(position);
//				if (holder.document == handle){
//					holderIsVisible = true;
//					break;
//				}
//			}
//			
//			if (holderIsVisible){
//				String localPath = transfer.getPath();
//				File fullImage = new File(localPath);
//				
//				File previewDir = PreviewUtils.getPreviewFolder(activity);
//				File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
//				boolean previewCreated = MegaUtils.createPreview(fullImage, previewFile);
//				
//				if (previewCreated){
//					Bitmap bitmap = PreviewUtils.getBitmapForCache(previewFile, activity);
//					visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
//					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
//					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
//				}
//			}
		}		
			
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log ("Download updated: " + transfer.getFileName() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
		
		long handle = transfer.getNodeHandle();
		
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
			visibleImgs.get(position).downloadProgressBar.setVisibility(View.VISIBLE);
			int progressValue;
			if (transfer.getTotalBytes() == 0){
				progressValue = 100;
			}
			else{
				progressValue = (int) ((transfer.getTransferredBytes()/(float)transfer.getTotalBytes()) * 100);
			}
			log("PROGRESS VALUE: " + progressValue);
			visibleImgs.get(position).downloadProgressBar.setProgress(progressValue);
		}
		
		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log ("TEMPORARY ERROR (" + transfer.getFileName() + "): " + e.getErrorCode() + "_" + e.getErrorString() + "_" + e.getNextAttempt());
		Util.showErrorAlertDialog("Temporary error: " + e.getErrorString(), true, activity);
	}
	
	
	private class AttachPreviewTask extends AsyncTask<String, Void, String[]>{
		
		@Override
		protected String[] doInBackground(String... params) {
			log("AttachPreviewStart");
			long handle = Long.parseLong(params[0]);
			String localPath = params[1];
			
			MegaNode node = megaApi.getNodeByHandle(handle);
			File fullImage = new File(localPath);
			
			File previewDir = PreviewUtils.getPreviewFolder(activity);
			File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
			boolean previewCreated = MegaUtils.createPreview(fullImage, previewFile);
			
			return new String[]{params[0], previewCreated+""}; 
		}
		
		@Override
		protected void onPostExecute(String[] params) {
			long handle = Long.parseLong(params[0]);
			boolean previewCreated = Boolean.parseBoolean(params[1]);
			
			if (previewCreated){
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
					MegaNode node = megaApi.getNodeByHandle(handle);
					File previewDir = PreviewUtils.getPreviewFolder(activity);
					File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
					Bitmap bitmap = PreviewUtils.getBitmapForCache(previewFile, activity);
					visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
				}
			}
		}
	}
}
