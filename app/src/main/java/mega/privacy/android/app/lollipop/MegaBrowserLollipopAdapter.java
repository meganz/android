package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
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
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

public class MegaBrowserLollipopAdapter extends RecyclerView.Adapter<MegaBrowserLollipopAdapter.ViewHolderBrowser> implements OnClickListener {
	
	public static final int ITEM_VIEW_TYPE_LIST = 0;
	public static final int ITEM_VIEW_TYPE_GRID = 1;
	
	static int FROM_FILE_BROWSER = 13;
	static int FROM_INCOMING_SHARES= 14;
	static int FROM_OFFLINE= 15;
		
	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	ArrayList<MegaNode> nodes;

	Object fragment;
	long parentHandle = -1;
	
	private SparseBooleanArray selectedItems;

	RecyclerView listFragment;
//	ImageView emptyImageViewFragment;
//	TextView emptyTextViewFragment;
	ActionBar aB;
	HashMap<Long, MegaTransfer> mTHash = null;
	MegaTransfer currentTransfer = null;
	boolean incoming = false;
	DatabaseHandler dbH = null;
	boolean multipleSelect;
	int type = ManagerActivityLollipop.FILE_BROWSER_ADAPTER;
	int adapterType;

//	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	/* public static view holder class */
	public class ViewHolderBrowser extends RecyclerView.ViewHolder{

		public ViewHolderBrowser(View v) {
			super(v);
		}
		
		public ImageView imageView;
		public ImageView savedOffline;
		public ImageView publicLinkImage;
		public TextView textViewFileName;
		public TextView textViewFileSize;
		public int currentPosition;
		public long document;	
		public RelativeLayout itemLayout;
		public ImageButton imageButtonThreeDots;
		public ProgressBar transferProgressBar;
	}
	
	public class ViewHolderBrowserList extends ViewHolderBrowser{
		
		public ViewHolderBrowserList(View v){
			super(v);
		}			

		public ImageView publicLinkImageMultiselect;
		public RelativeLayout itemLayout;
	}
	
	public class ViewHolderBrowserGrid extends ViewHolderBrowser{
		
		public ViewHolderBrowserGrid(View v){
			super(v);
		}
		
		public View separator;
		
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
	
	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

	public int getSelectedItemCount() {
		return selectedItems.size();
	}

	public List<Integer> getSelectedItems() {
		List<Integer> items = new ArrayList<Integer>(selectedItems.size());
		for (int i = 0; i < selectedItems.size(); i++) {
			items.add(selectedItems.keyAt(i));
		}
		return items;
	}	
	
	/*
	 * Get list of all selected nodes
	 */
	public List<MegaNode> getSelectedNodes() {
		ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaNode document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					nodes.add(document);
				}
			}
		}
		return nodes;
	}

	public MegaBrowserLollipopAdapter(Context _context, Object fragment, ArrayList<MegaNode> _nodes, long _parentHandle, RecyclerView recyclerView, ActionBar aB, int type, int adapterType) {
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.type = type;
		this.adapterType = adapterType;
		this.fragment = fragment;
		
		dbH = DatabaseHandler.getDbHandler(context);
		
		switch (type) {
			case ManagerActivityLollipop.FILE_BROWSER_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleBrowser(parentHandle);			
				break;
			}
			case ManagerActivityLollipop.CONTACT_FILE_ADAPTER: {
				((ContactPropertiesActivityLollipop) context).setParentHandle(parentHandle);
				break;
			}
			case ManagerActivityLollipop.RUBBISH_BIN_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleRubbish(parentHandle);
				break;
			}		
			case ManagerActivityLollipop.FOLDER_LINK_ADAPTER: {
				megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
				break;
			}
			case ManagerActivityLollipop.SEARCH_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleSearch(parentHandle);
				break;
			}
			case ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER: {
//				((ManagerActivityLollipop) context).setParentHandleOutgoing(parentHandle);
				break;
			}
			case ManagerActivityLollipop.INCOMING_SHARES_ADAPTER: {
				incoming=true;
				((ManagerActivityLollipop) context).setParentHandleIncoming(parentHandle);
				break;
			}
			default: {
	//			((ManagerActivityLollipop) context).setParentHandleCloud(parentHandle);
				break;
			}
		}

		this.listFragment = recyclerView;
