package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MegaMonthPicLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaPhotoSyncGridAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncGridAdapterLollipop.ViewHolderPhotoSyncGrid> {

	private class Media {
		public String filePath;
		public long timestamp;
	}

	public static int PADDING_GRID_LARGE = 6;
	public static int PADDING_GRID_SMALL = 3;
	
	private ViewHolderPhotoSyncGrid holder = null;

	private Context context;
	private MegaApplication app;
	private MegaApiAndroid megaApi;

	ArrayList<MegaMonthPicLollipop> monthPics;
	ArrayList<MegaNode> nodes;

	long photosyncHandle = -1;

	RecyclerView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;

	int numberOfCells;
	int gridWidth;

	boolean multipleSelect;

	SparseBooleanArray checkedItems = new SparseBooleanArray();

	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;

	Object fragment;
	int type = CAMERA_UPLOAD_ADAPTER;

	DatabaseHandler dbH;
	MegaPreferences prefs;
	String downloadLocationDefaultPath;
	String defaultPath;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			List<MegaNode> documents = getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						//NodeController nC = new NodeController(context);
						//nC.exportLink(documents.get(0));
						if(documents.get(0)==null){
							logWarning("The selected node is NULL");
							break;
						}
						((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					}
					break;
				}
				case R.id.cab_menu_share_link_remove:{

					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						//NodeController nC = new NodeController(context);
						//nC.removeLink(documents.get(0));
						if(documents.get(0)==null){
							logWarning("The selected node is NULL");
							break;
						}
						((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));

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
					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logDebug("onDestroyActionMode");
			multipleSelect = false;
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			List<MegaNode> selected = getSelectedDocuments();
			
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean showRemoveLink = false;


			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
				if(selected.get(0).isExported()){
					//Node has public link
					showRemoveLink=true;
					showLink=false;

				}
				else{
					showRemoveLink=false;
					showLink=true;
				}			}

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

			if(showCopy){
				menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if(showDownload){
				menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if(showLink){
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			if(showRemoveLink){
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if(showMove){
				if(selected.size()==1){
					menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}else{
					menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
		
	}	
	
	/*public static view holder class*/
    public class ViewHolderPhotoSyncGrid extends RecyclerView.ViewHolder {
    	
    	public ViewHolderPhotoSyncGrid(View v){
    		super(v);
    	}
    	
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsComplete;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
		public ArrayList<RelativeLayout> relativeLayoutsVideoInfo;
		public ArrayList<LinearLayout> relativeLayoutsGradientVideo;
    	public ArrayList<ImageView> imageViews;
    	public ArrayList<ImageView> videoIcons;
		public ArrayList<TextView> videoDuration;
    	public TextView textView;
    	public RelativeLayout textRelativeLayout;
    	public ArrayList<LinearLayout> longClickLayoutsSelected;
    	public ArrayList<LinearLayout> longClickLayoutsUnselected;
    	
    	public ArrayList<Long> documents;
    }
	
    public MegaPhotoSyncGridAdapterLollipop(Context _context, ArrayList<MegaMonthPicLollipop> _monthPics, long _photosyncHandle, RecyclerView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, int numberOfCells, int gridWidth, Object fragment, int type) {
    	this.context = _context;
		this.monthPics = _monthPics;
		this.photosyncHandle = _photosyncHandle;
		this.nodes = _nodes;
		
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		this.fragment = fragment;
		this.type = type;
		this.numberOfCells = numberOfCells;
		this.gridWidth = gridWidth;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		this.app = ((MegaApplication) ((Activity) context).getApplication());
	}
    
    public void setNumberOfCells(int numberOfCells, int gridWidth){
    	this.numberOfCells = numberOfCells;
    	this.gridWidth = gridWidth;
    }
	
	public void setNodes(ArrayList<MegaMonthPicLollipop> monthPics, ArrayList<MegaNode> nodes){
		this.monthPics = monthPics;
		this.nodes = nodes;
		notifyDataSetChanged();
	}
	
	public void setPhotoSyncHandle(long photoSyncHandle){
		this.photosyncHandle = photoSyncHandle;
		notifyDataSetChanged();
	}

	public Object getItem(int position) {
        return monthPics.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
		
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect){
		this.multipleSelect = multipleSelect;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public long getPhotoSyncHandle(){
		return photosyncHandle;
	}
	
	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		this.multipleSelect = false;

		clearSelections();

		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void selectAll(){

		this.multipleSelect = true;
		if (nodes != null){
			for ( int i=0; i< nodes.size(); i++ ) {
				checkedItems.append(i, true);
			}
		}

		actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	public void clearSelections() {
		logDebug("clearSelections");
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				checkedItems.append(checkedPosition, false);
			}
		}
		this.multipleSelect = false;
		updateActionModeTitle();
		notifyDataSetChanged();
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

	@Override
	public int getItemCount() {
		return monthPics.size();
	}
	
	public void onNodeClick(ViewHolderPhotoSyncGrid holder, int position, int index, int positionInNodes){
		if (!multipleSelect){
			MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
			if (n != null){
				if (!n.isFolder()){
					ImageView imageView = holder.imageViews.get(index);
					int[] positionIV = new int[2];
					imageView.getLocationOnScreen(positionIV);
					int[] screenPosition = new int[4];
					screenPosition[0] = positionIV[0];
					screenPosition[1] = positionIV[1];
					screenPosition[2] = imageView.getWidth();
					screenPosition[3] = imageView.getHeight();

					if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){
						
						Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
						intent.putExtra("position", positionInNodes);
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						intent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);
						intent.putExtra("orderGetChildren", orderGetChildren);

						logDebug("Position in nodes: " + positionInNodes);
						if (megaApi.getParentNode(nodes.get(positionInNodes)).getType() == MegaNode.TYPE_ROOT){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(positionInNodes)).getHandle());
						}
						intent.putExtra("screenPosition", screenPosition);
						context.startActivity(intent);
						((ManagerActivityLollipop) context).overridePendingTransition(0,0);
						CameraUploadFragmentLollipop.imageDrag = imageView;
					}
					else if (MimeTypeThumbnail.typeForName(n.getName()).isVideoReproducible()){
						MegaNode file = n;

						String mimeType = MimeTypeThumbnail.typeForName(file.getName()).getType();
						logDebug("File Handle: " + file.getHandle());
				  		
				  		//Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
						Intent mediaIntent;
						if (MimeTypeThumbnail.typeForName(n.getName()).isVideoNotSupported()){
							mediaIntent = new Intent(Intent.ACTION_VIEW);
						}
						else {
							mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
						}
						mediaIntent.putExtra("position", positionInNodes);
						if (megaApi.getParentNode(nodes.get(positionInNodes)).getType() == MegaNode.TYPE_ROOT){
							mediaIntent.putExtra("parentNodeHandle", -1L);
						}
						else{
							mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(positionInNodes)).getHandle());
						}
						mediaIntent.putExtra("orderGetChildren", orderGetChildren);
						mediaIntent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);

						mediaIntent.putExtra("HANDLE", file.getHandle());
						mediaIntent.putExtra("FILENAME", file.getName());

						String localPath = getLocalFile(context, file.getName(), file.getSize());

						if (localPath != null){
							File mediaFile = new File(localPath);
							//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
								mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeThumbnail.typeForName(file.getName()).getType());
							}
							else{
								mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeThumbnail.typeForName(file.getName()).getType());
							}
							mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						}
						else {
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

							String url = megaApi.httpServerGetLocalLink(file);
							mediaIntent.setDataAndType(Uri.parse(url), mimeType);
						}
				  		if (isIntentAvailable(context, mediaIntent)){
				  			context.startActivity(mediaIntent);
				  		}
				  		else{
				  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
				  			ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							NodeController nC = new NodeController(context);
							nC.prepareForDownload(handleList, true);
				  		}						
					}
					else{
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(n.getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList, true);
					}	
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
			List<MegaNode> selectedNodes = getSelectedDocuments();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				notifyDataSetChanged();
			}
			else{
				hideMultipleSelect();
			}
		}
	}
	
	public void onNodeLongClick(ViewHolderPhotoSyncGrid holder, int position, int index, int positionInNodes){
		logDebug("position: " + position + ", index: " + index + ", positionInNodes: " + positionInNodes);
		if (!multipleSelect){
			clearSelections();

			this.multipleSelect = true;
			checkedItems.append(positionInNodes, true);

			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

			updateActionModeTitle();
			notifyDataSetChanged();
		}
		else{
    		onNodeClick(holder, position, index, positionInNodes);
    	}
	}
	
	private void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
		if (actionMode == null){
			logWarning("actionMode null");
			return;
		}
		
		if (context == null){
			logWarning("context null");
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
		String title;
		int sum=files;
		title = Integer.toString(sum);
//		if (files == 0 && folders == 0) {
//			title = Integer.toString(sum);
//		} else if (files == 0) {
//			title = Integer.toString(folders);
//		} else if (folders == 0) {
//			title = Integer.toString(files);
//		} else {
//			title = Integer.toString(sum);
//		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			logError("Invalidate error", e);
		}
		// actionMode.
	}
	
	/*
	 * Get list of all selected documents
	 */
	public List<MegaNode> getSelectedDocuments() {
		logDebug("getSelectedDocuments");
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

	@Override
	public void onBindViewHolder(ViewHolderPhotoSyncGrid holder, int position) {
		logDebug("Position: " + position);
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);
		
		MegaMonthPicLollipop monthPic = (MegaMonthPicLollipop) getItem(position);
		
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
				logDebug("monthPic.monthYearString != null and not empty string");
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
						holder.documents.set(i, n.getHandle());
						
						Bitmap thumb = null;						
						holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());	
						if (n.hasThumbnail()){
							thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{
								thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
								if (thumb != null){
									holder.imageViews.get(i).setImageBitmap(thumb);
								}
								else{
									try{
										thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaPhotoSyncGrid(n, context, holder, megaApi, this, i);
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
						else{
							logDebug(n.getHandle()+" NO ThUMB!!");
						}

						if(isVideoFile(n.getName())){
							logDebug("IS VIDEO!");
							holder.relativeLayoutsVideoInfo.get(i).setVisibility(View.VISIBLE);
							holder.relativeLayoutsGradientVideo.get(i).setVisibility(View.VISIBLE);
							holder.videoIcons.get(i).setVisibility(View.VISIBLE);

							if(((CameraUploadFragmentLollipop) fragment).getIsLargeGrid()){
								holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_24dp);
								logDebug(n.getHandle() + " DURATION: " + n.getDuration());
								int duration = n.getDuration();
								if(duration>0){
									holder.videoDuration.get(i).setText(getVideoDuration(duration));

									RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams)holder.relativeLayoutsVideoInfo.get(i).getLayoutParams();
									relativeParams.bottomMargin=scaleWidthPx(3, outMetrics);
									relativeParams.leftMargin=scaleWidthPx(3, outMetrics);
									holder.relativeLayoutsVideoInfo.get(i).setLayoutParams(relativeParams);
									holder.videoDuration.get(i).setVisibility(View.VISIBLE);
								}
								else{
									holder.videoDuration.get(i).setVisibility(View.GONE);
								}
							}
							else{
								holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_18dp);
								holder.videoIcons.get(i).setVisibility(View.VISIBLE);
								RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams)holder.relativeLayoutsVideoInfo.get(i).getLayoutParams();
								relativeParams.bottomMargin=scaleWidthPx(1, outMetrics);
								relativeParams.leftMargin=scaleWidthPx(1, outMetrics);
								holder.relativeLayoutsVideoInfo.get(i).setLayoutParams(relativeParams);
								holder.videoDuration.get(i).setVisibility(View.GONE);
							}
						}
						else{
							holder.relativeLayoutsGradientVideo.get(i).setVisibility(View.GONE);
							holder.videoIcons.get(i).setVisibility(View.GONE);
							holder.videoDuration.get(i).setVisibility(View.GONE);
						}
					}
					else{
						holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
						holder.imageViews.get(i).setVisibility(View.GONE);
						holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
						holder.documents.set(i,  -1l);
					}
				}
			}
		}
		else{
			holder.textRelativeLayout.setVisibility(View.GONE);
			for (int i=0;i<numberOfCells;i++){
				holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
				holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
				
				if (monthPic.nodeHandles.size() > i){
					MegaNode n = megaApi.getNodeByHandle(monthPic.nodeHandles.get(i));
					if (n == null){
						Intent intent = new Intent(context, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  TOUR_FRAGMENT);
				        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				        context.startActivity(intent);
				        if (context instanceof Activity){
				        	((Activity)context).finish();
				        }
				        return;
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
					holder.documents.set(i, n.getHandle());
					
					Bitmap thumb = null;					
					holder.imageViews.get(i).setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
					if (n.hasThumbnail()){
						thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
						if (thumb != null){
							holder.imageViews.get(i).setImageBitmap(thumb);
						}
						else{
							thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
							if (thumb != null){
								holder.imageViews.get(i).setImageBitmap(thumb);
							}
							else{ 
								try{
									thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaPhotoSyncGrid(n, context, holder, megaApi, this, i);
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
					else{
						logDebug(n.getHandle()+" NO ThUMB!!");
					}

					if(isVideoFile(n.getName())){
						holder.relativeLayoutsVideoInfo.get(i).setVisibility(View.VISIBLE);
						holder.relativeLayoutsGradientVideo.get(i).setVisibility(View.VISIBLE);
						holder.videoIcons.get(i).setVisibility(View.VISIBLE);

						if(((CameraUploadFragmentLollipop) fragment).getIsLargeGrid()){						
							holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_24dp);
							logDebug(n.getHandle() + " DURATION: " + n.getDuration());
							int duration = n.getDuration();
							if(duration>0){
								holder.videoDuration.get(i).setText(getVideoDuration(duration));

								RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams)holder.relativeLayoutsVideoInfo.get(i).getLayoutParams();
								relativeParams.bottomMargin=scaleWidthPx(3, outMetrics);
								relativeParams.leftMargin=scaleWidthPx(3, outMetrics);
								holder.relativeLayoutsVideoInfo.get(i).setLayoutParams(relativeParams);
								holder.videoDuration.get(i).setVisibility(View.VISIBLE);
							}
							else{
								holder.videoDuration.get(i).setVisibility(View.GONE);
							}
						}
						else{
							holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_18dp);	
							RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams)holder.relativeLayoutsVideoInfo.get(i).getLayoutParams();
							relativeParams.bottomMargin=scaleWidthPx(1, outMetrics);
							relativeParams.leftMargin=scaleWidthPx(1, outMetrics);
							holder.relativeLayoutsVideoInfo.get(i).setLayoutParams(relativeParams);
							holder.videoDuration.get(i).setVisibility(View.GONE);
						}
					}
					else{
						holder.relativeLayoutsGradientVideo.get(i).setVisibility(View.GONE);
						holder.videoIcons.get(i).setVisibility(View.GONE);
						holder.videoDuration.get(i).setVisibility(View.GONE);
					}
				}
				else{
					holder.relativeLayoutsComplete.get(i).setVisibility(View.VISIBLE);
					holder.imageViews.get(i).setVisibility(View.GONE);
					holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
					holder.documents.set(i,  -1l);
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
	}

	@Override
	public ViewHolderPhotoSyncGrid onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		downloadLocationDefaultPath = getDownloadLocation(context);
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);
		
		float dpHeight = outMetrics.heightPixels / density;
		float dpWidth  = outMetrics.widthPixels / density;
		
//		Toast.makeText(context, "W: " + dpWidth + "__H: " + dpHeight, Toast.LENGTH_SHORT).show();
//		Toast.makeText(context, "Wpx: " + outMetrics.widthPixels + "__H: " + outMetrics.heightPixels, Toast.LENGTH_SHORT).show();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View v = inflater.inflate(R.layout.item_photo_sync_grid, parent, false);
		holder = new ViewHolderPhotoSyncGrid(v);
		holder.relativeLayoutsEmpty = new ArrayList<RelativeLayout>();
		holder.relativeLayoutsComplete = new ArrayList<RelativeLayout>();
		holder.imageViews = new ArrayList<ImageView>();
		holder.longClickLayoutsSelected = new ArrayList<LinearLayout>();
		holder.longClickLayoutsUnselected = new ArrayList<LinearLayout>();
		holder.relativeLayoutsVideoInfo = new ArrayList<RelativeLayout>();
		holder.relativeLayoutsGradientVideo = new ArrayList<LinearLayout>();
		holder.videoIcons = new ArrayList<ImageView>();
		holder.videoDuration = new ArrayList<TextView>();
		holder.documents = new ArrayList<Long>();
		
		holder.cellLayout = (LinearLayout) v.findViewById(R.id.cell_photosync_layout);
		for (int i=0;i<numberOfCells;i++){
			View rLView = inflater.inflate(R.layout.cell_photosync_grid_fill, holder.cellLayout, false);
			
			RelativeLayout rL = (RelativeLayout) rLView.findViewById(R.id.cell_photosync_grid_item_complete_layout);
//			rL.setLayoutParams(new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
			rL.setLayoutParams(new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, gridWidth, 1f));
			holder.cellLayout.addView(rL);
			holder.relativeLayoutsComplete.add(rL);
			
			RelativeLayout rLE = (RelativeLayout) rLView.findViewById(R.id.cell_photosync_item_layout_empty);
			holder.relativeLayoutsEmpty.add(rLE);
			
			ImageView iV = (ImageView) rLView.findViewById(R.id.cell_photosync_grid_thumbnail);
			iV.setLayoutParams(new RelativeLayout.LayoutParams(gridWidth, gridWidth));
			if(numberOfCells == CameraUploadFragmentLollipop.GRID_LARGE){
				logDebug("numOfCells is GRID_LARGE");

				int padding = PADDING_GRID_LARGE;
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)  iV.getLayoutParams();
				layoutParams.setMargins(padding, padding, padding, padding);
				iV.requestLayout();
//				iV.setPadding(padding, padding, padding, padding);
			}
			else if (numberOfCells == CameraUploadFragmentLollipop.GRID_SMALL){
				logDebug("numOfCells is GRID_SMALL");
				int padding = PADDING_GRID_SMALL;
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)  iV.getLayoutParams();
				layoutParams.setMargins(padding, padding, padding, padding);
				iV.requestLayout();
//				iV.setPadding(padding, padding, padding, padding);
			}
			else{
				logDebug("numOfCells is " + numberOfCells);
				int padding = 2;
				ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)  iV.getLayoutParams();
				layoutParams.setMargins(padding, padding, padding, padding);
				iV.requestLayout();
