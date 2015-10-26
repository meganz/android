package nz.mega.android.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mega.android.MegaApplication;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
import nz.mega.android.lollipop.CameraUploadFragmentLollipop.PhotoSyncHolder;
import nz.mega.android.utils.PreviewUtils;
import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.ThumbnailUtilsLollipop;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;


public class MegaPhotoSyncListAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList> implements OnClickListener {
	
	private class Media {
		public String filePath;
		public long timestamp;
	}
	
	private SparseBooleanArray selectedItems;
	
	ViewHolderPhotoSyncList holder = null;
	
	HashMap<Long, String> initDBHM(){
		HashMap<Long, String> hm = new HashMap<Long, String>();
		
		String projection[] = {	MediaColumns.DATA, 
				//MediaColumns.MIME_TYPE, 
				//MediaColumns.DATE_MODIFIED,
				MediaColumns.DATE_MODIFIED};
//		String selection = "(abs(" + MediaColumns.DATE_MODIFIED + "-" + n.getModificationTime() + ") < 3) OR ("+ MediaColumns.DATA + " LIKE '%" + n.getName() + "%')";
		String selection = "";
		log("SELECTION: " + selection);
		ArrayList<Uri> uris = new ArrayList<Uri>();
		uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);	
		String order = MediaColumns.DATE_MODIFIED + " ASC";
		String[] selectionArgs = null;
		
		for(int i=0; i<uris.size(); i++){
			if (app == null){
				app = ((MegaApplication) ((Activity) context).getApplication());
			}
			Cursor cursor = app.getContentResolver().query(uris.get(i), projection, selection, selectionArgs, order);
			if (cursor != null){
				int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
				int timestampColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
				while(cursor.moveToNext()){
					Media media = new Media();
			        media.filePath = cursor.getString(dataColumn);
			        media.timestamp = cursor.getLong(timestampColumn);
			        
			        hm.put(media.timestamp, media.filePath);
				}
			}
		}
		
