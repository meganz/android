package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mega.android.CameraUploadFragment.PhotoSyncHolder;
import nz.mega.android.MegaBrowserListAdapter.ViewHolderBrowserList;
import nz.mega.android.utils.PreviewUtils;
import nz.mega.android.utils.ThumbnailUtils;
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


public class MegaPhotoSyncListAdapter extends BaseAdapter implements OnClickListener {
	
	private class Media {
		public String filePath;
		public long timestamp;
	}
	
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
		MegaPhotoSyncListAdapter adapter;
		MegaNode node;
		Bitmap thumb = null;
		
		public MediaDBTask(Context context, ViewHolderPhotoSyncList holder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapter adapter) {
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
						thumb = ThumbnailUtils.getThumbnailFromMegaPhotoSyncList(node, context, holder, megaApi, adapter);
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

	int positionClicked;
	ArrayList<PhotoSyncHolder> nodesArray;
	ArrayList<MegaNode> nodes;
	
	long photosyncHandle = -1;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	boolean multipleSelect;
	
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	/*public static view holder class*/
    public class ViewHolderPhotoSyncList {
    	public CheckBox checkbox;
    	public ImageView imageView;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public ImageButton imageButtonThreeDots;
    	public RelativeLayout itemLayout;
    	public RelativeLayout optionsLayout;
    	public ImageView optionDownload;
    	public ImageView optionProperties;
    	public ImageView optionRename;
    	public ImageView optionCopy;
    	public ImageView optionMove;
    	public ImageView optionPublicLink;
    	public ImageView optionDelete;
    	public RelativeLayout monthLayout;
    	public TextView monthTextView;
    	public int currentPosition;
    	public long document;
    }
	
	public MegaPhotoSyncListAdapter(Context _context, ArrayList<PhotoSyncHolder> _nodesArray, long _photosyncHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes) {
		this.context = _context;
		this.nodesArray = _nodesArray;
		this.photosyncHandle = _photosyncHandle;
		this.nodes = _nodes;
		
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		this.app = ((MegaApplication) ((Activity) context).getApplication());
		this.hm = initDBHM();
	}
	
	public void setNodes(ArrayList<PhotoSyncHolder> nodesArray, ArrayList<MegaNode> nodes){
		this.nodesArray = nodesArray;
		this.nodes = nodes;
		positionClicked = -1;	
		notifyDataSetChanged();
	}
	
	public void setPhotoSyncHandle(long photoSyncHandle){
		this.photosyncHandle = photoSyncHandle;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		listFragment = (ListView) parent;
		
		final int _position = position;
		
		ViewHolderPhotoSyncList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_photo_sync_list, parent, false);
			holder = new ViewHolderPhotoSyncList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.photo_sync_list_item_layout);
			holder.monthLayout = (RelativeLayout) convertView.findViewById(R.id.photo_sync_list_month_layout);
			holder.monthTextView = (TextView) convertView.findViewById(R.id.photo_sync_list_month_name);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.photo_sync_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.imageView = (ImageView) convertView.findViewById(R.id.photo_sync_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.photo_sync_list_filename);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.photo_sync_list_filesize);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.photo_sync_list_three_dots);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.photo_sync_list_options);
			holder.optionDownload = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_download);
			holder.optionDownload.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDownload.getLayoutParams()).setMargins(Util.px2dp((9*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionProperties = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_properties);
			holder.optionProperties.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionRename = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_rename);
			holder.optionRename.getLayoutParams().width = Util.px2dp((30*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionRename.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionCopy = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_copy);
			holder.optionCopy.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionCopy.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionMove = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_move);
			holder.optionMove.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionMove.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionPublicLink = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_public_link);
			holder.optionPublicLink.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.photo_sync_list_option_delete);
			holder.optionDelete.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderPhotoSyncList) convertView.getTag();
		}
		
		if (!multipleSelect){
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		}
		else{
			holder.checkbox.setVisibility(View.VISIBLE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			
			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				holder.checkbox.setChecked(true);
			}
			else{
				holder.checkbox.setChecked(false);
			}
		}
		
		holder.currentPosition = position;
		
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
								thumb = ThumbnailUtils.getThumbnailFromMegaPhotoSyncList(node, context, holder, megaApi, this);
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
								ThumbnailUtils.createThumbnailPhotoSyncList(context, node, holder, megaApi, this);
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
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		holder.imageButtonThreeDots.setVisibility(View.GONE);

		if (positionClicked != -1){
			if (positionClicked == position){
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				listFragment.smoothScrollToPosition(_position);
			}
			else{
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
		}
		else{
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
		}
		
		holder.optionDownload.setTag(holder);
		holder.optionDownload.setOnClickListener(this);
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		
		holder.optionRename.setTag(holder);
		holder.optionRename.setOnClickListener(this);
		
		holder.optionCopy.setTag(holder);
		holder.optionCopy.setOnClickListener(this);
		
		holder.optionMove.setTag(holder);
		holder.optionMove.setOnClickListener(this);
		
		holder.optionDelete.setTag(holder);
		holder.optionDelete.setOnClickListener(this);
		
		holder.optionPublicLink.setTag(holder);
		holder.optionPublicLink.setOnClickListener(this);
		
		return convertView;
	}
	
	@Override
	public boolean isEnabled(int position) {

		return super.isEnabled(position);
	}

	@Override
    public int getCount() {
        return nodesArray.size();
    }
 
    @Override
    public Object getItem(int position) {
        return nodesArray.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
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
			case R.id.photo_sync_list_option_download:{
				positionClicked = -1;
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).onFileClick(handleList);
				break;
			}
			case R.id.photo_sync_list_option_properties:{
				Intent i = new Intent(context, FilePropertiesActivity.class);
				i.putExtra("handle", n.getHandle());
				i.putExtra("from", FROM_FILE_BROWSER);
			
				if (n.isFolder()){
					i.putExtra("imageId", R.drawable.ic_folder_list);
				}
				else{
					i.putExtra("imageId", MimeTypeMime.typeForName(n.getName()).getIconResourceId());	
				}				
				i.putExtra("name", n.getName());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.photo_sync_list_option_delete:{
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivity) context).moveToTrash(handleList);
				break;
			}
			case R.id.photo_sync_list_option_public_link:{
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivity) context).getPublicLinkAndShareIt(n);
				break;
			}
			case R.id.photo_sync_list_option_rename:{
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivity) context).showRenameDialog(n, n.getName());
				break;
			}
			case R.id.photo_sync_list_option_move:{
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showMove(handleList);
				break;
			}
			case R.id.photo_sync_list_option_copy:{
				positionClicked = -1;
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showCopy(handleList);
				break;
			}
			case R.id.photo_sync_list_three_dots:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
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
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public long getPhotoSyncHandle(){
		return photosyncHandle;
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserListAdapter", log);
	}
}
