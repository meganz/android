package nz.mega.android;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mega.android.MegaBrowserListAdapter.ViewHolderBrowserList;
import nz.mega.android.utils.PreviewUtils;
import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUtilsAndroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


public class MegaBrowserNewGridAdapter extends BaseAdapter {
	
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
		ViewHolderBrowserNewGrid holder;
		MegaApiAndroid megaApi;
		MegaBrowserNewGridAdapter adapter;
		MegaNode node;
		Bitmap thumb = null;
		int index;
		
		public MediaDBTask(Context context, ViewHolderBrowserNewGrid holder, MegaApiAndroid megaApi, MegaBrowserNewGridAdapter adapter, int i) {
			this.context = context;
			this.app = (MegaApplication)(((Activity)(this.context)).getApplication());
			this.holder = holder;
			this.megaApi = megaApi;
			this.adapter = adapter;
			this.index = i;
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
							thumbCreated = MegaUtilsAndroid.createThumbnail(f, thumbFile);
							if (!node.hasThumbnail()){
								log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
								megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
							}
							else{
								log("Thumbnail OK: " + node.getName() + "___" + thumbFile.getAbsolutePath());
							}
							
							if (!previewFile.exists()){
								previewCreated = MegaUtilsAndroid.createPreview(f, previewFile);
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
									thumbCreated = MegaUtilsAndroid.createThumbnail(f, thumbFile);
									if (!node.hasThumbnail()){
										log("Upload thumbnail -> " + node.getName() + "___" + thumbFile.getAbsolutePath());
										megaApi.setThumbnail(node, thumbFile.getAbsolutePath());
									}
									else{
										log("Thumbnail OK: " + node.getName() + "___" + thumbFile.getAbsolutePath());
									}
									
									if (!previewFile.exists()){
										previewCreated = MegaUtilsAndroid.createPreview(f, previewFile);
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
							previewCreated = MegaUtilsAndroid.createPreview(f, previewFile);
							if (!node.hasPreview()){
								log("Upload preview -> " + node.getName() + "___" + previewFile.getAbsolutePath());
								megaApi.setPreview(node, previewFile.getAbsolutePath());
							}
							if (!thumbFile.exists()){
								thumbCreated = MegaUtilsAndroid.createThumbnail(f, thumbFile);
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
//					try {
//						thumb = ThumbnailUtils.getThumbnailFromMegaList(node, context, holder, megaApi, adapter);
//					} 
//					catch (Exception e) {
//					} // Too many AsyncTasks
					
					try{
						thumb = ThumbnailUtils.getThumbnailFromMegaNewGrid(node, context, holder, megaApi, adapter, index);
					}
					catch(Exception e) {}
	
//					if (thumb != null) {
//						holder.imageViews.get(indice).setImageBitmap(thumb);
//					}
				}
			}
			else{
				log("From folder");
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
	ArrayList<Integer> imageIds;
	ArrayList<String> names;

	ImageView emptyImageView;
	TextView emptyTextView;
	
	Button leftNewFolder;
	Button rightUploadButton;
	
	TextView contentText;
	
	int numberOfCells;
	
	ArrayList<MegaNode> nodes;
	int positionClicked;
	
	MegaApiAndroid megaApi;
	
	long parentHandle;
	
	ListView listFragment;
	ActionBar aB;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	MegaTransfer currentTransfer = null;
	
	int type = ManagerActivity.FILE_BROWSER_ADAPTER;
	
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	SparseBooleanArray checkedItems = new SparseBooleanArray();
	private ActionMode actionMode;
	boolean multipleSelect = false;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item){
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
				case R.id.cab_menu_rename:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivity) context).showRenameDialog(documents.get(0), documents.get(0).getName());
					}
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
			
			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}
			
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
			if (showTrash){
				if (type == ManagerActivity.FILE_BROWSER_ADAPTER){
					menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_move_to_trash));
				}
				else if (type == ManagerActivity.RUBBISH_BIN_ADAPTER){
					menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_remove));
				}
			}
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
		
	}
	
	public MegaBrowserNewGridAdapter(Context _context, ArrayList<MegaNode> _nodes, long _parentHandle, ListView listView, ActionBar aB, int numberOfCells, int type, int orderGetChildren, ImageView emptyImageView, TextView emptyTextView, Button leftNewFolder, Button rightUploadButton, TextView contentText) {
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.numberOfCells = numberOfCells;
	
		if (type == ManagerActivity.FILE_BROWSER_ADAPTER){
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
		}
		else if (type == ManagerActivity.RUBBISH_BIN_ADAPTER){
			((ManagerActivity)context).setParentHandleRubbish(parentHandle);
		}

		this.listFragment = listView;
		this.emptyImageView = emptyImageView;
		this.emptyTextView = emptyTextView;
		this.leftNewFolder = leftNewFolder;
		this.rightUploadButton = rightUploadButton;
		this.contentText = contentText;
		
		this.aB = aB;
		this.type = type;
		this.orderGetChildren = orderGetChildren;		
		
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		this.app = ((MegaApplication) ((Activity) context).getApplication());
		this.hm = initDBHM();
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		positionClicked = -1;
		notifyDataSetChanged();
		
		contentText.setText(getInfoFolder(megaApi.getNodeByHandle(parentHandle)));
		
		if (getCount() == 0){
			listFragment.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			if (leftNewFolder != null){
				leftNewFolder.setVisibility(View.VISIBLE);
				rightUploadButton.setVisibility(View.VISIBLE);
			}

			if (megaApi.getRootNode().getHandle()==parentHandle) {
				emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
				emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
			} else {
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
		}
		else{
			listFragment.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			if (leftNewFolder != null){
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
			}
		}
	}
	
	/*private view holder class*/
    public class ViewHolderBrowserNewGrid {
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsThumbnail;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
    	public ArrayList<ImageView> imageViews;
    	public ArrayList<LinearLayout> menuLayouts;
    	public ArrayList<LinearLayout> longClickLayoutsSelected;
    	public ArrayList<LinearLayout> longClickLayoutsUnselected;
    	public ArrayList<TextView> fileNameViews;
    	public ArrayList<TextView> fileSizeViews;
    	public ArrayList<ProgressBar> progressBars;
    	public ArrayList<ImageButton> threeDots;
    	
    	public ArrayList<ImageView> optionsDownload;
    	public ArrayList<ImageView> optionsProperties;
    	public ArrayList<ImageView> optionsDelete;
    	public ArrayList<ImageView> optionsOverflow;
    	    	
    	public ArrayList<Long> documents;
    }
    
    ViewHolderBrowserNewGrid holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (convertView == null){
			holder = new ViewHolderBrowserNewGrid();
			holder.relativeLayoutsThumbnail = new ArrayList<RelativeLayout>();
			holder.relativeLayoutsEmpty = new ArrayList<RelativeLayout>();
			holder.imageViews = new ArrayList<ImageView>();
			holder.menuLayouts = new ArrayList<LinearLayout>();
			holder.longClickLayoutsSelected = new ArrayList<LinearLayout>();
			holder.longClickLayoutsUnselected = new ArrayList<LinearLayout>();
			holder.threeDots = new ArrayList<ImageButton>();
			holder.fileNameViews = new ArrayList<TextView>();
			holder.fileSizeViews = new ArrayList<TextView>();
			holder.progressBars = new ArrayList<ProgressBar>();
			
			holder.optionsDownload = new ArrayList<ImageView>();
			holder.optionsProperties = new ArrayList<ImageView>();
			holder.optionsDelete = new ArrayList<ImageView>();
			holder.optionsOverflow = new ArrayList<ImageView>();
						
			holder.documents = new ArrayList<Long>();
			
			convertView = inflater.inflate(R.layout.item_file_grid_list, parent, false);
			
			holder.cellLayout = (LinearLayout) convertView.findViewById(R.id.cell_layout);
			
			for (int i=0;i<numberOfCells;i++){
				View rLView = inflater.inflate(R.layout.cell_grid_fill, holder.cellLayout, false);
				RelativeLayout rL = (RelativeLayout) rLView.findViewById(R.id.cell_item_complete_layout);
				rL.setLayoutParams(new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
				holder.cellLayout.addView(rL);
				
				RelativeLayout rLT = (RelativeLayout) rLView.findViewById(R.id.cell_item_layout);
				holder.relativeLayoutsThumbnail.add(rLT);
				
				RelativeLayout rLE = (RelativeLayout) rLView.findViewById(R.id.cell_item_layout_empty);
				holder.relativeLayoutsEmpty.add(rLE);
				
				LinearLayout mL = (LinearLayout) rLView.findViewById(R.id.cell_menu_layout);
				holder.menuLayouts.add(mL);
				
				LinearLayout lcLS = (LinearLayout) rLView.findViewById(R.id.cell_menu_long_click_selected);
				holder.longClickLayoutsSelected.add(lcLS);
				
				LinearLayout lcLU = (LinearLayout) rLView.findViewById(R.id.cell_menu_long_click_unselected);
				holder.longClickLayoutsUnselected.add(lcLU);
				
				ImageView oDo = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_download);
				holder.optionsDownload.add(oDo);
				
				ImageView oDe = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_delete);
				holder.optionsDelete.add(oDe);
				
				ImageView oP = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_properties);
				holder.optionsProperties.add(oP);
				
				ImageView oO = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_overflow);
				holder.optionsOverflow.add(oO);
								
				ImageButton tD = (ImageButton) rLView.findViewById(R.id.cell_three_dots);
				holder.threeDots.add(tD);
				
				ImageView iV = (ImageView) rLView.findViewById(R.id.cell_thumbnail);
				holder.imageViews.add(iV);
				
				TextView fNV = (TextView) rLView.findViewById(R.id.cell_filename);
				fNV.setEllipsize(TextUtils.TruncateAt.MIDDLE);
				fNV.setSingleLine(true);
				holder.fileNameViews.add(fNV);
				
				TextView fSV = (TextView) rLView.findViewById(R.id.cell_filesize);
				holder.fileSizeViews.add(fSV);
				
				ProgressBar pB = (ProgressBar) rLView.findViewById(R.id.cell__browser_bar);
				holder.progressBars.add(pB);
				
				holder.documents.add(-1l);
			}
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderBrowserNewGrid) convertView.getTag();
		}
		
		for (int i=0;i<numberOfCells;i++){
			int totalPosition = position*numberOfCells + i;
			if (totalPosition > (nodes.size() - 1)){
				holder.relativeLayoutsThumbnail.get(i).setVisibility(View.GONE);
				holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
//				if (holder.documents.size() > i){
					holder.documents.set(i,  -1l);
//				}
//				else{
//					holder.documents.add(i, -1l);
//				}
			}
			else{
				holder.relativeLayoutsThumbnail.get(i).setVisibility(View.VISIBLE);
				holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
				holder.progressBars.get(i).setVisibility(View.GONE);
				
				if (multipleSelect){
					if (isChecked(totalPosition)){
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
				
				if (totalPosition == positionClicked){
					holder.menuLayouts.get(i).setVisibility(View.VISIBLE);
				}
				else{
					holder.menuLayouts.get(i).setVisibility(View.GONE);
				}
				
				MegaNode node = nodes.get(totalPosition);
//				if (holder.documents.size() > i){
					holder.documents.set(i, node.getHandle());
//				}
//				else{
//					holder.documents.add(i, node.getHandle());
//				}				
				holder.fileNameViews.get(i).setText(node.getName());
				if (nodes.get(totalPosition).isFolder()){
					if (megaApi.isShared(node)){
						holder.imageViews.get(i).setImageResource(R.drawable.folder_shared_thumbnail);	
					}
					else{
						holder.imageViews.get(i).setImageResource(R.drawable.folder_thumbnail);	
					}
					holder.fileSizeViews.get(i).setText(getInfoFolder(node));
				}
				else{
					holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
					holder.fileSizeViews.get(i).setText(Util.getSizeString(node.getSize()));
					
					if(mTHash!=null){
						
						MegaTransfer tempT = mTHash.get(node.getHandle());
						
						if (tempT!=null){
							holder.progressBars.get(i).setVisibility(View.VISIBLE);		
							holder.fileSizeViews.get(i).setVisibility(View.GONE);	
							
							double progressValue = 100.0 * tempT.getTransferredBytes() / tempT.getTotalBytes();
							holder.progressBars.get(i).setProgress((int)progressValue);
						}
						
						if (currentTransfer != null){
							if (node.getHandle() == currentTransfer.getNodeHandle()){
								holder.progressBars.get(i).setVisibility(View.VISIBLE);		
								holder.fileSizeViews.get(i).setVisibility(View.GONE);	
								double progressValue = 100.0 * currentTransfer.getTransferredBytes() / currentTransfer.getTotalBytes();
								holder.progressBars.get(i).setProgress((int)progressValue);
							}
						}
						
						if(mTHash.size() == 0){
							holder.progressBars.get(i).setVisibility(View.GONE);		
							holder.fileSizeViews.get(i).setVisibility(View.VISIBLE);	
						}
					}
					
					Bitmap thumb = null;
					if (node.hasThumbnail()){
						thumb = ThumbnailUtils.getThumbnailFromCache(node);
						if (thumb != null){
							holder.imageViews.get(i).setImageBitmap(thumb);
						}
						else{
							thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{ 
//								new MediaDBTask(context, holder, megaApi, this, i).execute(node);
								try{
									thumb = ThumbnailUtils.getThumbnailFromMegaNewGrid(node, context, holder, megaApi, this, i);
								}
								catch(Exception e) {}
								
								if (thumb != null){
									holder.imageViews.get(i).setImageBitmap(thumb);
								}
								else{
									holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
								}
							}
						}
					}
					else{
						thumb = ThumbnailUtils.getThumbnailFromCache(node);
						if (thumb != null){
							holder.imageViews.get(i).setImageBitmap(thumb);
						}
						else{
							thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{ 
								try{
									ThumbnailUtils.createThumbnailNewGrid(context, node, holder, megaApi, this, i);
								}
								catch(Exception e){} //Too many AsyncTasks
							}
						}
					}
				}
			}
		}
		
		for (int i=0;i<holder.imageViews.size();i++){
			final int index = i;
			final int totalPosition = position*numberOfCells + i;
			final int positionFinal = position;
			ImageView iV = holder.imageViews.get(i);
			iV.setTag(holder);
			iV.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder= (ViewHolderBrowserNewGrid) v.getTag();
					
					long handle = holder.documents.get(index);
					onNodeClick(holder, positionFinal, index, totalPosition);
				}
			} );
			
			iV.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					ViewHolderBrowserNewGrid holder= (ViewHolderBrowserNewGrid) v.getTag();
					
					long handle = holder.documents.get(index);
