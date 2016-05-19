package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class MegaPhotoSyncGridAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncGridAdapterLollipop.ViewHolderPhotoSyncGrid> {
	
	private class Media {
		public String filePath;
		public long timestamp;
	}
	
	ViewHolderPhotoSyncGrid holder = null;
	
	static int FROM_FILE_BROWSER = 13;
	static int FROM_INCOMING_SHARES= 14;
	static int FROM_OFFLINE= 15;
	
	Context context;
	MegaApplication app;
	MegaApiAndroid megaApi;

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
	int type = ManagerActivityLollipop.CAMERA_UPLOAD_ADAPTER;
	
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
					((ManagerActivityLollipop) context).onFileClick(handleList);
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
					nC.copyNodes(handleList);
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
					nC.moveNodes(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivityLollipop) context).getPublicLinkAndShareIt(documents.get(0));
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
					((ManagerActivityLollipop) context).moveToTrash(handleList);
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
	
	/*public static view holder class*/
    public class ViewHolderPhotoSyncGrid extends RecyclerView.ViewHolder {
    	
    	public ViewHolderPhotoSyncGrid(View v){
    		super(v);
    	}
    	
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsComplete;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
    	public ArrayList<ImageView> imageViews;
    	public ArrayList<ImageView> videoIcons;
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
	void hideMultipleSelect() {
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
		if (actionMode == null){
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}

		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	public void clearSelections() {
		log("clearSelections");
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
	
	private static void log(String log) {
		Util.log("MegaPhotoSyncGridAdapterLollipop", log);
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
					if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){
						
						Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
						intent.putExtra("position", positionInNodes);
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						intent.putExtra("adapterType", ManagerActivityLollipop.PHOTO_SYNC_ADAPTER);
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
				  		if (ManagerActivityLollipop.isIntentAvailable(context, mediaIntent)){
				  			context.startActivity(mediaIntent);
				  		}
				  		else{
				  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
				  			ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivityLollipop) context).onFileClick(handleList);
				  		}						
					}
					else{
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(n.getHandle());
						((ManagerActivityLollipop) context).onFileClick(handleList);
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
		log("onNodeLongClick");
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
	public List<MegaNode> getSelectedDocuments() {
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

	@Override
	public void onBindViewHolder(ViewHolderPhotoSyncGrid holder, int position) {
		log("onBindViewHolder");
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
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

						MegaNode nodeToCheck = megaApi.getNodeByHandle(holder.documents.get(i));
						if(Util.isVideoFile(nodeToCheck.getName())){
							holder.videoIcons.get(i).setVisibility(View.VISIBLE);
							if(((CameraUploadFragmentLollipop) fragment).getIsLargeGrid()){						
								holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_24dp);	
								RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
								lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								lp.setMargins(3, 0, 0, 3);								
								holder.videoIcons.get(i).setLayoutParams(lp);
							}
							else{
								holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_18dp);	
								RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
								lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								lp.setMargins(1, 0, 0, 1);								
								holder.videoIcons.get(i).setLayoutParams(lp);
							}							
						}
						else{
							holder.videoIcons.get(i).setVisibility(View.GONE);
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
						Intent intent = new Intent(context, TourActivityLollipop.class);
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

					MegaNode nodeToCheck = megaApi.getNodeByHandle(holder.documents.get(i));
					if(Util.isVideoFile(nodeToCheck.getName())){
						if(((CameraUploadFragmentLollipop) fragment).getIsLargeGrid()){						
							holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_24dp);	
							RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
							lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							lp.setMargins(3, 0, 0, 3);								
							holder.videoIcons.get(i).setLayoutParams(lp);
						}
						else{
							holder.videoIcons.get(i).setImageResource(R.drawable.ic_play_arrow_white_18dp);	
							RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
							lp.setMargins(1, 0, 0, 1);
							lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							holder.videoIcons.get(i).setLayoutParams(lp);
						}
						holder.videoIcons.get(i).setVisibility(View.VISIBLE);
					}
					else{
						holder.videoIcons.get(i).setVisibility(View.GONE);
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
		log("onCreateViewHolder");
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
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
		holder.videoIcons = new ArrayList<ImageView>();
		
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
			iV.setPadding(2, 2, 2, 2);
			holder.imageViews.add(iV);
			
			LinearLayout lcLS = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_selected);
			lcLS.setLayoutParams(new RelativeLayout.LayoutParams(gridWidth, gridWidth));
			holder.longClickLayoutsSelected.add(lcLS);
			
			LinearLayout lcLU = (LinearLayout) rLView.findViewById(R.id.cell_photosync_menu_long_click_unselected);
			lcLU.setLayoutParams(new RelativeLayout.LayoutParams(gridWidth, gridWidth));
			holder.longClickLayoutsUnselected.add(lcLU);
			
			ImageView vI = (ImageView) rLView.findViewById(R.id.cell_photosync_grid_video_icon);
			holder.videoIcons.add(vI);					
			
			holder.documents.add(-1l);
		}
		
		holder.textRelativeLayout = (RelativeLayout) v.findViewById(R.id.cell_photosync_grid_month_layout);
		
		holder.textView = (TextView) v.findViewById(R.id.cell_photosync_grid_month_name);
		
		v.setTag(holder);
		
		return holder;
	}
}
