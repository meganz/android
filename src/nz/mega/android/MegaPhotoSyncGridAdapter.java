package nz.mega.android;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mega.android.utils.PreviewUtils;
import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MegaPhotoSyncGridAdapter extends BaseAdapter {
	
	
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
		ViewHolderPhotoSyncGrid holder;
		MegaApiAndroid megaApi;
		MegaPhotoSyncGridAdapter adapter;
		MegaNode node;
		Bitmap thumb = null;
		int index;
		
		public MediaDBTask(Context context, ViewHolderPhotoSyncGrid holder, MegaApiAndroid megaApi, MegaPhotoSyncGridAdapter adapter, int index) {
			this.context = context;
			this.app = (MegaApplication)(((Activity)(this.context)).getApplication());
			this.holder = holder;
			this.megaApi = megaApi;
			this.adapter = adapter;
			this.index = index;
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
						thumb = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(node, context, holder, megaApi, adapter, index);
					} 
					catch (Exception e) {
					} // Too many AsyncTasks
	
					if (this.node != null){
						thumb = ThumbnailUtils.getThumbnailFromCache(node);
						if (thumb != null) {
							if (holder.documents.get(index) == this.node.getHandle()){
								holder.imageViews.get(index).setImageBitmap(thumb);
							}
						} else {
							thumb = ThumbnailUtils
									.getThumbnailFromFolder(node, context);
							if (holder.documents.get(index) == this.node.getHandle()){
								holder.imageViews.get(index).setImageBitmap(thumb);
							}
						}
					}
				}
			}
			else{
				log("From folder: " + res);
				if (this.node != null){
					thumb = ThumbnailUtils.getThumbnailFromCache(node);
					if (this.node != null){
						thumb = ThumbnailUtils.getThumbnailFromCache(node);
						if (thumb != null) {
							if (holder.documents.get(index) == this.node.getHandle()){
								holder.imageViews.get(index).setImageBitmap(thumb);
							}
						} else {
							thumb = ThumbnailUtils
									.getThumbnailFromFolder(node, context);
							if (holder.documents.get(index) == this.node.getHandle()){
								holder.imageViews.get(index).setImageBitmap(thumb);
							}
						}
					}
				}
				//HE ENCONTRADO LA IMAGEN Y LA PUEDO LEER
			}
