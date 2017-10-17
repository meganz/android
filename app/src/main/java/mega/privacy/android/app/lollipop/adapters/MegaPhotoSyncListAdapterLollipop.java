package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop.PhotoSyncHolder;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;


public class MegaPhotoSyncListAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList> implements OnClickListener {
	
	private class Media {
		public String filePath;
		public long timestamp;
	}
	
	private SparseBooleanArray selectedItems = new SparseBooleanArray();;
	
	ViewHolderPhotoSyncList holder = null;

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
	int type = Constants.CAMERA_UPLOAD_ADAPTER;
	
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
				if (type == Constants.CAMERA_UPLOAD_ADAPTER){
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
		
//		if(this.multipleSelect){
//			selectedItems = new SparseBooleanArray();
//		}
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
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_file_list_selected_row));
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