//					RelativeLayout rL = holder.relativeLayoutsThumbnail.get(index);
//					rL.setBackgroundColor(Color.parseColor("#000000"));
//					MegaNode n = megaApi.getNodeByHandle(handle);
//					nodeClicked(handle, totalPosition);
					
					onNodeLongClick(holder, positionFinal, index, totalPosition);
					
					return true;
				}
			});
			
			ImageButton tD = holder.threeDots.get(i);
			tD.setTag(holder);
			tD.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					log("POSITION: " + positionFinal + "___" + index);
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onThreeDotsClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oDo = holder.optionsDownload.get(i);
			oDo.setTag(holder);
			oDo.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onDownloadClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oP = holder.optionsProperties.get(i);
			oP.setTag(holder);
			oP.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onPropertiesClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oDe = holder.optionsDelete.get(i);
			oDe.setTag(holder);
			oDe.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onDeleteClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oO = holder.optionsOverflow.get(i);
			oO.setTag(holder);
			oO.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onOverflowClick(holder, positionFinal, index, totalPosition);
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
	
	public void onOverflowClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		final MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		if ((type == ManagerActivity.FILE_BROWSER_ADAPTER)	|| (type == ManagerActivity.SEARCH_ADAPTER)) {

			AlertDialog moreOptionsDialog;

			String [] optionsString = null;
			if (n.isFolder()){
				optionsString = new String[] {context.getString(R.string.context_share_folder), context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy), context.getString(R.string.context_send_link)}; 
			}
			else{
				optionsString = new String[] {context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy)};
			}
			
			final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_text, android.R.id.text1, optionsString);
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.more_options_overflow);
			builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (n.isFile()){
						which = which + 1;
					}
					switch (which){
						case 0:{
							setPositionClicked(-1);
							notifyDataSetChanged();									
							((ManagerActivity) context).shareFolder(n);
							break;
						}
						case 1:{
							setPositionClicked(-1);
							notifyDataSetChanged();
							((ManagerActivity) context).showRenameDialog(n, n.getName());
							break;
						}
						case 2:{
							setPositionClicked(-1);
							notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());									
							((ManagerActivity) context).showMove(handleList);
							break;
						}
						case 3:{
							setPositionClicked(-1);
							notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());									
							((ManagerActivity) context).showCopy(handleList);
							break;
						}
