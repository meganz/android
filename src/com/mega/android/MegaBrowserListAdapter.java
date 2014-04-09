package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MegaBrowserListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	NodeList nodes;
	
	long parentHandle = -1;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	boolean multipleSelect;
	boolean isContact = false;
	
	/*public static view holder class*/
    public class ViewHolderBrowserList {
    	CheckBox checkbox;
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
//        ImageButton optionOpen;
        ImageView optionDownload;
        ImageView optionProperties;
        ImageView optionRename;
        ImageView optionCopy;
        ImageView optionMove;
        ImageView optionPublicLink;
        ImageView optionDelete;
        int currentPosition;
        long document;
    }
	
	public MegaBrowserListAdapter(Context _context, NodeList _nodes, long _parentHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, boolean isContact) {
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		if (!isContact){
			((ManagerActivity)context).setParentHandle(parentHandle);
		}
		else{
			((ContactFileListActivity)context).setParentHandle(parentHandle);
		}
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		this.isContact = isContact;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	public void setNodes(NodeList nodes){
		this.nodes = nodes;
		positionClicked = -1;	
		notifyDataSetChanged();
//		listFragment.clearFocus();
//		if (listFragment != null){
//			listFragment.post(new Runnable() {
//                @Override
//                public void run() {                	
//                    listFragment.setSelection(0);
//                }
//            });
//			listFragment.setSelection(0);
//		}
//		list.smoothScrollToPosition(0);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View v;
	
		listFragment = (ListView) parent;
		
		final int _position = position;
		
		ViewHolderBrowserList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_list, parent, false);
			holder = new ViewHolderBrowserList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.file_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_options);
			holder.optionDownload = (ImageView) convertView.findViewById(R.id.file_list_option_download);
			holder.optionDownload.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDownload.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionProperties = (ImageView) convertView.findViewById(R.id.file_list_option_properties);
			holder.optionProperties.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionRename = (ImageView) convertView.findViewById(R.id.file_list_option_rename);
			holder.optionRename.getLayoutParams().width = Util.px2dp((30*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionRename.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionCopy = (ImageView) convertView.findViewById(R.id.file_list_option_copy);
			holder.optionCopy.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionCopy.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionMove = (ImageView) convertView.findViewById(R.id.file_list_option_move);
			holder.optionMove.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionMove.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionPublicLink = (ImageView) convertView.findViewById(R.id.file_list_option_public_link);
			holder.optionPublicLink.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.file_list_option_delete);
			holder.optionDelete.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.file_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderBrowserList) convertView.getTag();
		}
		
		if (!multipleSelect){
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		}
		else{
			holder.checkbox.setVisibility(View.VISIBLE);
			holder.arrowSelection.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			
			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				holder.checkbox.setChecked(true);
			}
			else{
				holder.checkbox.setChecked(false);
			}
		}
		
		holder.currentPosition = position;
		
		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		holder.textViewFileName.setText(node.getName());
		
		if (node.isFolder()){
			holder.textViewFileSize.setText(getInfoFolder(node));
			holder.imageView.setImageResource(R.drawable.mime_folder);
		}
		else{
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
			holder.imageView.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
			
			if (node.hasThumbnail()){
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{ 
						try{
							thumb = ThumbnailUtils.getThumbnailFromMegaList(node, context, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks
						
						if (thumb != null){
							holder.imageView.setImageBitmap(thumb);
						}
					}
				}
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{ 
						try{
							ThumbnailUtils.createThumbnailList(context, node, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks
					}
				}			
			}
		}
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_grey);
				listFragment.smoothScrollToPosition(_position);
				
				if (isContact){
					holder.optionDownload.setVisibility(View.VISIBLE);
					holder.optionProperties.setVisibility(View.VISIBLE);
					holder.optionCopy.setVisibility(View.VISIBLE);
					holder.optionMove.setVisibility(View.GONE);
					holder.optionPublicLink.setVisibility(View.GONE);
					holder.optionRename.setVisibility(View.GONE);
					holder.optionDelete.setVisibility(View.GONE);
					
					holder.optionDownload.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionDownload.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionProperties.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionCopy.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionCopy.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
				}
			}
			else{
				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
			}
		}
		else{
			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
		}
		