//			onKeysGenerated(key[0], key[1]);
		}		
	}
	
	Context context;
	MegaApplication app;
	
	ArrayList<MegaNode> nodes;
	ArrayList<MegaMonthPic> monthPics;
	int positionClicked;
	
	MegaApiAndroid megaApi;
	
	long photoSyncHandle = -1;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	int numberOfCells;
	
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	boolean multipleSelect = false;
	SparseBooleanArray checkedItems = new SparseBooleanArray();
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			List<MegaNode> documents = getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).onFileClick(handleList);
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).showCopy(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).showMove(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivity) context).getPublicLinkAndShareIt(documents.get(0));
					}
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).moveToTrash(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			log("onDestroyActionMode");	
			multipleSelect = false;
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = getSelectedDocuments();
			
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			
			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
				showLink = true;
			}
			
			if (selected.size() != 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
				showCopy = true;
				
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
				
				if(selected.size() == nodes.size()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
		
	}
	
	public MegaPhotoSyncGridAdapter(Context _context, ArrayList<MegaMonthPic> _monthPics, long _photosyncHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, int numberOfCells) {
		this.context = _context;
		this.monthPics = _monthPics;
		this.photoSyncHandle = _photosyncHandle;
		this.nodes = _nodes;
		
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.numberOfCells = numberOfCells;
		
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		this.hm = initDBHM();
	}
	
	public void setNodes(ArrayList<MegaMonthPic> monthPics, ArrayList<MegaNode> nodes){
		
		this.monthPics = monthPics;
		this.nodes = nodes;
		positionClicked = -1;	
//		log("setNodes: " + numberOfCells + "__" + nodes.size() + "__" + monthPics.size());
		notifyDataSetChanged();
//		listFragment.clearFocus();
//		if (listFragment != null){
//			listFragment.post(new Runnable() {
//                @Override
//                public void run() {                	
//                    listFragment.setSelection(0);
//                }
//            });
//		}
//		list.smoothScrollToPosition(0);
	}
	
	public void setPhotoSyncHandle(long photoSyncHandle){
		this.photoSyncHandle = photoSyncHandle;
		notifyDataSetChanged();
	}
	
	/*private view holder class*/
    public class ViewHolderPhotoSyncGrid {
    	
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsComplete;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
    	public ArrayList<ImageView> imageViews;
    	public TextView textView;
    	public RelativeLayout textRelativeLayout;
    	public ArrayList<LinearLayout> longClickLayoutsSelected;
    	public ArrayList<LinearLayout> longClickLayoutsUnselected;
    	
    	public ArrayList<Long> documents;
    }
    
    ViewHolderPhotoSyncGrid holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (convertView == null){
			holder = new ViewHolderPhotoSyncGrid();
			holder.relativeLayoutsEmpty = new ArrayList<RelativeLayout>();
			holder.relativeLayoutsComplete = new ArrayList<RelativeLayout>();
			holder.imageViews = new ArrayList<ImageView>();
			holder.longClickLayoutsSelected = new ArrayList<LinearLayout>();
			holder.longClickLayoutsUnselected = new ArrayList<LinearLayout>();
			
			holder.documents = new ArrayList<Long>();

			convertView = inflater.inflate(R.layout.item_photo_sync_grid, parent, false);

			holder.cellLayout = (LinearLayout) convertView.findViewById(R.id.cell_photosync_layout);
			
			for (int i=0;i<numberOfCells;i++){
				View rLView = inflater.inflate(R.layout.cell_photosync_grid_fill, holder.cellLayout, false);
				
				RelativeLayout rL = (RelativeLayout) rLView.findViewById(R.id.cell_photosync_grid_item_complete_layout);
				rL.setLayoutParams(new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
				holder.cellLayout.addView(rL);
				holder.relativeLayoutsComplete.add(rL);
				
				RelativeLayout rLE = (RelativeLayout) rLView.findViewById(R.id.cell_photosync_item_layout_empty);
				holder.relativeLayoutsEmpty.add(rLE);
				
				ImageView iV = (ImageView) rLView.findViewById(R.id.cell_photosync_grid_thumbnail);
				holder.imageViews.add(iV);
				
				LinearLayout lcLS = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_selected);
				holder.longClickLayoutsSelected.add(lcLS);
				
				LinearLayout lcLU = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_unselected);
				holder.longClickLayoutsUnselected.add(lcLU);
				
				holder.documents.add(-1l);
			}
			
			holder.textRelativeLayout = (RelativeLayout) convertView.findViewById(R.id.cell_photosync_grid_month_layout);
			
			holder.textView = (TextView) convertView.findViewById(R.id.cell_photosync_grid_month_name);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderPhotoSyncGrid) convertView.getTag();
		}
		
		MegaMonthPic monthPic = (MegaMonthPic) getItem(position);
		
		if (monthPic.monthYearString != null){
			if (monthPic.monthYearString.compareTo("") != 0){
				holder.textRelativeLayout.setVisibility(View.VISIBLE);
				holder.textView.setText(monthPic.monthYearString);
				for (int i=0;i<numberOfCells;i++){
					holder.relativeLayoutsComplete.get(i).setVisibility(View.GONE);
					holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
				}
			}
			else{
				holder.textRelativeLayout.setVisibility(View.GONE);
				for (int i=0;i<numberOfCells;i++){
					if (monthPic.nodeHandles.size() > i){
						MegaNode n = megaApi.getNodeByHandle(monthPic.nodeHandles.get(i));
						int positionInNodes = 0;
						for (int j=0;j<nodes.size();j++){
							if(nodes.get(j).getHandle() == monthPic.nodeHandles.get(i)){
								positionInNodes = j;
								break;
							}
						}
						
						if (multipleSelect){
							if (isChecked(positionInNodes)){
								holder.longClickLayoutsSelected.get(i).setVisibility(View.VISIBLE);
								holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
							}
							else{
								holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
								holder.longClickLayoutsUnselected.get(i).setVisibility(View.VISIBLE);
							}
						}
						else{
							holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
							holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
						}
						
						holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
						holder.imageViews.get(i).setVisibility(View.VISIBLE);
						holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
//						if (holder.documents.size() > i){
							holder.documents.set(i, n.getHandle());
//						}
//						else{
//							holder.documents.add(i, n.getHandle());
//						}
						
						Bitmap thumb = null;						
						holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());	
						if (n.hasThumbnail()){
							thumb = ThumbnailUtils.getThumbnailFromCache(n);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{
								thumb = ThumbnailUtils.getThumbnailFromFolder(n, context);
								if (thumb != null){
									holder.imageViews.get(i).setImageBitmap(thumb);
								}
								else{
//									new MediaDBTask(context, holder, megaApi, this, i).execute(n);
									try{
										thumb = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(n, context, holder, megaApi, this, i);
									}
									catch(Exception e){} //Too many AsyncTasks
									
									if (thumb != null){
										holder.imageViews.get(i).setImageBitmap(thumb);
									}
									else{
										holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
									}
								}
							}
						}
					}
					else{
						holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
						holder.imageViews.get(i).setVisibility(View.GONE);
						holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
//						if (holder.documents.size() > i){
							holder.documents.set(i,  -1l);
//						}
//						else{
//							holder.documents.add(i, -1l);
//						}
					}
				}
			}
				
		}		
		else{
			holder.textRelativeLayout.setVisibility(View.GONE);
			for (int i=0;i<numberOfCells;i++){
				
				if (monthPic.nodeHandles.size() > i){
					MegaNode n = megaApi.getNodeByHandle(monthPic.nodeHandles.get(i));
					if (n == null){
						Intent intent = new Intent(context, TourActivity.class);
				        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				        context.startActivity(intent);
				        if (context instanceof Activity){
				        	((Activity)context).finish();
				        }
				        return convertView;
					}
					int positionInNodes = 0;
					for (int j=0;j<nodes.size();j++){
						if(nodes.get(j).getHandle() == monthPic.nodeHandles.get(i)){
							positionInNodes = j;
							break;
						}
					}
					
					if (multipleSelect){
						if (isChecked(positionInNodes)){
							holder.longClickLayoutsSelected.get(i).setVisibility(View.VISIBLE);
							holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
						}
						else{
							holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
							holder.longClickLayoutsUnselected.get(i).setVisibility(View.VISIBLE);
						}
					}
					else{
						holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
						holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
					}
					
					holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
					holder.imageViews.get(i).setVisibility(View.VISIBLE);
					holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
//					if (holder.documents.size() > i){
						holder.documents.set(i, n.getHandle());
//					}
//					else{
//						holder.documents.add(i, n.getHandle());
//					}
					
					Bitmap thumb = null;					
					holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
					if (n.hasThumbnail()){
						thumb = ThumbnailUtils.getThumbnailFromCache(n);
						if (thumb != null){
							holder.imageViews.get(i).setImageBitmap(thumb);
						}
						else{
							thumb = ThumbnailUtils.getThumbnailFromFolder(n, context);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{ 
//								new MediaDBTask(context, holder, megaApi, this, i).execute(n);
								try{
									thumb = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(n, context, holder, megaApi, this, i);
								}
								catch(Exception e){} //Too many AsyncTasks
								
								if (thumb != null){
									holder.imageViews.get(i).setImageBitmap(thumb);
								}
								else{
									holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
								}
							}
						}
					}
				}
				else{
					holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
					holder.imageViews.get(i).setVisibility(View.GONE);
					holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
//					if (holder.documents.size() > i){
						holder.documents.set(i,  -1l);
//					}
//					else{
//						holder.documents.add(i, -1l);
//					}
				}				
			}
		}
		
		
		for (int i=0; i< holder.imageViews.size(); i++){
			final int index = i;
			final int positionFinal = position;
			ImageView iV = holder.imageViews.get(i);
			iV.setTag(holder);
			iV.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderPhotoSyncGrid holder= (ViewHolderPhotoSyncGrid) v.getTag();
					
					long handle = holder.documents.get(index);
					
					MegaNode n = megaApi.getNodeByHandle(handle);
					if (n != null){
						int positionInNodes = 0;
						for (int i=0;i<nodes.size();i++){
							if(nodes.get(i).getHandle() == n.getHandle()){
								positionInNodes = i;
								break;
							}
						}
						
						onNodeClick(holder, positionFinal, index, positionInNodes);
					}
				}
			} );
			
			iV.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					ViewHolderPhotoSyncGrid holder= (ViewHolderPhotoSyncGrid) v.getTag();
					
					long handle = holder.documents.get(index);
					
					MegaNode n = megaApi.getNodeByHandle(handle);
					if (n != null){
						int positionInNodes = 0;
						for (int i=0;i<nodes.size();i++){
							if(nodes.get(i).getHandle() == n.getHandle()){
								positionInNodes = i;
								break;
							}
						}
						
						onNodeLongClick(holder, positionFinal, index, positionInNodes);
					}
					
					return true;
				}
			});
		}
		
		
		return convertView;
	}	
	
	public boolean isChecked(int totalPosition){
		
		if (!multipleSelect){
			return false;
		}
		else{
			if (checkedItems.get(totalPosition, false) == false){
				return false;
			}
			else{
				return true;
			}	
		}
	}
	
	public void onNodeLongClick(ViewHolderPhotoSyncGrid holder, int position, int index, int positionInNodes){
		log("onNodeLongClick");
		if (!multipleSelect){
			if (positionClicked == -1){
				clearSelections();
				actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
				checkedItems.append(positionInNodes, true);
				this.multipleSelect = true;
				updateActionModeTitle();
				notifyDataSetChanged();
			}
		}
		else{
    		onNodeClick(holder, position, index, positionInNodes);
    	}
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		this.multipleSelect = false;
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void selectAll(){
		actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());

		this.multipleSelect = true;
		if (nodes != null){
			for ( int i=0; i< nodes.size(); i++ ) {
				checkedItems.append(i, true);
			}
		}
		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		log("clearSelections");
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				checkedItems.append(checkedPosition, false);
			}
		}
		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null){
			log("actionMode null");
			return;
		}
		
		if (context == null){
			log("context null");
			return;
		}
		
		List<MegaNode> documents = getSelectedDocuments();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}
		Resources res = context.getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = "";
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
		// actionMode.
	}
	
	/*
	 * Get list of all selected documents
	 */
	private List<MegaNode> getSelectedDocuments() {
		log("getSelectedDocuments");
		ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaNode document = null;
				try {
					if (nodes != null) {
						document = nodes.get(checkedItems.keyAt(i));
					}
				}
				catch (IndexOutOfBoundsException e) {}
				
				if (document != null){
					documents.add(document);
				}
			}
		}
		
		return documents;
	}
	
	public void onNodeClick(ViewHolderPhotoSyncGrid holder, int position, int index, int positionInNodes){
		
		if (!multipleSelect){
			MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
			if (n != null){
				if (!n.isFolder()){
					if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){
						
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", positionInNodes);
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						intent.putExtra("adapterType", ManagerActivity.PHOTO_SYNC_ADAPTER);
						intent.putExtra("orderGetChildren", orderGetChildren);
						context.startActivity(intent);
					}
					else if (MimeTypeThumbnail.typeForName(n.getName()).isVideo() || MimeTypeThumbnail.typeForName(n.getName()).isAudio() ){
						MegaNode file = n;
						Intent service = new Intent(context, MegaStreamingService.class);
				  		context.startService(service);
				  		String fileName = file.getName();
						try {
							fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
						} 
						catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						
				  		String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
				  		String mimeType = MimeTypeThumbnail.typeForName(file.getName()).getType();
				  		System.out.println("FILENAME: " + fileName);
				  		
				  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
				  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
				  		if (ManagerActivity.isIntentAvailable(context, mediaIntent)){
				  			context.startActivity(mediaIntent);
				  		}
				  		else{
				  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
				  			ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivity) context).onFileClick(handleList);
				  		}						
					}
					else{
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(n.getHandle());
						((ManagerActivity) context).onFileClick(handleList);
					}	
					positionClicked = -1;
					notifyDataSetChanged();
				}
			}
		}
		else{
			if (checkedItems.get(positionInNodes, false) == false){
				checkedItems.append(positionInNodes, true);
			}
			else{
				checkedItems.append(positionInNodes, false);
			}				
			updateActionModeTitle();
			notifyDataSetChanged();
		}
	}

	@Override
    public int getCount() {
		return monthPics.size();
    }
 
    @Override
    public Object getItem(int position) {
        return monthPics.get(position);
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
	
	public long getPhotoSyncHandle(){
		return photoSyncHandle;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	private static void log(String log) {
		Util.log("MegaPhotoSyncGridAdapter", log);
	}
}