//						case 4:{
//							setPositionClicked(-1);
//							notifyDataSetChanged();
//							((ManagerActivity) context).getPublicLinkAndShareIt(n);
//							break;
//						}
					}

					dialog.dismiss();
				}
			});
			
			builder.setPositiveButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			moreOptionsDialog = builder.create();
			moreOptionsDialog.show();
			Util.brandAlertDialog(moreOptionsDialog);
		}
	}
	
	public void onDeleteClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(n.getHandle());
		if (type == ManagerActivity.OUTGOING_SHARES_ADAPTER){
			ArrayList<MegaShare> shareList = megaApi.getOutShares(n);				
			((ManagerActivity) context).removeAllSharingContacts(shareList, n);
		}
		else if (type != ManagerActivity.CONTACT_FILE_ADAPTER ) {
			((ManagerActivity) context).moveToTrash(handleList);
		} 
		else {
			((ContactPropertiesMainActivity) context).moveToTrash(handleList);
		}
		this.positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void onPropertiesClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		Intent i = new Intent(context, FilePropertiesActivity.class);
		i.putExtra("handle", n.getHandle());
		i.putExtra("from", FROM_FILE_BROWSER);

		if (n.isFolder()) {
			if (megaApi.isShared(n)){
				i.putExtra("imageId", R.drawable.folder_shared_thumbnail);	
			}
			else{
				i.putExtra("imageId", R.drawable.folder_thumbnail);
			}

		} 
		else {
			i.putExtra("imageId", MimeTypeMime.typeForName(n.getName()).getIconResourceId());
		}
		i.putExtra("name", n.getName());
		context.startActivity(i);
		this.positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void onDownloadClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(n.getHandle());
		if (type == ManagerActivity.CONTACT_FILE_ADAPTER) {
			((ContactPropertiesMainActivity) context).onFileClick(handleList);
		} else if (type == ManagerActivity.FOLDER_LINK_ADAPTER) {
			((FolderLinkActivity) context).onFileClick(handleList);
		} else {
			((ManagerActivity) context).onFileClick(handleList);
		}
		this.positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void onThreeDotsClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		if (!multipleSelect){
			if (positionClicked == totalPosition){
				holder.menuLayouts.get(index).setVisibility(View.GONE);
				this.positionClicked = -1;
				notifyDataSetChanged();
			}
			else{
				holder.menuLayouts.get(index).setVisibility(View.VISIBLE);
				this.positionClicked = totalPosition;
				notifyDataSetChanged();
			}
		}
	}
	
	private String getInfoFolder (MegaNode n){
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		
		return info;
	}

	@Override
    public int getCount() {
		float numberOfRows = (float)(nodes.size()) / (float)numberOfCells;
		
		if (numberOfRows > (int)numberOfRows){
			numberOfRows = (int)numberOfRows + 1;
		}
		return (int)numberOfRows;
    }
 
    @Override
    public Object getItem(int position) {
        return nodes.get(position);
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
    
    public void onNodeLongClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
    	log("onNodeLongClick");
    	if (!multipleSelect){
	    	if (positionClicked == -1){
	        	clearSelections();
	        	actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
	        	checkedItems.append(totalPosition, true);
	        	this.multipleSelect = true;
				updateActionModeTitle();
				notifyDataSetChanged();
	    	}
    	}
    	else{
    		onNodeClick(holder, position, index, totalPosition);
    	}
    }
    
    /*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
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
    
    public void onNodeClick(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
    	
    	if (!multipleSelect){
	    	MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
	    	
	    	if (n.isFolder()){
	    		
	    		if (Util.CREATE_THUMB_PREVIEW_SERVICE){
		    		if (context != null){
		    			Intent intent = new Intent(context, CreateThumbPreviewService.class);
		    			intent.putExtra(CreateThumbPreviewService.EXTRA_PARENT_HASH, n.getHandle());
		    			context.startService(intent);
		    		}
	    		}
				
				if ((n.getName().compareTo(CameraSyncService.CAMERA_UPLOADS) == 0) && (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT)){
					((ManagerActivity)context).cameraUploadsClicked();
					return;
				}
				
				aB.setTitle(n.getName());
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
	
				parentHandle = n.getHandle();
				if (type == ManagerActivity.FILE_BROWSER_ADAPTER){
					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
				}
				else if (type == ManagerActivity.RUBBISH_BIN_ADAPTER){
					((ManagerActivity)context).setParentHandleRubbish(parentHandle);
				}

				nodes = megaApi.getChildren(n, orderGetChildren);
				setNodes(nodes);
				listFragment.setSelection(0);
			}
			else{
				if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewer.class);
					intent.putExtra("position", totalPosition);
					if (type == ManagerActivity.FILE_BROWSER_ADAPTER){
						intent.putExtra("adapterType", ManagerActivity.FILE_BROWSER_ADAPTER);
						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						}
						intent.putExtra("orderGetChildren", orderGetChildren);
					}
					else if (type == ManagerActivity.RUBBISH_BIN_ADAPTER){
						intent.putExtra("adapterType", ManagerActivity.RUBBISH_BIN_ADAPTER);
						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_RUBBISH){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						}
						intent.putExtra("orderGetChildren", orderGetChildren);
					}
					else if (type == ManagerActivity.SHARED_WITH_ME_ADAPTER){
						intent.putExtra("adapterType", ManagerActivity.SHARED_WITH_ME_ADAPTER);
						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_INCOMING){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						}
					}
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
    	else{
			if (checkedItems.get(totalPosition, false) == false){
				checkedItems.append(totalPosition, true);
			}
			else{
				checkedItems.append(totalPosition, false);
			}				
			updateActionModeTitle();
			notifyDataSetChanged();
    	}
    }
	
	public long getParentHandle(){
		return parentHandle;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (type == ManagerActivity.FILE_BROWSER_ADAPTER){
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
		}
		else if (type == ManagerActivity.RUBBISH_BIN_ADAPTER){
			((ManagerActivity)context).setParentHandleRubbish(parentHandle);
		}	
	}
	
    public void setTransfers(HashMap<Long, MegaTransfer> _mTHash)
    {
    	this.mTHash = _mTHash;
    	notifyDataSetChanged();
    }
    
    public void setCurrentTransfer(MegaTransfer mT)
    {
    	this.currentTransfer = mT;
    	MegaNode nodeT = megaApi.getNodeByHandle(mT.getNodeHandle());
    	if (megaApi.getParentNode(nodeT) != null){
	    	if(megaApi.getParentNode(nodeT).getHandle()==parentHandle){    		
	    		notifyDataSetChanged();    		
	    	}
    	}
    } 
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public boolean isMultipleSelect(){
		return multipleSelect;
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserNewGridAdapter", log);
	}
}
