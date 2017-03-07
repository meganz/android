package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
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

	Context context;
	MegaApiAndroid megaApi;

//	int positionClicked;
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
	boolean inbox = false;
	DatabaseHandler dbH = null;
	boolean multipleSelect;
	int type = Constants.FILE_BROWSER_ADAPTER;
	int adapterType;

//	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	/* public static view holder class */
	public static class ViewHolderBrowser extends ViewHolder {

		public ViewHolderBrowser(View v) {
			super(v);
		}
		
		public ImageView imageView;
		public ImageView savedOffline;
		public ImageView publicLinkImage;
		public TextView textViewFileName;
		public TextView textViewFileSize;
		public long document;
		public RelativeLayout itemLayout;
		public ImageButton imageButtonThreeDots;
		public ProgressBar transferProgressBar;
	}
	
	public static class ViewHolderBrowserList extends ViewHolderBrowser{
		
		public ViewHolderBrowserList(View v){
			super(v);
		}			

		public RelativeLayout itemLayout;
		public ImageView permissionsIcon;
	}
	
	public static class ViewHolderBrowserGrid extends ViewHolderBrowser{
		
		public ViewHolderBrowserGrid(View v){
			super(v);
		}
		
		public View separator;
		
	}

	public void toggleAllSelection(int pos) {
		log("toggleSelection: "+pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}

		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			log("adapter type is LIST");
			MegaBrowserLollipopAdapter.ViewHolderBrowserList view = (MegaBrowserLollipopAdapter.ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				log("Start animation: "+pos);
				Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
				flipAnimation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (selectedItems.size() <= 0){

							if(type==Constants.RUBBISH_BIN_ADAPTER){
								((RubbishBinFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.INBOX_ADAPTER){
								((InboxFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.INCOMING_SHARES_ADAPTER){
								((IncomingSharesFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.OUTGOING_SHARES_ADAPTER){
								((OutgoingSharesFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.CONTACT_FILE_ADAPTER){
								((ContactFileListFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.FOLDER_LINK_ADAPTER){
								((FolderLinkActivityLollipop) context).hideMultipleSelect();
							}
							else if(type==Constants.SEARCH_ADAPTER){
								((SearchFragmentLollipop) fragment).hideMultipleSelect();
							}
							else{
								((FileBrowserFragmentLollipop) fragment).hideMultipleSelect();
							}
						}
						notifyItemChanged(positionToflip);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
				view.imageView.startAnimation(flipAnimation);
			}
		}
		else {
			log("adapter type is GRID");
		}
	}

	public void toggleSelection(int pos) {
		log("toggleSelection: "+pos);

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			log("adapter type is LIST");
			MegaBrowserLollipopAdapter.ViewHolderBrowserList view = (MegaBrowserLollipopAdapter.ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				log("Start animation: "+pos);
				Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
				flipAnimation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (selectedItems.size() <= 0){

							if(type==Constants.RUBBISH_BIN_ADAPTER){
								((RubbishBinFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.INBOX_ADAPTER){
								((InboxFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.INCOMING_SHARES_ADAPTER){
								((IncomingSharesFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.OUTGOING_SHARES_ADAPTER){
								((OutgoingSharesFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.CONTACT_FILE_ADAPTER){
								((ContactFileListFragmentLollipop) fragment).hideMultipleSelect();
							}
							else if(type==Constants.FOLDER_LINK_ADAPTER){
								((FolderLinkActivityLollipop) context).hideMultipleSelect();
							}
							else if(type==Constants.SEARCH_ADAPTER){
								((SearchFragmentLollipop) fragment).hideMultipleSelect();
							}
							else{
								((FileBrowserFragmentLollipop) fragment).hideMultipleSelect();
							}
						}
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
				view.imageView.startAnimation(flipAnimation);
			}
		}
		else{
			log("adapter type is GRID");
		}
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

	public void clearSelections() {
		log("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

//	public void clearSelections() {
//		if(selectedItems!=null){
//			selectedItems.clear();
//			for (int i= 0; i<this.getItemCount();i++) {
//				if (isItemChecked(i)) {
//					toggleAllSelection(i);
//				}
//			}
//		}
//		notifyDataSetChanged();
//	}
//
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
			case Constants.FILE_BROWSER_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleBrowser(parentHandle);			
				break;
			}
			case Constants.CONTACT_FILE_ADAPTER: {
				((ContactFileListActivityLollipop) context).setParentHandle(parentHandle);
				break;
			}
			case Constants.RUBBISH_BIN_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleRubbish(parentHandle);
				break;
			}		
			case Constants.FOLDER_LINK_ADAPTER: {
				megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
				break;
			}
			case Constants.SEARCH_ADAPTER: {
				((ManagerActivityLollipop) context).setParentHandleSearch(parentHandle);
				break;
			}
			case Constants.OUTGOING_SHARES_ADAPTER: {
//				((ManagerActivityLollipop) context).setParentHandleOutgoing(parentHandle);
				break;
			}
			case Constants.INCOMING_SHARES_ADAPTER: {
				incoming=true;
				((ManagerActivityLollipop) context).setParentHandleIncoming(parentHandle);
				break;
			}
			case Constants.INBOX_ADAPTER: {
				log("onCreate INBOX_ADAPTER");
				inbox=true;
				((ManagerActivityLollipop) context).setParentHandleInbox(parentHandle);
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
		notifyDataSetChanged();
	}
	
	public void setAdapterType(int adapterType){
		this.adapterType = adapterType;
	}

	@Override
	public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);


		if (viewType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			log("onCreateViewHolder -> type: ITEM_VIEW_TYPE_LIST");
		
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
		
			ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
			holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.file_list_item_layout);
			holderList.imageView = (ImageView) v.findViewById(R.id.file_list_thumbnail);
			holderList.savedOffline = (ImageView) v.findViewById(R.id.file_list_saved_offline);
			
			holderList.publicLinkImage = (ImageView) v.findViewById(R.id.file_list_public_link);
			holderList.permissionsIcon = (ImageView) v.findViewById(R.id.file_list_incoming_permissions);
			
			holderList.textViewFileName = (TextView) v.findViewById(R.id.file_list_filename);			

			holderList.textViewFileSize = (TextView) v.findViewById(R.id.file_list_filesize);
			holderList.transferProgressBar = (ProgressBar) v.findViewById(R.id.transfers_list_browser_bar);
			
			holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.file_list_three_dots);

			holderList.savedOffline.setVisibility(View.INVISIBLE);
		
			holderList.publicLinkImage.setVisibility(View.INVISIBLE);
			
			holderList.transferProgressBar.setVisibility(View.GONE);
			holderList.textViewFileSize.setVisibility(View.VISIBLE);
			
			holderList.itemLayout.setTag(holderList);
			holderList.itemLayout.setOnClickListener(this);
			
			holderList.imageButtonThreeDots.setTag(holderList);
			holderList.imageButtonThreeDots.setOnClickListener(this);

			v.setTag(holderList);
			
			return holderList;
		}
		else if (viewType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			log("onCreateViewHolder -> type: ITEM_VIEW_TYPE_LIST");

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
			if(holderGrid.textViewFileSize!=null){
				holderGrid.textViewFileSize.setVisibility(View.VISIBLE);
			}
			else{
				log("textViewFileSize is NULL");
			}

			holderGrid.savedOffline.setVisibility(View.INVISIBLE);
			holderGrid.publicLinkImage.setVisibility(View.GONE);
			
			holderGrid.itemLayout.setTag(holderGrid);
			holderGrid.itemLayout.setOnClickListener(this);
			
			holderGrid.imageButtonThreeDots.setTag(holderGrid);
			holderGrid.imageButtonThreeDots.setOnClickListener(this);

			v.setTag(holderGrid);
			
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
			holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
			holder.separator.setBackgroundColor(context.getResources().getColor(R.color.new_background_fragment));
		} 
		else {

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid_long_click_lollipop));
				holder.separator.setBackgroundColor(Color.WHITE);
			}
			else{
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
				holder.separator.setBackgroundColor(context.getResources().getColor(R.color.new_background_fragment));
			}
		}
		
		if(node.isExported()){
			//Node has public link			
			holder.publicLinkImage.setVisibility(View.VISIBLE);
			if(node.isExpired()){
				log("Node exported but expired!!");
				holder.publicLinkImage.setVisibility(View.INVISIBLE);
			}
		}
		else{
			holder.publicLinkImage.setVisibility(View.INVISIBLE);
		}
		
		if (node.isFolder()) {
			holder.transferProgressBar.setVisibility(View.GONE);		
			holder.textViewFileSize.setVisibility(View.VISIBLE);

			if(type==Constants.FOLDER_LINK_ADAPTER){
				holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
			}else{
				holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));
			}
			
			if(type==Constants.INCOMING_SHARES_ADAPTER){
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
			else if (type==Constants.OUTGOING_SHARES_ADAPTER){
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
			log("Node found OFFLINE: "+offlineNode.getName());
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
			else if(inbox){
				String pathMega = megaApi.getNodePath(node);
				pathMega = pathMega.replace("/in", "");
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +pathMega+offlineNode.getName());
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
		else{
			log("Node NOT found OFFLINE: "+node.getName());
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

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		log("Node to show: "+position+" "+node.getName());
		holder.textViewFileName.setText(node.getName());
	
		holder.textViewFileSize.setText("");
		
		holder.publicLinkImage.setVisibility(View.INVISIBLE);
		holder.permissionsIcon.setVisibility(View.GONE);

		if(node.isExported()){
			//Node has public link
			holder.publicLinkImage.setVisibility(View.VISIBLE);
			if(node.isExpired()){
				log("Node exported but expired!!");
				holder.publicLinkImage.setVisibility(View.INVISIBLE);
			}
		}
		else{
			holder.publicLinkImage.setVisibility(View.INVISIBLE);
		}

		if (node.isFolder()) {

			holder.itemLayout.setBackgroundColor(Color.WHITE);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(0, 0, 0, 0);
			holder.imageView.setLayoutParams(params);
			
			holder.transferProgressBar.setVisibility(View.GONE);		
			holder.textViewFileSize.setVisibility(View.VISIBLE);
//			holder.propertiesText.setText(R.string.general_folder_info);
			holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));

			if(type==Constants.FOLDER_LINK_ADAPTER){
				holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
				if (!multipleSelect) {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageView.setImageResource(R.drawable.ic_folder_list);
				}
				else {

					if(this.isItemChecked(position)){
						RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.setMargins(16, 0, 8, 0);
						holder.imageView.setLayoutParams(paramsMultiselect);

						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
						holder.imageView.setImageResource(R.drawable.ic_multiselect);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
						holder.imageView.setImageResource(R.drawable.ic_folder_list);
					}
				}
			}
			else if(type==Constants.CONTACT_FILE_ADAPTER){
				if (!multipleSelect) {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				}
				else {

					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
						RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.setMargins(16, 0, 8, 0);
						holder.imageView.setLayoutParams(paramsMultiselect);
						holder.imageView.setImageResource(R.drawable.ic_multiselect);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					}
				}

				int accessLevel = megaApi.getAccess(node);

				if(accessLevel== MegaShare.ACCESS_FULL){
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
				}
				else if(accessLevel== MegaShare.ACCESS_READWRITE){
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
				}
				else{
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
				}
				holder.permissionsIcon.setVisibility(View.VISIBLE);
			}
			else if(type==Constants.INCOMING_SHARES_ADAPTER){
				holder.publicLinkImage.setVisibility(View.INVISIBLE);
				if (!multipleSelect) {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				}
				else {

					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
						RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.setMargins(16, 0, 8, 0);
						holder.imageView.setLayoutParams(paramsMultiselect);
						holder.imageView.setImageResource(R.drawable.ic_multiselect);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					}
				}

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

				int accessLevel = megaApi.getAccess(node);

				if(accessLevel== MegaShare.ACCESS_FULL){
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
				}
				else if(accessLevel== MegaShare.ACCESS_READWRITE){
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
				}
				else{
					holder.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
				}
				holder.permissionsIcon.setVisibility(View.VISIBLE);
			}
			else if (type==Constants.OUTGOING_SHARES_ADAPTER){
				if (!multipleSelect) {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
				}
				else {

					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
						RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.setMargins(16, 0, 8, 0);
						holder.imageView.setLayoutParams(paramsMultiselect);
						holder.imageView.setImageResource(R.drawable.ic_multiselect);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					}
				}
				//Show the number of contacts who shared the folder
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
				if (sl != null) {
					if(sl.size()!=0){
						holder.textViewFileSize.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact)+" "+sl.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
					}										
				}
			}
			else{
				if (!multipleSelect) {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					if(node.isShared()||node.isInShare()){
						holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
					}
					else{
						holder.imageView.setImageResource(R.drawable.ic_folder_list);
					}
				}
				else {

					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
						RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
						paramsMultiselect.setMargins(16, 0, 8, 0);
						holder.imageView.setLayoutParams(paramsMultiselect);
						holder.imageView.setImageResource(R.drawable.ic_multiselect);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
						if(node.isShared()||node.isInShare()){
							holder.imageView.setImageResource(R.drawable.ic_folder_shared_list);
						}
						else{
							holder.imageView.setImageResource(R.drawable.ic_folder_list);
						}
					}
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

			if (!multipleSelect) {
				holder.itemLayout.setBackgroundColor(Color.WHITE);
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
			else {

				if(this.isItemChecked(position)){
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
					RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
					paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
					paramsMultiselect.setMargins(16, 0, 8, 0);
					holder.imageView.setLayoutParams(paramsMultiselect);
					holder.imageView.setImageResource(R.drawable.ic_multiselect);
				}
				else{
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.white));

					log("Check the thumb");

					if (node.hasThumbnail()) {
						log("Node has thumbnail");
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.setMargins(18, 0, 18, 0);
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
								log("NOT thumbnail");
								holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
								try {
									ThumbnailUtilsLollipop.createThumbnailList(context, node,holder, megaApi, this);
								} catch (Exception e) {
								} // Too many AsyncTasks
							}
						}
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
			else if(inbox){
				log("In Inbox");
				String pathMega = megaApi.getNodePath(node);
				pathMega = pathMega.replace("/in", "");
				if (Environment.getExternalStorageDirectory() != null){
					offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +pathMega);
					log("The path to find is: "+offlineFile.getPath());
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

	@Override
	public void onClick(View v) {
		log("onClick");
		
		ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		log("onClick -> Current position: "+currentPosition);

		final MegaNode n = (MegaNode) getItem(currentPosition);

		switch (v.getId()) {		
			case R.id.file_list_three_dots:
			case R.id.file_grid_three_dots:{	
				
				log("onClick: file_list_three_dots: "+currentPosition);			

				if(type==Constants.CONTACT_FILE_ADAPTER){
					((ContactFileListFragmentLollipop) fragment).showOptionsPanel(n);
				}
				else if(type==Constants.FOLDER_LINK_ADAPTER){
					((FolderLinkActivityLollipop) context).showOptionsPanel(n);
				}
				else{
					((ManagerActivityLollipop) context).showNodeOptionsPanel(n);
				}				
				
				break;
			}
			case R.id.file_list_item_layout:
			case R.id.file_grid_item_layout:{
				if(type==Constants.RUBBISH_BIN_ADAPTER){
					((RubbishBinFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==Constants.INBOX_ADAPTER){
					((InboxFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==Constants.INCOMING_SHARES_ADAPTER){
					((IncomingSharesFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==Constants.OUTGOING_SHARES_ADAPTER){
					((OutgoingSharesFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==Constants.CONTACT_FILE_ADAPTER){
					((ContactFileListFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(type==Constants.FOLDER_LINK_ADAPTER){
					((FolderLinkActivityLollipop) context).itemClick(currentPosition);
				}
				else if(type==Constants.SEARCH_ADAPTER){
					((SearchFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else{
					log("click layout FileBrowserFragmentLollipop!");
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
		case Constants.FILE_BROWSER_ADAPTER: {
			log("setParentHandleBrowser -FILE_BROWSER_ADAPTER");
			((ManagerActivityLollipop) context).setParentHandleBrowser(parentHandle);
			break;
		}
		case Constants.CONTACT_FILE_ADAPTER: {
			((ContactFileListActivityLollipop) context).setParentHandle(parentHandle);
			break;
		}
		case Constants.RUBBISH_BIN_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleRubbish(parentHandle);
			break;
		}
		case Constants.FOLDER_LINK_ADAPTER: {
			break;
		}
		case Constants.SEARCH_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleSearch(parentHandle);
			break;
		}
		case Constants.INBOX_ADAPTER: {
			log("setParentHandleBrowser -INBOX_ADAPTER");
			((ManagerActivityLollipop) context).setParentHandleInbox(parentHandle);
			break;
		}
		case Constants.INCOMING_SHARES_ADAPTER: {
			((ManagerActivityLollipop) context).setParentHandleIncoming(parentHandle);
			break;
		}
		case Constants.OUTGOING_SHARES_ADAPTER: {
			log("setParentHandleOutgoing -ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER");
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
		log("setMultipleSelect");
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
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

		if (type == Constants.SHARED_WITH_ME_ADAPTER){
			notifyDataSetChanged();
		}
	}

	private static void log(String log) {
		Util.log("MegaBrowserLollipopAdapter", log);
	}
}