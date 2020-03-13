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
import android.support.annotation.Nullable;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaFullScreenImageAdapterLollipop extends PagerAdapter implements OnClickListener, MegaRequestListenerInterface, MegaTransferListenerInterface {
	
	private Activity activity;
	private MegaFullScreenImageAdapterLollipop megaFullScreenImageAdapter;
	private ArrayList<Long> imageHandles;
	private SparseArray<ViewHolderFullImage> visibleImgs = new SparseArray<ViewHolderFullImage>();
	private boolean aBshown = true;
	private boolean menuVisible = false;
	
	private ArrayList<Long> pendingPreviews = new ArrayList<Long>();
	private ArrayList<Long> pendingFullImages = new ArrayList<Long>();

	MegaApiAndroid megaApiFolder;
	MegaApiAndroid megaApi;
	Context context;
	boolean isFileLink = false;
	MegaNode fileLink = null;
	boolean isFolderLink = false;

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
    
    private class PreviewAsyncTask extends AsyncTask<Long, Void, Integer>{
		
    	long handle;
    	Bitmap preview;
    	
		@Override
		protected Integer doInBackground(Long... params){
			logDebug("PreviewAsyncTask()-doInBackground");
			handle = params[0];
			MegaNode node = megaApi.getNodeByHandle(handle);
			if (node == null) {
				logWarning("Cannot get the preview because the node is NULL.");
				return 3;
			}

			preview = getPreviewFromFolderFullImage(node, activity);
			
			if (preview != null){
				return 0;
			}
			else{
				if (pendingPreviews != null && pendingPreviews.contains(node.getHandle())) {
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
			logDebug("PreviewAsyncTask()-onPostExecute");

			if (param == 0){
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
					previewCache.put(handle, preview);
                    if (preview != null) {
                        visibleImgs.get(position).imgDisplay.setImageBitmap(preview);
                    } else {
                        logWarning("Preview can't be loaded. Device low memory.");
                        Toast.makeText(context, R.string.not_load_preview_low_memory, Toast.LENGTH_SHORT).show();
                    }
					visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
				}
			}
			else if(param == 2){
				MegaNode node = megaApi.getNodeByHandle(handle);
				File previewFile = new File(getPreviewFolder(activity), node.getBase64Handle()+".jpg");
				logDebug("GET PREVIEW OF HANDLE: " + node.getHandle());
				pendingPreviews.add(node.getHandle());
				megaApi.getPreview(node,  previewFile.getAbsolutePath(), megaFullScreenImageAdapter);				
			}
		}
	}
	
	private class PreviewDownloadAsyncTask extends AsyncTask<Long, Void, Integer>{
		
		long handle;
    	Bitmap preview;
    	File destination;

    	
		@Override
		protected Integer doInBackground(Long... params){
			logDebug("PreviewDownloadAsyncTask()-doInBackground");

			handle = params[0];
			MegaNode node = megaApi.getNodeByHandle(handle);
			if (node == null){
				return 3;
			}
			preview = getPreviewFromFolderFullImage(node, activity);
			
			if (preview != null){
				return 0;
			}
			else{
				destination = buildPreviewFile(activity,node.getName());
				
				if (isFileAvailable(destination)){
					if (destination.length() == node.getSize()){
						File previewDir = getPreviewFolder(activity);
						File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
						logDebug("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
						boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);
						
						if (previewCreated){
							preview = getBitmapForCacheFullImage(previewFile, activity);
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
					logDebug("The image is already downloaded or added to the list");
					return 1;
				}
				else{
					return 2;
				}
			}
		}
		
		@Override
		protected void onPostExecute(Integer param){
			logDebug("PreviewDownloadAsyncTask()-onPostExecute");

			if (param == 0 || param == 1){
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
					if(param == 0)
					{
						if (visibleImgs.get(position).isGIF){
                            if (preview != null) {
                                visibleImgs.get(position).gifImgDisplay.setImageBitmap(preview);
                            } else {
                                logWarning("Preview can't be loaded. Device low memory.");
                                Toast.makeText(context, R.string.not_load_preview_low_memory, Toast.LENGTH_SHORT).show();
                            }
						}
						else {
                            if (preview != null) {
                                visibleImgs.get(position).imgDisplay.setImageBitmap(preview);
                            } else {
                                logWarning("Preview can't be loaded. Device low memory.");
                                Toast.makeText(context, R.string.not_load_preview_low_memory, Toast.LENGTH_SHORT).show();
                            }
						}
					}
					if (visibleImgs.get(position).isGIF){
						visibleImgs.get(position).progressBar.setVisibility(View.VISIBLE);
					}
					else {
						visibleImgs.get(position).progressBar.setVisibility(View.GONE);
						visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
					}
				}
			}
			else if (param == 2){
				MegaNode node = megaApi.getNodeByHandle(handle);
				pendingFullImages.add(handle);
				logDebug("Node handle: " + node.getHandle());
				String previewFolder = getCacheFolder(context, PREVIEW_FOLDER).getAbsolutePath() + File.separator;
				megaApi.startDownload(node, previewFolder, megaFullScreenImageAdapter);
			}
		}
	}	
	
	private class AttachPreviewTask extends AsyncTask<String, Void, String[]>{
		
		@Override
		protected String[] doInBackground(String... params) {
			logDebug("AttachPreviewTask()-doInBackground");
			long handle = Long.parseLong(params[0]);
			String localPath = params[1];
			
			MegaNode node = megaApi.getNodeByHandle(handle);
			File fullImage = new File(localPath);
			
			File previewDir = getPreviewFolder(activity);
			File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
			boolean previewCreated = MegaUtilsAndroid.createPreview(fullImage, previewFile);
			
			if (previewCreated){
				fullImage.delete();
			}
			
			return new String[]{params[0], previewCreated+""}; 
		}
		
		@Override
		protected void onPostExecute(String[] params) {
			logDebug("AttachPreviewTask()-onPostExecute");

			long handle = Long.parseLong(params[0]);
			boolean previewCreated = Boolean.parseBoolean(params[1]);
			
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
			
			if (holderIsVisible) {
				if(previewCreated) {
					MegaNode node = megaApi.getNodeByHandle(handle);
					File previewDir = getPreviewFolder(activity);
					File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
					Bitmap bitmap = getBitmapForCacheFullImage(previewFile, activity);
                    if (bitmap != null) {
                        visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
                    } else {
                        logWarning("Preview can't be loaded. Device low memory.");
                        Toast.makeText(context, R.string.not_load_preview_low_memory, Toast.LENGTH_SHORT).show();
                    }
				}
				
				visibleImgs.get(position).progressBar.setVisibility(View.GONE);
				visibleImgs.get(position).downloadProgressBar.setVisibility(View.GONE);
			}
		}
	}
	
	// constructor
	public MegaFullScreenImageAdapterLollipop(Context context ,Activity activity, ArrayList<Long> imageHandles, MegaApiAndroid megaApi) {
		this.activity = activity;
		this.imageHandles = imageHandles;
		this.megaApi = megaApi;
		this.megaFullScreenImageAdapter = this;
		this.context = context;
		this.isFileLink = ((FullScreenImageViewerLollipop) context).isFileLink();
		this.fileLink = ((FullScreenImageViewerLollipop) context).getCurrentDocument();
		this.isFolderLink = ((FullScreenImageViewerLollipop) context).isFolderLink();

		dbH = DatabaseHandler.getDbHandler(context);

		prefs = dbH.getPreferences();

		downloadLocationDefaultPath = getDownloadLocation(context);
	}

	@Override
	public int getCount() {
		return imageHandles.size();
	}

	public void refreshImageHandles(ArrayList<Long> imageHandles){
		logDebug("refreshImageHandles");
		this.imageHandles = imageHandles;
		visibleImgs.clear();
		notifyDataSetChanged();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
	}
	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
		logDebug ("POSITION " + position);

		MegaNode node = megaApi.getNodeByHandle(imageHandles.get(position));
		if (isFolderLink){
			MegaApplication app = (MegaApplication) ((FullScreenImageViewerLollipop) context).getApplication();
			megaApiFolder = app.getMegaApiFolder();
			MegaNode nodeAuth = megaApiFolder.authorizeNode(node);
			if (nodeAuth == null) {
				nodeAuth = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(imageHandles.get(position)));
				node = nodeAuth;
			}
			else {
				node = nodeAuth;
			}
		}
		ViewHolderFullImage holder = new ViewHolderFullImage();
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.item_full_screen_image_viewer, container,false);
		
		if ((node == null)&&(!isFileLink)){
			Intent intent = new Intent(activity, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        activity.startActivity(intent);
	        activity.finish();
	        return viewLayout;
		}

		holder.progressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_progress_bar);
		holder.downloadProgressBar = (ProgressBar) viewLayout.findViewById(R.id.full_screen_image_viewer_download_progress_bar);
		holder.downloadProgressBar.setVisibility(View.GONE);
		holder.document = imageHandles.get(position);
		
		holder.imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_image);
		holder.imgDisplay.setOnClickListener(this);
		holder.gifImgDisplay = (ImageView) viewLayout.findViewById(R.id.full_screen_image_viewer_gif);
		holder.gifImgDisplay.setOnClickListener(this);

		Bitmap thumb = null;
		Bitmap preview = null;

		final ProgressBar pb = holder.progressBar;

		if ((node == null) && isFileLink) {
			logDebug("isFileLink");

			if (MimeTypeList.typeForName(fileLink.getName()).isGIF()) {
				holder.isGIF = true;
				holder.imgDisplay.setVisibility(View.GONE);
				holder.gifImgDisplay.setVisibility(View.VISIBLE);
				holder.progressBar.setVisibility(View.VISIBLE);

				Bitmap resource = setImageResource(fileLink, holder);
				Drawable drawable = null;
				if (resource != null) {
					drawable = new BitmapDrawable(context.getResources(), resource);
				}
				if (drawable == null) {
					drawable = ContextCompat.getDrawable(context, MimeTypeThumbnail.typeForName(fileLink.getName()).getIconResourceId());
				}
				holder.progressBar.setVisibility(View.VISIBLE);

				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				}
				else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}
				String url = megaApi.httpServerGetLocalLink(fileLink);
				if (url != null) {

					if (drawable != null) {
						Glide.with(context)
								.load(Uri.parse(url.toString()))
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
								.load(Uri.parse(url.toString()))
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
			else {
				setImageHolder(holder, fileLink);
			}
		}
		else {
			if (MimeTypeList.typeForName(node.getName()).isGIF()) {
				holder.isGIF = true;
				holder.imgDisplay.setVisibility(View.GONE);
				holder.gifImgDisplay.setVisibility(View.VISIBLE);
				holder.progressBar.setVisibility(View.VISIBLE);

				Bitmap resource = setImageResource(node, holder);
				Drawable drawable = null;
				if (resource != null) {
					drawable = new BitmapDrawable(context.getResources(), resource);
				}
				if (drawable == null) {
					drawable = ContextCompat.getDrawable(context, MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
				}

				boolean isOnMegaDownloads = false;
				String localPath = getLocalFile(context, node.getName(), node.getSize(), downloadLocationDefaultPath);
				logDebug("isOnMegaDownloads: " + isOnMegaDownloads + " nodeName: " + node.getName() + " localPath: " + localPath);
				if (localPath != null && megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))) {

					if (drawable != null) {
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
                    if (dbH == null) {
                        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                    }
                    String url = null;
                    if (dbH != null && dbH.getCredentials() != null) {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart();
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if (mi.totalMem > BUFFER_COMP) {
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        }
                        else {
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                        }
                        url = megaApi.httpServerGetLocalLink(node);
                    }
                    else if (isFolderLink){
                        if (megaApiFolder.httpServerIsRunning() == 0) {
                            megaApiFolder.httpServerStart();
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if (mi.totalMem > BUFFER_COMP) {
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                            megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        }
                        else {
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                            megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                        }
                        url = megaApiFolder.httpServerGetLocalLink(node);
                    }

					if (url != null) {

						if (drawable != null) {
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
				setImageHolder(holder, node);
			}
		}
		visibleImgs.put(position, holder);
		
        ((ViewPager) container).addView(viewLayout);
		
		return viewLayout;
	}

	public void setImageHolder (ViewHolderFullImage holder, MegaNode node) {
		Bitmap thumb = null;
		Bitmap preview = null;

		holder.isGIF = false;
		holder.imgDisplay.setVisibility(View.VISIBLE);
		holder.gifImgDisplay.setVisibility(View.GONE);
		holder.imgDisplay.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

		thumb = getThumbnailFromCache(node);
		if (thumb != null) {
			holder.imgDisplay.setImageBitmap(thumb);
		}
		else {
			thumb = getThumbnailFromFolder(node, activity);
			if (thumb != null) {
				holder.imgDisplay.setImageBitmap(thumb);
			}
		}

		if (node.hasPreview()) {
			preview = getPreviewFromCache(node);
			if (preview != null) {
				previewCache.put(node.getHandle(), preview);
				holder.imgDisplay.setImageBitmap(preview);
				holder.progressBar.setVisibility(View.GONE);
			}
			else {
				try {
					new PreviewAsyncTask().execute(node.getHandle());
				} catch (Exception ex) {
					//Too many AsyncTasks
					logError("Too many AsyncTasks", ex);
				}
			}
		}
		else {
			preview = getPreviewFromCache(node);
			if (preview != null) {
				previewCache.put(node.getHandle(), preview);
				holder.imgDisplay.setImageBitmap(preview);
				holder.progressBar.setVisibility(View.GONE);
			}
			else {
				try {
					new PreviewDownloadAsyncTask().execute(node.getHandle());
				} catch (Exception ex) {
					//Too many AsyncTasks
					logError("Too many AsyncTasks", ex);
				}
			}
		}
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
		else {
			preview = getPreviewFromCache(node);
			if (preview != null) {
				previewCache.put(node.getHandle(), preview);
			}
		}
		if (preview != null) {
			return preview;
		} else if (thumb != null) {
			return thumb;
		} else {
			return null;
		}
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
		logDebug ("position " + position + " visibleImgs.size(): " + visibleImgs.size());
		visibleImgs.remove(position);
        ((ViewPager) container).removeView((RelativeLayout) object);
        System.gc();
    }

	public ImageView getVisibleImage(int position){
		logDebug("position: " + position);
		if (visibleImgs.get(position) != null){
			if (visibleImgs.get(position).isGIF){
				return visibleImgs.get(position).gifImgDisplay;
			}
			else {
				return visibleImgs.get(position).imgDisplay;
			}
		}
		return null;
	}

	public Long getImageHandle(int position) {
		logDebug("position: " + position);
		return imageHandles.get(position);
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
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {

		logDebug("onRequestFinish: " + request.getRequestString());
		logDebug("Node Handle: " + request.getNodeHandle());

		long handle = request.getNodeHandle();
		MegaNode node = api.getNodeByHandle(handle);
		
		pendingPreviews.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			File previewDir = getPreviewFolder(activity);
			File preview = new File(previewDir, node.getBase64Handle()+".jpg");
			
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
						Bitmap bitmap = getBitmapForCacheFullImage(preview, activity);
                        if (bitmap != null) {
                            visibleImgs.get(position).imgDisplay.setImageBitmap(bitmap);
                        } else {
                            logWarning("Preview can't be loaded. Device low memory.");
                            Toast.makeText(context, R.string.not_load_preview_low_memory, Toast.LENGTH_SHORT).show();
                        }
						visibleImgs.get(position).progressBar.setVisibility(View.GONE);
					}
				}
			}
		}
		else{
			logError("ERROR FINISH: " + e.getErrorCode() + "_" + e.getErrorString());
			try{
				new PreviewDownloadAsyncTask().execute(handle);
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
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.isStreamingTransfer()){
			return;
		}

		logDebug("Download started : " + transfer.getNodeHandle() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
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

		if(transfer.isStreamingTransfer()){
			return;
		}

		long handle = transfer.getNodeHandle();
		
		pendingFullImages.remove(handle);
//		pendingThumbnails.remove(handle);
		
		if (e.getErrorCode() == MegaError.API_OK){
			logDebug("Download finished OK: " + transfer.getNodeHandle() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
			
			try{
				new AttachPreviewTask().execute(handle+"", transfer.getPath());
			}
			catch(Exception ex){
				//Too many AsyncTasks
				logError("Too many AsyncTasks", ex);
			}
		}		
			
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
//		log ("Download updated: " + transfer.getFileName() + "_" + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());

		if(transfer.isStreamingTransfer()){
			return;
		}
		
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
//			log("PROGRESS VALUE: " + progressValue);
			visibleImgs.get(position).downloadProgressBar.setProgress(progressValue);
		}
		
		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		logWarning ("TEMPORARY ERROR (" + transfer.getNodeHandle() + "): " + e.getErrorCode() + "_" + e.getErrorString());
//		Util.showErrorAlertDialog("Temporary error: " + e.getErrorString(), true, activity);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}
}