//		this.emptyImageViewFragment = emptyImageView;
//		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		this.positionClicked = -1;
		this.type = type;		

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication())
					.getMegaApi();
		}
	}

	public void setNodes(ArrayList<MegaNode> nodes) {
		log("setNodes");
		this.nodes = nodes;
//		contentTextFragment.setText(getInfoFolder(node));
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void setAdapterType(int adapterType){
		this.adapterType = adapterType;
	}
	
	public class MyGlobalLayoutListener implements OnGlobalLayoutListener{
		
		ViewHolderBrowserList holder;

		public MyGlobalLayoutListener(ViewHolderBrowserList holder) {
			super();
			this.holder = holder;
		}

		@Override
		public void onGlobalLayout() {
			log("onGlobalLayout");
			if (holder.textViewFileName.getLineCount() > 1) {
	            log("----------------FILA MAYOR");
	        }
			else{
				log("----------------FILA 1----------------");
			}
		}
		
	}
	
	@Override
	public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");
		
//		Toast.makeText(context, "VIEWTYPE: " + viewType, Toast.LENGTH_SHORT).show();
		// set the view's size, margins, paddings and layout parameters

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		if (viewType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
		
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
		
			ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
			holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.file_list_item_layout);
			holderList.imageView = (ImageView) v.findViewById(R.id.file_list_thumbnail);
			holderList.savedOffline = (ImageView) v.findViewById(R.id.file_list_saved_offline);
			
			holderList.publicLinkImageMultiselect = (ImageView) v.findViewById(R.id.file_list_public_link_multiselect);
			holderList.publicLinkImage = (ImageView) v.findViewById(R.id.file_list_public_link);
			
			holderList.textViewFileName = (TextView) v.findViewById(R.id.file_list_filename);			
			holderList.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holderList.textViewFileName.getLayoutParams().width = Util.scaleWidthPx(210, outMetrics);
			
//			holderList.textViewFileName.getViewTreeObserver().addOnGlobalLayoutListener(new MyGlobalLayoutListener(holderList));		
			
			holderList.textViewFileSize = (TextView) v.findViewById(R.id.file_list_filesize);
			holderList.transferProgressBar = (ProgressBar) v.findViewById(R.id.transfers_list__browser_bar);
			
			holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.file_list_three_dots);
			
			//Right margin
			RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holderList.imageButtonThreeDots.getLayoutParams();
			actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0); 
			holderList.imageButtonThreeDots.setLayoutParams(actionButtonParams);			
		
			v.setTag(holderList);
		
			holderList.savedOffline.setVisibility(View.INVISIBLE);
		
			holderList.publicLinkImage.setVisibility(View.GONE);
			holderList.publicLinkImageMultiselect.setVisibility(View.GONE);
			
			holderList.transferProgressBar.setVisibility(View.GONE);
			holderList.textViewFileSize.setVisibility(View.VISIBLE);
			
			holderList.itemLayout.setTag(holderList);
			holderList.itemLayout.setOnClickListener(this);
			
			holderList.imageButtonThreeDots.setTag(holderList);
			holderList.imageButtonThreeDots.setOnClickListener(this);
			
			return holderList;
		}
		else if (viewType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
			ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);
			
			holderGrid.itemLayout = (RelativeLayout) v.findViewById(R.id.file_grid_item_layout);
			holderGrid.imageView = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
			holderGrid.textViewFileName = (TextView) v.findViewById(R.id.file_grid_filename);
			holderGrid.textViewFileSize = (TextView) v.findViewById(R.id.file_grid_filesize);
			holderGrid.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.file_grid_three_dots);
			holderGrid.transferProgressBar = (ProgressBar) v.findViewById(R.id.transfers_grid_browser_bar);
			holderGrid.savedOffline = (ImageView) v.findViewById(R.id.file_grid_saved_offline);
			holderGrid.separator = (View) v.findViewById(R.id.file_grid_separator);
			holderGrid.publicLinkImage = (ImageView) v.findViewById(R.id.file_grid_public_link);
			holderGrid.transferProgressBar.setVisibility(View.GONE);
			holderGrid.textViewFileSize.setVisibility(View.VISIBLE);
			
			v.setTag(holderGrid);
			
			holderGrid.savedOffline.setVisibility(View.INVISIBLE);
			holderGrid.publicLinkImage.setVisibility(View.GONE);
			
			holderGrid.itemLayout.setTag(holderGrid);
			holderGrid.itemLayout.setOnClickListener(this);
			
			holderGrid.imageButtonThreeDots.setTag(holderGrid);
			holderGrid.imageButtonThreeDots.setOnClickListener(this);
			
			return holderGrid;
		}
		else{
			return null;
		}
	}
	
	@Override
	public void onBindViewHolder(ViewHolderBrowser holder, int position) {
		log("onBindViewHolder");
		
		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			ViewHolderBrowserList holderList = (ViewHolderBrowserList) holder;
			onBindViewHolderList(holderList, position);
		}
		else if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid) holder;
			onBindViewHolderGrid(holderGrid, position);
		}
	}
	
	public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position){
		log("onBindViewHolderGrid");
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		holder.currentPosition = position;
	
		MegaNode node = (MegaNode) getItem(position);
		if (node == null){
			return;
		}
		
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		log("Node : "+position+" "+node.getName());
		
		holder.textViewFileName.setText(node.getName());
		holder.textViewFileSize.setText("");
		
		if (!multipleSelect) {
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
			
			if (positionClicked != -1) {
				if (positionClicked == position) {
					//				holder.arrowSelection.setVisibility(View.VISIBLE);
//					holder.optionsLayout.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
					holder.separator.setBackgroundColor(context.getResources().getColor(R.color.grid_item_separator));
//					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
					listFragment.smoothScrollToPosition(positionClicked);
				}
				else {
					//				holder.arrowSelection.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
					holder.separator.setBackgroundColor(context.getResources().getColor(R.color.grid_item_separator));
//					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				}
			} 
			else {
				//			holder.arrowSelection.setVisibility(View.GONE);
				
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
				holder.separator.setBackgroundColor(context.getResources().getColor(R.color.grid_item_separator));
//				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
	
		} 
		else {
			holder.imageButtonThreeDots.setVisibility(View.GONE);		

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid_long_click_lollipop));
				holder.separator.setBackgroundColor(Color.WHITE);
			}
			else{
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
				holder.separator.setBackgroundColor(context.getResources().getColor(R.color.grid_item_separator));
			}
		}
		
		if(node.isExported()){
			//Node has public link			
			holder.publicLinkImage.setVisibility(View.VISIBLE);
		}
		else{
			holder.publicLinkImage.setVisibility(View.GONE);
		}
		
		if (node.isFolder()) {
			holder.transferProgressBar.setVisibility(View.GONE);		
			holder.textViewFileSize.setVisibility(View.VISIBLE);			
			holder.textViewFileSize.setText(getInfoFolder(node));
			
			if(type==ManagerActivityLollipop.INCOMING_SHARES_ADAPTER){
				holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				//Show the owner of the shared folder
				ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
				for(int j=0; j<sharesIncoming.size();j++){
					MegaShare mS = sharesIncoming.get(j);
					if(mS.getNodeHandle()==node.getHandle()){						
						MegaUser user= megaApi.getContact(mS.getUser());
						if(user!=null){
							MegaContact contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
							if(contactDB!=null){
								if(!contactDB.getName().equals("")){
									holder.textViewFileSize.setText(contactDB.getName()+" "+contactDB.getLastName());
								}
								else{
									holder.textViewFileSize.setText(user.getEmail());
								}
							}
							else{
								log("The contactDB is null: ");
								holder.textViewFileSize.setText(user.getEmail());
							}		
						}
						else{
							holder.textViewFileSize.setText(mS.getUser());
						}						
					}				
				}
			}
			else if (type==ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER){
				holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				//Show the number of contacts who shared the folder
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
				if (sl != null) {
					if(sl.size()!=0){
						holder.textViewFileSize.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact)+" "+sl.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
					}
				}
			}
			else{
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
				if (sl != null) {
					if (sl.size() > 0) {					
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					} else {
						holder.imageView.setImageResource(R.drawable.ic_folder_list);
					}
				} 
				else {
					holder.imageView.setImageResource(R.drawable.ic_folder_list);
				}				
			}			
		}
		else {
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));	

			if(mTHash!=null){

				log("NODE: " + mTHash.get(node.getHandle()));
				MegaTransfer tempT = mTHash.get(node.getHandle());

				if (tempT != null){
					holder.transferProgressBar.setVisibility(View.VISIBLE);		
					holder.textViewFileSize.setVisibility(View.GONE);	

					double progressValue = 100.0 * tempT.getTransferredBytes() / tempT.getTotalBytes();
					holder.transferProgressBar.setProgress((int)progressValue);
				}

				if (currentTransfer != null){
					if (node.getHandle() == currentTransfer.getNodeHandle()){
						holder.transferProgressBar.setVisibility(View.VISIBLE);		
						holder.textViewFileSize.setVisibility(View.GONE);	
						double progressValue = 100.0 * currentTransfer.getTransferredBytes() / currentTransfer.getTotalBytes();
						holder.transferProgressBar.setProgress((int)progressValue);
						holder.transferProgressBar.setVisibility(View.VISIBLE);		
						holder.textViewFileSize.setVisibility(View.GONE);
					}
					else{
						holder.transferProgressBar.setVisibility(View.GONE);		
						holder.textViewFileSize.setVisibility(View.VISIBLE);
					}
				}
				else{
					holder.transferProgressBar.setVisibility(View.GONE);		
					holder.textViewFileSize.setVisibility(View.VISIBLE);
				}

				if(mTHash.size() == 0){
					holder.transferProgressBar.setVisibility(View.GONE);		
					holder.textViewFileSize.setVisibility(View.VISIBLE);	
				}
			}					

			holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

			if (node.hasThumbnail()) {
			
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					if(!multipleSelect){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						holder.imageView.setImageBitmap(thumb);
					}
				} 
				else {
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						if(!multipleSelect){
							holder.imageView.setImageBitmap(thumb);
						}
						else{
							holder.imageView.setImageBitmap(thumb);
						}
					}
					else {
						try {
							thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks

						if (thumb != null) {
							if(!multipleSelect){
								holder.imageView.setImageBitmap(thumb);
							}
							else{
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
			} 
			else {
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					if(!multipleSelect){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						holder.imageView.setImageBitmap(thumb);
					}
				} 
				else {
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						if(!multipleSelect){
							holder.imageView.setImageBitmap(thumb);
						}
						else{
							holder.imageView.setImageBitmap(thumb);
						}
					} 
					else {
						try {
							ThumbnailUtilsLollipop.createThumbnailList(context, node,holder, megaApi, this);
						} 
						catch (Exception e) {
						} // Too many AsyncTasks
					}
				}
			}
		}
		
		//Check if is an offline file to show the red arrow
		File offlineFile = null;
		//Find in the database
		MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
		if(offlineNode!=null){
			if(incoming){
				log("Incoming tab: MegaBrowserGridAdapter: "+node.getHandle());				
				//Find in the filesystem
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + offlineNode.getHandleIncoming()+offlineNode.getPath()+offlineNode.getName());
					log("offline File: "+offlineFile.getAbsolutePath());
				}
				else{
					offlineFile = context.getFilesDir();
				}
				
			}
			else{
				//Find in the filesystem
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +megaApi.getNodePath(node)+offlineNode.getName());
				}
				else{
					offlineFile = context.getFilesDir();
				}
			}	
		}		
		
		if (offlineFile!=null){
			if (offlineFile.exists()){
				log("File EXISTS!!!");
				holder.savedOffline.setVisibility(View.VISIBLE);
			}
			else{
				log("File NOT exists!!!");
				holder.savedOffline.setVisibility(View.INVISIBLE);
			}
		}
		else{
			holder.savedOffline.setVisibility(View.INVISIBLE);
		}		
	}
	
	public void onBindViewHolderList(ViewHolderBrowserList holder, int position){
		log("onBindViewHolderList");
//		listFragment = (RecyclerView) parent;
//		final int _position = position;	
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		holder.currentPosition = position;
	
		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		log("Node to show: "+position+" "+node.getName());
		holder.textViewFileName.setText(node.getName());	
		
		if (!multipleSelect) {
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
			
			if (positionClicked != -1) {
				if (positionClicked == position) {
					//				holder.arrowSelection.setVisibility(View.VISIBLE);
//					holder.optionsLayout.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
//					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
					listFragment.smoothScrollToPosition(positionClicked);
				}
				else {
					//				holder.arrowSelection.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundColor(Color.WHITE);
//					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				}
			} 
			else {
				//			holder.arrowSelection.setVisibility(View.GONE);
				holder.itemLayout.setBackgroundColor(Color.WHITE);
//				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
	
		} 
		else {
			holder.imageButtonThreeDots.setVisibility(View.GONE);		

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}
		}
	
		holder.textViewFileSize.setText("");
		
		holder.publicLinkImageMultiselect.setVisibility(View.GONE);
		holder.publicLinkImage.setVisibility(View.GONE);
		
		if(node.isExported()){
			//Node has public link
			if(multipleSelect){
				holder.publicLinkImageMultiselect.setVisibility(View.VISIBLE);
				holder.publicLinkImage.setVisibility(View.GONE);
			}
			else
			{
				holder.publicLinkImageMultiselect.setVisibility(View.GONE);
				holder.publicLinkImage.setVisibility(View.VISIBLE);
			}
		}
		else{
			holder.publicLinkImageMultiselect.setVisibility(View.GONE);
			holder.publicLinkImage.setVisibility(View.GONE);
			holder.publicLinkImageMultiselect.setVisibility(View.GONE);
			holder.publicLinkImage.setVisibility(View.GONE);
		}
		
		if (node.isFolder()) {
			
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(0, 0, 0, 0);
			holder.imageView.setLayoutParams(params);
			
			holder.transferProgressBar.setVisibility(View.GONE);		
			holder.textViewFileSize.setVisibility(View.VISIBLE);
//			holder.propertiesText.setText(R.string.general_folder_info);
			holder.textViewFileSize.setText(getInfoFolder(node));
			
			if(type==ManagerActivityLollipop.INCOMING_SHARES_ADAPTER){
				holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				//Show the owner of the shared folder
				ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
				for(int j=0; j<sharesIncoming.size();j++){
					MegaShare mS = sharesIncoming.get(j);
					if(mS.getNodeHandle()==node.getHandle()){						
						MegaUser user= megaApi.getContact(mS.getUser());
						if(user!=null){
							MegaContact contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
							if(contactDB!=null){
								if(!contactDB.getName().equals("")){
									holder.textViewFileSize.setText(contactDB.getName()+" "+contactDB.getLastName());
								}
								else{
									holder.textViewFileSize.setText(user.getEmail());
								}
							}
							else{
								log("The contactDB is null: ");
								holder.textViewFileSize.setText(user.getEmail());
							}		
						}
						else{
							holder.textViewFileSize.setText(mS.getUser());
						}						
					}				
				}
			}
			else if (type==ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER){
				holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				//Show the number of contacts who shared the folder
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
				if (sl != null) {
					if(sl.size()!=0){
						holder.textViewFileSize.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact)+" "+sl.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
					}										
				}
			}
			else{
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
				if (sl != null) {
					if (sl.size() > 0) {					
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					} else {
						holder.imageView.setImageResource(R.drawable.ic_folder_list);
					}
				} 
				else {
					holder.imageView.setImageResource(R.drawable.ic_folder_list);
				}
			}			
		} 
		else {
			
//			holder.propertiesText.setText(R.string.general_file_info);
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));	

			if(mTHash!=null){

				log("NODE: " + mTHash.get(node.getHandle()));
				MegaTransfer tempT = mTHash.get(node.getHandle());

				if (tempT!=null){
					holder.transferProgressBar.setVisibility(View.VISIBLE);		
					holder.textViewFileSize.setVisibility(View.GONE);	

					double progressValue = 100.0 * tempT.getTransferredBytes() / tempT.getTotalBytes();
					holder.transferProgressBar.setProgress((int)progressValue);
				}

				if (currentTransfer != null){
					if (node.getHandle() == currentTransfer.getNodeHandle()){
						holder.transferProgressBar.setVisibility(View.VISIBLE);		
						holder.textViewFileSize.setVisibility(View.GONE);	
						double progressValue = 100.0 * currentTransfer.getTransferredBytes() / currentTransfer.getTotalBytes();
						holder.transferProgressBar.setProgress((int)progressValue);
						holder.transferProgressBar.setVisibility(View.VISIBLE);		
						holder.textViewFileSize.setVisibility(View.GONE);
					}
					else{
						holder.transferProgressBar.setVisibility(View.GONE);		
						holder.textViewFileSize.setVisibility(View.VISIBLE);
					}
				}
				else{
					holder.transferProgressBar.setVisibility(View.GONE);		
					holder.textViewFileSize.setVisibility(View.VISIBLE);
				}

				if(mTHash.size() == 0){
					holder.transferProgressBar.setVisibility(View.GONE);		
					holder.textViewFileSize.setVisibility(View.VISIBLE);	
				}
				
				log("mTHash.size()= "+mTHash.size());
			}					

			holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
			
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(0, 0, 0, 0);
			holder.imageView.setLayoutParams(params);
			
			log("Check the thumb");

			if (node.hasThumbnail()) {
				log("Node has thumbnail");
				RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
				params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.setMargins(20, 0, 12, 0);
				holder.imageView.setLayoutParams(params1);
				
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					if(!multipleSelect){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						holder.imageView.setImageBitmap(thumb);
					}
				} else {
					thumb = ThumbnailUtils
							.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						if(!multipleSelect){
							holder.imageView.setImageBitmap(thumb);
						}
						else{
							holder.imageView.setImageBitmap(thumb);
						}
					} else {
						try {
							thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks

						if (thumb != null) {
							if(!multipleSelect){
								holder.imageView.setImageBitmap(thumb);
							}
							else{
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
			} else {
				log("Node NOT thumbnail");
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params1.setMargins(20, 0, 12, 0);
					holder.imageView.setLayoutParams(params1);
					
					if(!multipleSelect){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						holder.imageView.setImageBitmap(thumb);
					}
					
				} else {
					thumb = ThumbnailUtils
							.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.setMargins(20, 0, 12, 0);
						
						holder.imageView.setLayoutParams(params1);
						
						if(!multipleSelect){
							holder.imageView.setImageBitmap(thumb);
						}
						else{
							holder.imageView.setImageBitmap(thumb);
						}
					} else {
						try {
							ThumbnailUtilsLollipop.createThumbnailList(context, node,holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks
					}
				}
			}
		}
		
		//Check if is an offline file to show the red arrow
		File offlineFile = null;
		//Find in the database
		MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
		if(offlineNode!=null){
			log("YESS FOUND: "+node.getName());
			if(incoming){
				log("Incoming tab: MegaBrowserListAdapter: "+node.getHandle());				
				//Find in the filesystem
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + offlineNode.getHandleIncoming()+offlineNode.getPath());
					log("offline File: "+offlineFile.getAbsolutePath());
				}
				else{
					offlineFile = context.getFilesDir();
				}
				
			}
			else{
				log("CLOUD tab: MegaBrowserListAdapter: "+node.getHandle());
				//Find in the filesystem
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +megaApi.getNodePath(node));
				}
				else{
					offlineFile = context.getFilesDir();
				}
			}	
		}
		else{
			log("Not found: "+node.getHandle()+" "+node.getName());
		}
		
		if (offlineFile!=null){
			if (offlineFile.exists()){
				holder.savedOffline.setVisibility(View.VISIBLE);
			}
			else{
				holder.savedOffline.setVisibility(View.INVISIBLE);
			}
		}
		else{
			holder.savedOffline.setVisibility(View.INVISIBLE);
		}
	}
	
	private String getInfoFolder(MegaNode n) {
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ context.getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		} else {
			info = numFiles
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_files, numFiles);
		}

		return info;
	}