		return hm;	
	}
	
	HashMap<Long, String> hm;
	
	private class MediaDBTask extends AsyncTask<MegaNode, Void, String> {
		
		Context context;
		MegaApplication app;
		ViewHolderPhotoSyncList holder;
		MegaApiAndroid megaApi;
		MegaPhotoSyncListAdapterLollipop adapter;
		MegaNode node;
		Bitmap thumb = null;
		
		public MediaDBTask(Context context, ViewHolderPhotoSyncList holder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapterLollipop adapter) {
			this.context = context;
			this.app = (MegaApplication)(((Activity)(this.context)).getApplication());
			this.holder = holder;
			this.megaApi = megaApi;
			this.adapter = adapter;
		}
		@Override
		protected String doInBackground(MegaNode... params) {
			this.node = params[0];
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (this.node == null){
				return null;
			}
			
			if (app == null){
				return null;
			}
			
			if (hm == null){
				return null;
			}
			
			boolean thumbCreated = false;
			boolean previewCreated = false;
			
			File previewDir = PreviewUtils.getPreviewFolder(context);
			File thumbDir = ThumbnailUtils.getThumbFolder(context);
			File previewFile = new File(previewDir, MegaApiAndroid.handleToBase64(node.getHandle())+".jpg");
			File thumbFile = new File(thumbDir, MegaApiAndroid.handleToBase64(node.getHandle())+".jpg");
							
			if (!thumbFile.exists()){
		
				log("n.getName(): " + node.getName() + "____" + node.getModificationTime());
				String filePath = hm.get(node.getModificationTime());
				if (filePath != null){
					File f = new File(filePath);
					if (f != null){
						if (f.length() == node.getSize()){
							log("IDEM: " + filePath + "____" + node.getName());
							thumbCreated = MegaUtils.createThumbnail(f, thumbFile);
							if (!node.hasThumbnail()){
								log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
								megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
							}
							else{
								log("Thumbnail OK: " + node.getName() + "___" + thumbFile.getAbsolutePath());
							}
							
							if (!previewFile.exists()){
								previewCreated = MegaUtils.createPreview(f, previewFile);
								if (!node.hasPreview()){
									log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
									megaApi.setPreview(node, previewFile.getAbsolutePath());
								}
							}
							else{
								if (!node.hasPreview()){
									log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
									megaApi.setPreview(node, previewFile.getAbsolutePath());
								}
							}
						}
					}
				}
				else{
					List<String> paths = new ArrayList<String>(hm.values());
					for (int i=0;i<paths.size();i++){
						if (paths.get(i).contains(node.getName())){
							filePath = paths.get(i);
							File f = new File(filePath);
							if (f != null){
								if (f.length() == node.getSize()){
									log("IDEM(por nombre): " + filePath + "____" + node.getName());
									thumbCreated = MegaUtils.createThumbnail(f, thumbFile);
									if (!node.hasThumbnail()){
										log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
										megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
									}
									else{
										log("Thumbnail OK: " + node.getName() + "___" + thumbFile.getAbsolutePath());
									}
									
									if (!previewFile.exists()){
										previewCreated = MegaUtils.createPreview(f, previewFile);
										if (!node.hasPreview()){
											log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
											megaApi.setPreview(node, previewFile.getAbsolutePath());
										}
									}
									else{
										if (!node.hasPreview()){
											log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
											megaApi.setPreview(node, previewFile.getAbsolutePath());
										}
									}
								}
							}
						}
					}
				}
			}
			else{
				thumbCreated = true;
				if (!node.hasThumbnail()){
					log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
					megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
				}
				else{
					log("Thumbnail OK: " + node.getName() + "___" + thumbFile.getAbsolutePath());
				}
			}
			
			if (!previewFile.exists()){
				log("n.getName(): " + node.getName() + "____" + node.getModificationTime());
				String filePath = hm.get(node.getModificationTime());
				if (filePath != null){
					File f = new File(filePath);
					if (f != null){
						if (f.length() == node.getSize()){
							log("IDEM: " + filePath + "____" + node.getName());
							previewCreated = MegaUtils.createPreview(f, previewFile);
							if (!node.hasPreview()){
								log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
								megaApi.setPreview(node, previewFile.getAbsolutePath());
							}
							if (!thumbFile.exists()){
								thumbCreated = MegaUtils.createThumbnail(f, thumbFile);
								if (!node.hasThumbnail()){
									log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
									megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
								}
							}
							else{
								if (!node.hasThumbnail()){
									log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
									megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
								}
							}
						}
					}
				}
			}
			else{
				if (!node.hasPreview()){
					log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
					megaApi.setPreview(node, previewFile.getAbsolutePath());
				}
			}
			
			if (thumbCreated){
				if (thumbFile != null){
					return thumbFile.getAbsolutePath();
				}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(String res) {
			if (res == null){
				log("megaApi.getThumbnail");
				if (this.node != null){
					try {
						thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaPhotoSyncList(node, context, holder, megaApi, adapter);
					} 
					catch (Exception e) {
					} // Too many AsyncTasks
	
					if (thumb != null) {
						if(!multipleSelect){
							if ((holder.document == node.getHandle())){
								holder.imageView.setImageBitmap(thumb);
							}
						}
						else{
							if ((holder.document == node.getHandle())){
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
			}
			else{
				log("From folder: " + res);
				if (this.node != null){
					thumb = ThumbnailUtils.getThumbnailFromCache(node);
					if (thumb != null) {
						if ((holder.document == node.getHandle())){
							holder.imageView.setImageBitmap(thumb);
						}
					} 
					else {
						thumb = ThumbnailUtils
								.getThumbnailFromFolder(node, context);
						if (thumb != null) {
							if ((holder.document == node.getHandle())){
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
				//HE ENCONTRADO LA IMAGEN Y LA PUEDO LEER
			}
//			onKeysGenerated(key[0], key[1]);
		}		
	}
	
	static int FROM_FILE_BROWSER = 13;
	static int FROM_INCOMING_SHARES= 14;
	static int FROM_OFFLINE= 15;
	
	Context context;
	MegaApplication app;
	MegaApiAndroid megaApi;

	ArrayList<PhotoSyncHolder> nodesArray;
	ArrayList<MegaNode> nodes;
	
	long photosyncHandle = -1;
	
	RecyclerView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	boolean multipleSelect;
	
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	Object fragment;
	int type = ManagerActivityLollipop.CAMERA_UPLOAD_ADAPTER;
	
	/*public static view holder class*/
    public class ViewHolderPhotoSyncList extends RecyclerView.ViewHolder {
    	
    	public ViewHolderPhotoSyncList(View v){
    		super(v);
    	}
    	
    	public ImageView imageView;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public RelativeLayout itemLayout;
    	public RelativeLayout monthLayout;
    	public TextView monthTextView;
    	public int currentPosition;
    	public long document;
    }
	
	public MegaPhotoSyncListAdapterLollipop(Context _context, ArrayList<PhotoSyncHolder> _nodesArray, long _photosyncHandle, RecyclerView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, Object fragment, int type) {
		this.context = _context;
		this.nodesArray = _nodesArray;
		this.photosyncHandle = _photosyncHandle;
		this.nodes = _nodes;
		
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		this.fragment = fragment;
		this.type = type;
		
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		this.app = ((MegaApplication) ((Activity) context).getApplication());
		this.hm = initDBHM();
	}
	
	public void setNodes(ArrayList<PhotoSyncHolder> nodesArray, ArrayList<MegaNode> nodes){
		this.nodesArray = nodesArray;
		this.nodes = nodes;
		notifyDataSetChanged();
	}
	
	public void setPhotoSyncHandle(long photoSyncHandle){
		this.photosyncHandle = photoSyncHandle;
		notifyDataSetChanged();
	}

	public Object getItem(int position) {
        return nodesArray.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    @Override
	public void onClick(View v) {
		ViewHolderPhotoSyncList holder = (ViewHolderPhotoSyncList) v.getTag();
		int currentPosition = holder.currentPosition;
		PhotoSyncHolder psH = (PhotoSyncHolder) getItem(currentPosition);
		
		if (megaApi == null){
			return;
		}
		
		MegaNode n = megaApi.getNodeByHandle(psH.handle);
		
		if (n == null){
			return;
		}
		
		switch (v.getId()){
			case R.id.photo_sync_list_item_layout:{
				if (type == ManagerActivityLollipop.CAMERA_UPLOAD_ADAPTER){
					((CameraUploadFragmentLollipop) fragment).itemClick(currentPosition);
				}
				break;
			}
		}		
	}
	
	/*
	 * Get document at specified position
	 */
	public PhotoSyncHolder getDocumentAt(int position) {
		try {
			if(nodesArray != null){
				return nodesArray.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
		
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
		
		if(this.multipleSelect){
			selectedItems = new SparseBooleanArray();
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public long getPhotoSyncHandle(){
		return photosyncHandle;
	}
	
	/*
	 * Get document at specified position
	 */
	public PhotoSyncHolder getNodeAt(int position) {
		try {
			if (nodesArray != null) {
				return nodesArray.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}
	
	/*
	 * Get list of all selected nodes
	 */
	public List<PhotoSyncHolder> getSelectedDocuments() {
		ArrayList<PhotoSyncHolder> nodes = new ArrayList<PhotoSyncHolder>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				PhotoSyncHolder document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					nodes.add(document);
				}
			}
		}
		return nodes;
	}
	
	public void toggleSelection(int pos) {
		log("toggleSelection");
		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);
	}
	
	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}
	
	public void clearSelections() {
		if(selectedItems!=null){
			selectedItems.clear();
		}
		notifyDataSetChanged();
	}
	
	private static void log(String log) {
		Util.log("MegaPhotoSyncListAdapter", log);
	}

	@Override
	public int getItemCount() {
		return nodesArray.size();
	}

	@Override
	public void onBindViewHolder(ViewHolderPhotoSyncList holder, int position) {
		log("onBindViewHolder");
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		holder.currentPosition = position;
		
		if (!multipleSelect){
			holder.itemLayout.setBackgroundColor(Color.WHITE);
		}
		else{
			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}
		}
		
		PhotoSyncHolder psh = (PhotoSyncHolder) getItem(position);
		if (psh.isNode){
			MegaNode node = megaApi.getNodeByHandle(psh.handle);
			holder.document = node.getHandle();
			Bitmap thumb = null;
			
			holder.textViewFileName.setText(node.getName());
			holder.monthLayout.setVisibility(View.GONE);
			holder.itemLayout.setVisibility(View.VISIBLE);
			
			if (node.isFolder()){

			}
			else{
				long nodeSize = node.getSize();
				holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
				holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
				
				if (node.hasThumbnail()){
					thumb = ThumbnailUtils.getThumbnailFromCache(node);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
						if (thumb != null){
							holder.imageView.setImageBitmap(thumb);
						}
						else{ 
//							new MediaDBTask(context, holder, megaApi, this).execute(node);
							try{
								thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaPhotoSyncList(node, context, holder, megaApi, this);
							}
							catch(Exception e){} //Too many AsyncTasks
							
							if (thumb != null){
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
				else{
					thumb = ThumbnailUtils.getThumbnailFromCache(node);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
						if (thumb != null){
							holder.imageView.setImageBitmap(thumb);
						}
						else{ 
							try{
								ThumbnailUtilsLollipop.createThumbnailPhotoSyncList(context, node, holder, megaApi, this);
							}
							catch(Exception e){} //Too many AsyncTasks
						}
					}			
				}
			}
		}
		else{
			holder.monthTextView.setText(psh.monthYear);
			holder.itemLayout.setVisibility(View.GONE);
			holder.monthLayout.setVisibility(View.VISIBLE);
			
		}
	}

	@Override
	public ViewHolderPhotoSyncList onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_sync_list, parent, false);
		
		holder = new ViewHolderPhotoSyncList(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.photo_sync_list_item_layout);
		holder.monthLayout = (RelativeLayout) v.findViewById(R.id.photo_sync_list_month_layout);
		holder.monthTextView = (TextView) v.findViewById(R.id.photo_sync_list_month_name);
		holder.imageView = (ImageView) v.findViewById(R.id.photo_sync_list_thumbnail);
		holder.textViewFileName = (TextView) v.findViewById(R.id.photo_sync_list_filename);
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
		holder.textViewFileSize = (TextView) v.findViewById(R.id.photo_sync_list_filesize);
		
		v.setTag(holder);
		
		holder.itemLayout.setTag(holder);
		holder.itemLayout.setOnClickListener(this);
		
		return holder;
	}
}