//		holder.optionOpen.setTag(holder);
//		holder.optionOpen.setOnClickListener(this);
		
		holder.optionDownload.setTag(holder);
		holder.optionDownload.setOnClickListener(this);
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		
		holder.optionRename.setTag(holder);
		holder.optionRename.setOnClickListener(this);
		
		holder.optionCopy.setTag(holder);
		holder.optionCopy.setOnClickListener(this);
		
		holder.optionMove.setTag(holder);
		holder.optionMove.setOnClickListener(this);
		
		holder.optionDelete.setTag(holder);
		holder.optionDelete.setOnClickListener(this);
		
		holder.optionPublicLink.setTag(holder);
		holder.optionPublicLink.setOnClickListener(this);
		
		return convertView;
	}
	
	private String getInfoFolder (MegaNode n){
		NodeList nL = megaApi.getChildren(n);
		
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nL.size();i++){
			MegaNode c = nL.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
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
	public boolean isEnabled(int position) {
//		if (position == 0){
//			return false;
//		}
//		else{
//			return true;
//		}
		return super.isEnabled(position);
	}

	@Override
    public int getCount() {
        return nodes.size();
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

	@Override
	public void onClick(View v) {
		ViewHolderBrowserList holder = (ViewHolderBrowserList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaNode n = (MegaNode) getItem(currentPosition);
		
		switch (v.getId()){
//			case R.id.file_list_option_open:{
//				
//				if (n.isFolder()){
//					aB.setTitle(n.getName());
//					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = n.getHandle();
//					nodes = megaApi.getChildren(n);
//					setNodes(nodes);
//					listFragment.setSelection(0);
//					
//					//If folder has no files
//					if (getCount() == 0){
//						listFragment.setVisibility(View.GONE);
//						emptyImageViewFragment.setVisibility(View.VISIBLE);
//						emptyTextViewFragment.setVisibility(View.VISIBLE);
//						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_cloud_drive);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_cloud_drive);
//						} else {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_folder);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_folder);
//						}
//					}
//					else{
//						listFragment.setVisibility(View.VISIBLE);
//						emptyImageViewFragment.setVisibility(View.GONE);
//						emptyTextViewFragment.setVisibility(View.GONE);
//					}
//				}
//				else{
//					if (MimeType.typeForName(n.getName()).isImage()){
//						Intent intent = new Intent(context, FullScreenImageViewer.class);
//						intent.putExtra("position", currentPosition);
//						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
//							intent.putExtra("parentNodeHandle", -1L);
//						}
//						else{
//							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
//						}
//						context.startActivity(intent);
//					}
//					else{
//						Toast.makeText(context, "[IS FILE (not image)]Node handle clicked: " + n.getHandle(), Toast.LENGTH_SHORT).show();
//					}
//					positionClicked = -1;
//					notifyDataSetChanged();
//				}				
//				break;
//			}
			case R.id.file_list_option_download:{
				positionClicked = -1;
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				if (!isContact){
					((ManagerActivity) context).onFileClick(handleList);
				}
				else{
					((ContactFileListActivity)context).onFileClick(handleList);
				}
				break;
			}
			case R.id.file_list_option_properties:{
				Intent i = new Intent(context, FilePropertiesActivity.class);
			
				if (n.isFolder()){
					i.putExtra("imageId", R.drawable.mime_folder);
				}
				else{
					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
				}				
				i.putExtra("name", n.getName());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.file_list_option_delete:{
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				if (!isContact){
					((ManagerActivity) context).moveToTrash(handleList);
				}
				break;
			}
			case R.id.file_list_option_public_link:{
				if (!isContact){
					((ManagerActivity) context).getPublicLinkAndShareIt(n);
				}
				break;
			}
			case R.id.file_list_option_rename:{
				if (!isContact){
					((ManagerActivity) context).showRenameDialog(n, n.getName());
				}
				break;
			}
			case R.id.file_list_option_move:{
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				if (!isContact){
					((ManagerActivity) context).showMove(handleList);
				}
				break;
			}
			case R.id.file_list_option_copy:{
				positionClicked = -1;
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				if (!isContact){
					((ManagerActivity) context).showCopy(handleList);
				}
				else{
					((ContactFileListActivity) context).showCopy(handleList);
				}
				break;
			}
			case R.id.file_list_three_dots:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
		}		
	}
	
	/*
	 * Get document at specified position
	 */
	public MegaNode getDocumentAt(int position) {
		try {
			if(nodes != null){
				return nodes.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
		
	public long getParentHandle(){
		return parentHandle;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (!isContact){
			((ManagerActivity)context).setParentHandle(parentHandle);
		}
		else{
			((ContactFileListActivity)context).setParentHandle(parentHandle);
		}
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserListAdapter", log);
	}
}