//	public boolean isEnabled(int position) {
//		// if (position == 0){
//		// return false;
//		// }
//		// else{
//		// return true;
//		// }
//		return super.isEnabled(position);
//	}


	@Override
	public int getItemCount() {
		// TODO Auto-generated method stub
		if (nodes != null){		
			return nodes.size();
		}
		else{
			return 0;
		}
	}
	
	@Override
	public int getItemViewType(int position) {
		return adapterType;
	}
	
	public Object getItem(int position) {
		if (nodes != null){
			return nodes.get(position);
		}
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		positionClicked = p;
		notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		
		ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();

		int currentPosition = holder.currentPosition;
		final MegaNode n = (MegaNode) getItem(currentPosition);

		switch (v.getId()) {		
			case R.id.file_list_three_dots:
			case R.id.file_grid_three_dots:{	
				
				log("onClick: file_list_three_dots: "+currentPosition);			
	
				if (positionClicked == -1) {
					positionClicked = currentPosition;
					notifyDataSetChanged();
				} else {
					if (positionClicked == currentPosition) {
						positionClicked = -1;
						notifyDataSetChanged();
					} else {
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				if(type==ManagerActivityLollipop.CONTACT_FILE_ADAPTER){
					((ContactFileListFragmentLollipop) fragment).showOptionsPanel(n);
				}
				else if(type==ManagerActivityLollipop.FOLDER_LINK_ADAPTER){
					((FolderLinkActivityLollipop) context).showOptionsPanel(n);
				}
				else{
					((ManagerActivityLollipop) context).showOptionsPanel(n);
				}				
				
				break;
			}
			case R.id.file_list_item_layout:
			case R.id.file_grid_item_layout:{
				if(type==ManagerActivityLollipop.RUBBISH_BIN_ADAPTER){
					((RubbishBinFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.INBOX_ADAPTER){
					((InboxFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.INCOMING_SHARES_ADAPTER){
					((IncomingSharesFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER){
					((OutgoingSharesFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.CONTACT_FILE_ADAPTER){
					((ContactFileListFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.FOLDER_LINK_ADAPTER){
					((FolderLinkActivityLollipop) context).itemClick(currentPosition);
				}
				else if(type==ManagerActivityLollipop.SEARCH_ADAPTER){
					((SearchFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else{
					((FileBrowserFragmentLollipop) fragment).itemClick(currentPosition);
				}				
				break;
			}
		}
	}

	/*
	 * Get document at specified position
	 */
	public MegaNode getNodeAt(int position) {
		try {
			if (nodes != null) {
				return nodes.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}

	public long getParentHandle() {
		return parentHandle;
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
		switch (type) {
		case ManagerActivityLollipop.FILE_BROWSER_ADAPTER: {
			log("setParentHandleBrowser -FILE_BROWSER_ADAPTER");
			((ManagerActivityLollipop) context).setParentHandleBrowser(parentHandle);
			break;
		}
		case ManagerActivityLollipop.CONTACT_FILE_ADAPTER: {
			((ContactPropertiesActivityLollipop) context).setParentHandle(parentHandle);
			break;
		}
		case ManagerActivityLollipop.RUBBISH_BIN_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleRubbish(parentHandle);
			break;
		}
		case ManagerActivityLollipop.FOLDER_LINK_ADAPTER: {
			break;
		}
		case ManagerActivityLollipop.SEARCH_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleSearch(parentHandle);
			break;
		}
		case ManagerActivityLollipop.INBOX_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleInbox(parentHandle);
			break;
		}
		case ManagerActivityLollipop.INCOMING_SHARES_ADAPTER: {
			//TODO necesito algo?
			((ManagerActivityLollipop) context).setParentHandleIncoming(parentHandle);
			break;
		}
		case ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER: {
			log("setParentHandleOutgoing -ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER");
			//TODO necesito algo?
			((ManagerActivityLollipop) context).setParentHandleOutgoing(parentHandle);
			break;
		}
		default: {
			log("setParentHandle -default");
//			((ManagerActivityLollipop) context).setParentHandleCloud(parentHandle);
			break;
		}
		}
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

//	public void setOrder(int orderGetChildren) {
//		this.orderGetChildren = orderGetChildren;
//	}

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
			if(megaApi.getParentNode(nodeT).getHandle() == parentHandle){    		
				notifyDataSetChanged();    		
			}
		}

		if (type == ManagerActivityLollipop.SHARED_WITH_ME_ADAPTER){
			notifyDataSetChanged();
		}
	}   

	private static void log(String log) {
		Util.log("MegaBrowserLollipopAdapter", log);
	}
}