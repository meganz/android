package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop.PhotoSyncHolder;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;


public class MegaPhotoSyncListAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList> implements OnClickListener, SectionTitleProvider, View.OnLongClickListener {

	private class Media {
		public String filePath;
		public long timestamp;
	}
	public static final int ITEM_VIEW_TYPE_NODE= 0;
	public static final int ITEM_VIEW_TYPE_MONTH = 1;

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
	LinearLayout emptyTextViewFragment;
	ActionBar aB;
	
	boolean multipleSelect;

	
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	Object fragment;
	int type = Constants.CAMERA_UPLOAD_ADAPTER;
	
	/*public static view holder class*/
    public static class ViewHolderPhotoSyncList extends RecyclerView.ViewHolder {
    	
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
	
	public MegaPhotoSyncListAdapterLollipop(Context _context, ArrayList<PhotoSyncHolder> _nodesArray, long _photosyncHandle, RecyclerView listView, ImageView emptyImageView, LinearLayout emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, Object fragment, int type) {
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

	public void setContext(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return this.context;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}

	public RecyclerView getListFragment() {
		return this.listFragment;
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
					ImageView imageView = (ImageView) v.findViewById(R.id.photo_sync_list_thumbnail);
					int[] positionIV = new int[2];
					imageView.getLocationOnScreen(positionIV);
					int[] screenPosition = new int[4];
					screenPosition[0] = positionIV[0];
					screenPosition[1] = positionIV[1];
					screenPosition[2] = imageView.getWidth();
					screenPosition[3] = imageView.getHeight();
					((CameraUploadFragmentLollipop) fragment).itemClick(currentPosition, imageView, screenPosition);
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
					if(document.isNode ){
						nodes.add(document);

					}
				}
			}
		}
		return nodes;
	}

	boolean putOrDeletePosition (int pos) {
		if (selectedItems.get(pos, false)) {
			LogUtil.logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
			return true;
		}
		else {
			LogUtil.logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
			return false;
		}
	}

	void startAnimation (final int pos, final boolean delete) {
		MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList view = (MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList)listFragment.findViewHolderForLayoutPosition(pos);
		if (view != null) {
			LogUtil.logDebug("Start animation: " + pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					LogUtil.logDebug("onAnimationStart");
					if (!delete) {
						notifyItemChanged(pos);
					}
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					LogUtil.logDebug("onAnimationEnd");
					if (selectedItems.size() <= 0) {
						((CameraUploadFragmentLollipop) fragment).hideMultipleSelect();
					}
					if (delete) {
						notifyItemChanged(pos);
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
		else {
			if (selectedItems.size() <= 0) {
				((CameraUploadFragmentLollipop) fragment).hideMultipleSelect();
			}
			notifyItemChanged(pos);
		}
	}

	public void toggleAllSelection(int pos) {
		LogUtil.logDebug("Position: " + pos);

		startAnimation(pos, putOrDeletePosition(pos));
	}
	
	public void toggleSelection(final int pos) {
		LogUtil.logDebug("Position: " + pos);

		startAnimation(pos, putOrDeletePosition(pos));
	}
	
	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}
	
	public void clearSelections() {
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

	@Override
	public int getItemCount() {
		return nodesArray.size();
	}

	@Override
	public void onBindViewHolder(ViewHolderPhotoSyncList holder, int position) {
		LogUtil.logDebug("Position: " + position);
		
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
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
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
				if (this.isItemChecked(position)) {
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
					params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
					params.setMargins(0,0,0,0);
					holder.imageView.setLayoutParams(params);
					holder.imageView.setImageResource(R.drawable.ic_select_folder);
				}
				else {RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
					params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
					int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
					params.setMargins(left,0,0,0);
					holder.imageView.setLayoutParams(params);
					holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

					if (node.hasThumbnail()) {
						thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
						if (thumb != null) {
							holder.imageView.setImageBitmap(thumb);
						}
						else {
							thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
							if (thumb != null) {
								holder.imageView.setImageBitmap(thumb);
							}
							else {
								try {
									thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaPhotoSyncList(node, context, holder, megaApi, this);
								} catch (Exception e) {
								} //Too many AsyncTasks

								if (thumb != null) {
									holder.imageView.setImageBitmap(thumb);
								}
							}
						}
					}
					else {
						thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
						if (thumb != null) {
							holder.imageView.setImageBitmap(thumb);
						}
						else {
							thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
							if (thumb != null) {
								holder.imageView.setImageBitmap(thumb);
							}
							else {
								try {
									ThumbnailUtilsLollipop.createThumbnailPhotoSyncList(context, node, holder, megaApi, this);
								} catch (Exception e) {
								} //Too many AsyncTasks
							}
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
	public int getItemViewType(int position) {
		LogUtil.logDebug("Position: " + position);
		PhotoSyncHolder psh = (PhotoSyncHolder) getItem(position);
		if (psh.isNode){
				return ITEM_VIEW_TYPE_NODE;
		} else{
			return ITEM_VIEW_TYPE_MONTH;
		}
	}
//	private MegaNode getItemNode(int position) {
//		return nodes.get(position);
//	}

	@Override
	public String getSectionTitle(int position) {
		PhotoSyncHolder psh = (PhotoSyncHolder) getItem(position);
		if (psh.isNode){
			return psh.nodeDate;
		} else{
			return null;
		}
	}

	@Override
	public ViewHolderPhotoSyncList onCreateViewHolder(ViewGroup parent, int viewType) {
		LogUtil.logDebug("onCreateViewHolder");
		
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
		holder.itemLayout.setOnLongClickListener(this);
		
		return holder;
	}

    @Override
    public boolean onLongClick(View v) {
        ViewHolderPhotoSyncList holder = (ViewHolderPhotoSyncList)v.getTag();
        int currentPosition = holder.getAdapterPosition();
        PhotoSyncHolder psHPosition = nodesArray.get(currentPosition);
        if (psHPosition.isNode) {
            ((CameraUploadFragmentLollipop) fragment).activateActionMode();
            ((CameraUploadFragmentLollipop) fragment).itemClick(currentPosition, null, null);
        }

        return true;
    }
}