//				iV.setPadding(padding, padding, padding, padding);
			}
			holder.imageViews.add(iV);
			
			LinearLayout lcLS = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_selected);
			lcLS.setLayoutParams(new RelativeLayout.LayoutParams(gridWidth, gridWidth));
			holder.longClickLayoutsSelected.add(lcLS);
			
			LinearLayout lcLU = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_unselected);
			lcLU.setLayoutParams(new RelativeLayout.LayoutParams(gridWidth, gridWidth));
			holder.longClickLayoutsUnselected.add(lcLU);

			RelativeLayout rLVdI = (RelativeLayout) rLView.findViewById(R.id.cell_photosync_grid_video_info_layout);
			holder.relativeLayoutsVideoInfo.add(rLVdI);

			LinearLayout rLVgra = (LinearLayout) rLView.findViewById(R.id.cell_photosync_gradient_effect);
			holder.relativeLayoutsGradientVideo.add(rLVgra);
			
			ImageView vI = (ImageView) rLView.findViewById(R.id.cell_photosync_grid_video_icon);
			holder.videoIcons.add(vI);

			TextView vD = (TextView) rLView.findViewById(R.id.cell_photosync_grid_video_duration);
			holder.videoDuration.add(vD);

			holder.documents.add(-1l);
		}
		
		holder.textRelativeLayout = (RelativeLayout) v.findViewById(R.id.cell_photosync_grid_month_layout);
		
		holder.textView = (TextView) v.findViewById(R.id.cell_photosync_grid_month_name);
//		//Margins
//		RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)holder.textView.getLayoutParams();
//		contentTextParams.setMargins(scaleWidthPx(63, outMetrics), scaleHeightPx(5, outMetrics), 0, scaleHeightPx(5, outMetrics));
//		holder.textView.setLayoutParams(contentTextParams);
		
		v.setTag(holder);
		
		return holder;
	}
}
