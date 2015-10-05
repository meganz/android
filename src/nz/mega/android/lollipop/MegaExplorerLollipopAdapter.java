package nz.mega.android.lollipop;

import java.util.ArrayList;

import nz.mega.android.MegaApplication;
import nz.mega.android.MimeTypeList;
import nz.mega.android.R;
import nz.mega.android.utils.ThumbnailUtilsLollipop;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MegaExplorerLollipopAdapter extends RecyclerView.Adapter<MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop> implements OnClickListener{
	
	final public static int CLOUD_EXPLORER = 0;
	final public static int INCOMING_SHARES_EXPLORER = 1;

	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	ArrayList<MegaNode> nodes;
	
	private ArrayList<Long> disabledNodes;
	
	long parentHandle = -1;
	boolean selectFile = false;
	int caller;
	
	OnItemClickListener mItemClickListener;
	RecyclerView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	
	/*public static view holder class*/
    public class ViewHolderExplorerLollipop extends RecyclerView.ViewHolder implements View.OnClickListener{
    	public ImageView imageView;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public TextView textViewUpdated;
    	public RelativeLayout itemLayout;
    	public int currentPosition;
    	public long document;    	
    	
    	public ViewHolderExplorerLollipop(View itemView) {
			super(itemView);
            itemView.setOnClickListener(this);
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
    
	
	public MegaExplorerLollipopAdapter(Context _context, ArrayList<MegaNode> _nodes, long _parentHandle, RecyclerView listView, ImageView emptyImageView, TextView emptyTextView, boolean selectFile){
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.selectFile = selectFile;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public int getItemCount() {
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
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
		holder.textViewFileSize = (TextView) v.findViewById(R.id.file_explorer_filesize);
		holder.textViewUpdated = (TextView) v.findViewById(R.id.file_explorer_updated);
			
		v.setTag(holder);
		return holder;
	}

	@Override
	public void onBindViewHolder(ViewHolderExplorerLollipop holder, int position){
		
		final int _position = position;		

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		holder.currentPosition = position;
		
		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		holder.textViewFileName.setText(node.getName());
			
		if (node.isFolder()){			

			Util.setViewAlpha(holder.imageView, 1);
			holder.textViewFileName.setTextColor(context.getResources().getColor(android.R.color.black));
			
			if (disabledNodes != null){
				if (disabledNodes.contains(node.getHandle())){
					Util.setViewAlpha(holder.imageView,  .4f);
					holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
				}
			}
			holder.textViewFileSize.setText(getInfoFolder(node));
			holder.imageView.setImageResource(R.drawable.ic_folder_list);
		}
		else{
			if(selectFile)
			{
				Util.setViewAlpha(holder.imageView, 1);
				holder.textViewFileName.setTextColor(context.getResources().getColor(android.R.color.black));	
			}
			else{
				Util.setViewAlpha(holder.imageView, .4f);
				holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
			}			
			
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
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node, context);
					if (thumb != null){
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
		
		long nodeDate = node.getCreationTime();
		if (nodeDate != 0){
			try{ 
				holder.textViewUpdated.setText(Util.getDateString(nodeDate));
			}
			catch(Exception ex) {
				holder.textViewUpdated.setText(""); 
			}
		}
		else{
			holder.textViewUpdated.setText("");
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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
