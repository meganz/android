package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


public class MegaExplorerLollipopAdapter extends RecyclerView.Adapter<MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop> {
	
	final public static int CLOUD_EXPLORER = 0;
	final public static int INCOMING_SHARES_EXPLORER = 1;

	public static int MAX_WIDTH_FILENAME_LAND=500;
	public static int MAX_WIDTH_FILENAME_PORT=235;

	Context context;
	MegaApiAndroid megaApi;
	MegaPreferences prefs;


	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	ArrayList<MegaNode> nodes;

	DatabaseHandler dbH = null;
	private ArrayList<Long> disabledNodes;

	Object fragment;
	
	long parentHandle = -1;
	boolean selectFile = false;

	boolean multipleSelect;
	private SparseBooleanArray selectedItems;

	OnItemClickListener mItemClickListener;
	RecyclerView listFragment;

	private ArrayList<Long> disabledNodesCloudDrive;

	/*public static view holder class*/
    public class ViewHolderExplorerLollipop extends RecyclerView.ViewHolder implements View.OnClickListener{
    	public ImageView imageView;
		public ImageView permissionsIcon;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public RelativeLayout itemLayout;
    	public int currentPosition;
    	public long document;


    	public ViewHolderExplorerLollipop(View itemView) {
			super(itemView);
//            itemView.setOnClickListener(this);
		}
    	
		@Override
		public void onClick(View v) {
			if(mItemClickListener != null){
				mItemClickListener.onItemClick(v, getPosition());
			}			
		}
    }
    
    public interface OnItemClickListener {
		   public void onItemClick(View view , int position);
	}
	
	public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
		this.mItemClickListener = mItemClickListener;
	}
	
	ViewHolderExplorerLollipop holder = null;    

	public MegaExplorerLollipopAdapter(Context _context, Object fragment, ArrayList<MegaNode> _nodes, long _parentHandle, RecyclerView listView, boolean selectFile){
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.listFragment = listView;
		this.selectFile = selectFile;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		this.fragment = fragment;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
		disabledNodesCloudDrive = new ArrayList<Long>();

	}
	
	@Override
	public int getItemCount() {
		if (nodes == null){
			nodes = new ArrayList<MegaNode>();
		}

		return nodes.size();
	}

	public Object getItem(int position) {
		return nodes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public ViewHolderExplorerLollipop onCreateViewHolder(ViewGroup parent, int viewType) {
		
		listFragment = (RecyclerView) parent;
		//		final int _position = position;
				
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;		
	    float scaleW = Util.getScaleW(outMetrics, density);
		
	    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);

		holder = new ViewHolderExplorerLollipop(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_item_layout);
		holder.imageView = (ImageView) v.findViewById(R.id.file_explorer_thumbnail);
		holder.textViewFileName = (TextView) v.findViewById(R.id.file_explorer_filename);

		holder.textViewFileSize = (TextView) v.findViewById(R.id.file_explorer_filesize);
		holder.permissionsIcon = (ImageView) v.findViewById(R.id.file_explorer_permissions);

		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			holder.textViewFileName.setMaxWidth(Util.scaleWidthPx(260, outMetrics));
			holder.textViewFileSize.setMaxWidth(Util.scaleWidthPx(260, outMetrics));

		}
		else{
			holder.textViewFileName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
			holder.textViewFileSize.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

		}

		v.setTag(holder);
		return holder;
	}

	@Override
	public void onBindViewHolder(ViewHolderExplorerLollipop holder, int position){
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;

		holder.currentPosition = position;
		
		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		holder.textViewFileName.setText(node.getName());
			
		if (node.isFolder()){
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(0, 0, 0, 0);

			holder.imageView.setLayoutParams(params);

			holder.itemLayout.setBackgroundColor(Color.WHITE);

			if (disabledNodes != null){

				if (disabledNodes.contains(node.getHandle())){
					log("Disabled!");
					holder.imageView.setAlpha(.4f);
					holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
					holder.permissionsIcon.setAlpha(.2f);
					holder.itemView.setOnClickListener(null);
				}
				else{
					log("Full access");
					holder.imageView.setAlpha(1.0f);
					holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
					holder.itemView.setOnClickListener(holder);
					holder.permissionsIcon.setAlpha(.35f);
				}
			}
			else{
				holder.imageView.setAlpha(1.0f);
				holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
				holder.itemView.setOnClickListener(holder);
			}

			if(node.isInShare()){
				holder.imageView.setImageResource(R.drawable.ic_folder_incoming_list);
				ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
				for(int j=0; j<sharesIncoming.size();j++){
					MegaShare mS = sharesIncoming.get(j);
					if(mS.getNodeHandle()==node.getHandle()){
						MegaUser user= megaApi.getContact(mS.getUser());
						if(user!=null){
							MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
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

				//Check permissions
				holder.permissionsIcon.setVisibility(View.VISIBLE);
				int accessLevel = megaApi.getAccess(node);

				if(accessLevel== MegaShare.ACCESS_FULL){
					holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
				}
				else if(accessLevel== MegaShare.ACCESS_READ){
					holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
				}
				else{
					holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
				}
			}
			else if(node.isOutShare()||megaApi.isPendingShare(node)) {
				holder.permissionsIcon.setVisibility(View.GONE);
				holder.imageView.setImageResource(R.drawable.ic_folder_outgoing_list);
				holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));

			}else{
				holder.permissionsIcon.setVisibility(View.GONE);
				boolean isCU = isCameraUploads(node);
				if(isCU){
					holder.imageView.setImageResource(R.drawable.ic_folder_image_list);
				}else{
					holder.imageView.setImageResource(R.drawable.ic_folder_list);
				}
				holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node, context));
			}
		}
		else{

			holder.permissionsIcon.setVisibility(View.GONE);
			
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
			holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(0, 0, 0, 0);

			holder.imageView.setLayoutParams(params);

			if(selectFile){

				holder.imageView.setAlpha(1.0f);
				holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
				holder.itemView.setOnClickListener(holder);

				if (multipleSelect) {
					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
						holder.imageView.setImageResource(R.drawable.ic_select_folder);
						log("Do not show thumb");
						return;
					}
					else{
						holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
						holder.itemLayout.setBackgroundColor(Color.WHITE);
					}
				}
				else{
					holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
			}
			else{
				holder.imageView.setAlpha(.4f);
				holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
				holder.itemView.setOnClickListener(null);
			}

			if (node.hasThumbnail()){

				RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
				params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
				params1.setMargins(left, 0, 0, 0);

				holder.imageView.setLayoutParams(params1);

				thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						try{
							thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaExplorerLollipop(node, context, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks

						if (thumb != null){
							holder.imageView.setImageBitmap(thumb);
						}
					}
				}
			}
			else{
				thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
				if (thumb != null){
					RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
					params1.setMargins(left, 0, 0, 0);

					holder.imageView.setLayoutParams(params1);
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
					if (thumb != null){
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
						params1.setMargins(left, 0, 0, 0);

						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						try{
							ThumbnailUtilsLollipop.createThumbnailExplorerLollipop(context, node, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks
					}
				}
			}


		}		
	}

	public void toggleAllSelection(int pos) {
		log("toggleAllSelection: "+pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}

		MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop view = (MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			log("Start animation: "+pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					log("onAnimationEnd");
					if (selectedItems.size() <= 0){
						log("toggleAllSelection: hideMultipleSelect");

						((CloudDriveExplorerFragmentLollipop) fragment).hideMultipleSelect();

					}
					log("toggleAllSelection: notified item changed");
					notifyItemChanged(positionToflip);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
		else{
			log("NULL view pos: "+positionToflip);
			notifyItemChanged(pos);
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


		MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop view = (MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop) listFragment.findViewHolderForLayoutPosition(pos);
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
							((CloudDriveExplorerFragmentLollipop) fragment).hideMultipleSelect();
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});

			view.imageView.startAnimation(flipAnimation);

		}
		else{
			log("view is null - not animation");
		}

	}

	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
                if(!((CloudDriveExplorerFragmentLollipop) fragment).isFolder(i)){
                    toggleAllSelection(i);
                }
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

	public void clearSelectedItems() {
		log("clearSelectedItems");
		selectedItems.clear();
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
		if(selectedItems!=null){
			return selectedItems.size();
		}
		return 0;
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

	public long[] getSelectedHandles() {

		long handles[] = new long[selectedItems.size()];

		int k=0;
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaNode document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					handles[k] = document.getHandle();
					k++;
				}
			}
		}
		return handles;
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

	public boolean isEnabled(int position) {
		if (nodes.size() == 0){
			return false;
		}
		
		MegaNode document = nodes.get(position);
		if (document.isFile()){
			if(selectFile){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			if (disabledNodes != null) {
				if (disabledNodes.contains(document.getHandle())){
					return false;
				}
			}
		}
		
		return true;
	}

	public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public long getParentHandle(){
		return parentHandle;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
	}
	
	/*
	 * Set provided nodes disabled
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		this.disabledNodes = disabledNodes;
	}

	public boolean isSelectFile() {
		return selectFile;
	}

	public void setSelectFile(boolean selectFile) {
		this.selectFile = selectFile;
	}

	private static void log(String log) {
		Util.log("MegaExplorerLollipopAdapter", log);
	}


	public boolean isCameraUploads(MegaNode n){
		log("isCameraUploads()");
		String cameraSyncHandle = null;

		//Check if the item is the Camera Uploads folder
		if(dbH.getPreferences()!=null){
			prefs = dbH.getPreferences();
			if(prefs.getCamSyncHandle()!=null){
				cameraSyncHandle = prefs.getCamSyncHandle();
			}else{
				cameraSyncHandle = null;
			}
		}else{
			prefs=null;
		}

		if(cameraSyncHandle!=null){
			if(!(cameraSyncHandle.equals(""))){
				if ((n.getHandle()==Long.parseLong(cameraSyncHandle))){
					return true;
				}

			}else{
				if(n.getName().equals("Camera Uploads")){
					if (prefs != null){
						prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
					}
					dbH.setCamSyncHandle(n.getHandle());
					log("FOUND Camera Uploads!!----> "+n.getHandle());
					return true;
				}
			}

		}else{
			if(n.getName().equals("Camera Uploads")){
				if (prefs != null){
					prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
				}
				dbH.setCamSyncHandle(n.getHandle());
				log("FOUND Camera Uploads!!: "+n.getHandle());
				return true;
			}
		}

		//Check if the item is the Media Uploads folder
		String secondaryMediaHandle = null;

		if(prefs!=null){
			if(prefs.getMegaHandleSecondaryFolder()!=null){
				secondaryMediaHandle =prefs.getMegaHandleSecondaryFolder();
			}else{
				secondaryMediaHandle = null;
			}
		}

		if(secondaryMediaHandle!=null){
			if(!(secondaryMediaHandle.equals(""))){
				if ((n.getHandle()==Long.parseLong(secondaryMediaHandle))){
					log("Click on Media Uploads");
					return true;
				}
			}
		}else{
			if(n.getName().equals(CameraUploadsService.SECONDARY_UPLOADS)){
				if (prefs != null){
					prefs.setMegaHandleSecondaryFolder(String.valueOf(n.getHandle()));
				}
				dbH.setSecondaryFolderHandle(n.getHandle());
				log("FOUND Media Uploads!!: "+n.getHandle());
				return true;
			}
		}
		return false;
	}

}
