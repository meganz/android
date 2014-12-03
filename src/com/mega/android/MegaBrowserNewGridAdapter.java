package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
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

import com.mega.android.utils.ThumbnailUtils;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaTransfer;

public class MegaBrowserNewGridAdapter extends BaseAdapter {
	
	Context context;
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
		else if (type == ManagerActivity.SHARED_WITH_ME_ADAPTER){
			((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
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
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		positionClicked = -1;
		
		contentText.setText(getInfoFolder(megaApi.getNodeByHandle(parentHandle)));
		
		if (getCount() == 0){
			listFragment.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			leftNewFolder.setVisibility(View.VISIBLE);
			rightUploadButton.setVisibility(View.VISIBLE);

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
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);
		}
	}
	
	/*private view holder class*/
    public class ViewHolderBrowserNewGrid {
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsThumbnail;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
    	public ArrayList<ImageView> imageViews;
    	public ArrayList<LinearLayout> menuLayouts;
    	public ArrayList<View> separators;
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
			holder.separators = new ArrayList<View>();
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
				
				ImageView oDo = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_download);
				holder.optionsDownload.add(oDo);
				
				ImageView oDe = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_delete);
				holder.optionsDelete.add(oDe);
				
				ImageView oP = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_properties);
				holder.optionsProperties.add(oP);
				
				ImageView oO = (ImageView) rLView.findViewById(R.id.grid_menu_layout_option_overflow);
				holder.optionsOverflow.add(oO);
				
				View s = (View) rLView.findViewById(R.id.cell_separator);
				holder.separators.add(s);
				
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
				if (holder.documents.size() > i){
					holder.documents.set(i,  -1l);
				}
				else{
					holder.documents.add(i, -1l);
				}
			}
			else{
				holder.relativeLayoutsThumbnail.get(i).setVisibility(View.VISIBLE);
				holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
				holder.progressBars.get(i).setVisibility(View.GONE);
				
				if (totalPosition == positionClicked){
					holder.imageViews.get(i).setVisibility(View.GONE);
					holder.menuLayouts.get(i).setVisibility(View.VISIBLE);
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.separators.get(i).getLayoutParams();
					params.addRule(RelativeLayout.BELOW, holder.menuLayouts.get(i).getId());
				}
				else{
					holder.imageViews.get(i).setVisibility(View.VISIBLE);
					holder.menuLayouts.get(i).setVisibility(View.GONE);
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.separators.get(i).getLayoutParams();
					params.addRule(RelativeLayout.BELOW, holder.imageViews.get(i).getId());
				}
				
				MegaNode node = nodes.get(totalPosition);
				if (holder.documents.size() > i){
					holder.documents.set(i, node.getHandle());
				}
				else{
					holder.documents.add(i, node.getHandle());
				}				
				holder.fileNameViews.get(i).setText(node.getName());
				if (nodes.get(totalPosition).isFolder()){
					if (megaApi.isShared(node)){
						holder.imageViews.get(i).setImageResource(R.drawable.mime_folder_shared);	
					}
					else{
						holder.imageViews.get(i).setImageResource(R.drawable.mime_folder);	
					}
					holder.fileSizeViews.get(i).setText(getInfoFolder(node));
				}
				else{
					holder.imageViews.get(i).setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
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
								try{
									thumb = ThumbnailUtils.getThumbnailFromMegaNewGrid(node, context, holder, megaApi, this, i);
								}
								catch(Exception e) {}
								
								if (thumb != null){
									holder.imageViews.get(i).setImageBitmap(thumb);
								}
								else{
									holder.imageViews.get(i).setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
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
					MegaNode n = megaApi.getNodeByHandle(handle);
					nodeClicked(handle, totalPosition);
				}
			} );
			
			iV.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					ViewHolderBrowserNewGrid holder= (ViewHolderBrowserNewGrid) v.getTag();
					
					long handle = holder.documents.get(index);
//					RelativeLayout rL = holder.relativeLayoutsThumbnail.get(index);
//					rL.setBackgroundColor(Color.parseColor("#000000"));
					MegaNode n = megaApi.getNodeByHandle(handle);
//					nodeClicked(handle, totalPosition);
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
					
					threeDotsClicked(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oDo = holder.optionsDownload.get(i);
			oDo.setTag(holder);
			oDo.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onClickDownload(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oP = holder.optionsProperties.get(i);
			oP.setTag(holder);
			oP.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onClickProperties(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oDe = holder.optionsDelete.get(i);
			oDe.setTag(holder);
			oDe.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onClickDelete(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oO = holder.optionsOverflow.get(i);
			oO.setTag(holder);
			oO.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderBrowserNewGrid holder = (ViewHolderBrowserNewGrid) v.getTag();
					
					onClickOverflow(holder, positionFinal, index, totalPosition);
				}
			});

		}
		
		return convertView;
	}
	
	public void onClickOverflow(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		final MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		if ((type == ManagerActivity.FILE_BROWSER_ADAPTER)	|| (type == ManagerActivity.SEARCH_ADAPTER)) {

			AlertDialog moreOptionsDialog;

			String [] optionsString = null;
			if (n.isFolder()){
				optionsString = new String[] {context.getString(R.string.context_share_folder), context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy), context.getString(R.string.context_send_link)}; 
			}
			else{
				optionsString = new String[] {context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy), context.getString(R.string.context_send_link)};
			}
			
			final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_text, android.R.id.text1, optionsString);
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("More options");
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
						case 4:{
							setPositionClicked(-1);
							notifyDataSetChanged();
							((ManagerActivity) context).getPublicLinkAndShareIt(n);
							break;
						}
					}

					dialog.dismiss();
				}
			});
			
			builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			moreOptionsDialog = builder.create();
			moreOptionsDialog.show();
			brandAlertDialog(moreOptionsDialog);
		}
	}
	
	public static void brandAlertDialog(AlertDialog dialog) {
	    try {
	        Resources resources = dialog.getContext().getResources();

	        int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");

	        TextView alertTitle = (TextView) dialog.getWindow().getDecorView().findViewById(alertTitleId);
	        if (alertTitle != null){	        	
	        	alertTitle.setTextColor(dialog.getContext().getResources().getColor(R.color.mega)); // change title text color
	        }

	        int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
	        View titleDivider = dialog.getWindow().getDecorView().findViewById(titleDividerId);
	        if (titleDivider != null){
	        	titleDivider.setBackgroundColor(dialog.getContext().getResources().getColor(R.color.mega)); // change divider color
	        }
	    } catch (Exception ex) {
	    	Toast.makeText(dialog.getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
	        ex.printStackTrace();
	    }
	}
	
	public void onClickDelete(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
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
	
	public void onClickProperties(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		MegaNode n = megaApi.getNodeByHandle(holder.documents.get(index));
		
		Intent i = new Intent(context, FilePropertiesActivity.class);
		i.putExtra("handle", n.getHandle());

		if (n.isFolder()) {
			if (megaApi.isShared(n)){
				i.putExtra("imageId", R.drawable.mime_folder_shared);	
			}
			else{
				i.putExtra("imageId", R.drawable.mime_folder);
			}

		} 
		else {
			i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());
		}
		i.putExtra("name", n.getName());
		context.startActivity(i);
		this.positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void onClickDownload(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
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
	
	public void threeDotsClicked(ViewHolderBrowserNewGrid holder, int position, int index, int totalPosition){
		if (positionClicked == totalPosition){
			holder.imageViews.get(index).setVisibility(View.VISIBLE);
			holder.menuLayouts.get(index).setVisibility(View.GONE);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.separators.get(index).getLayoutParams();
			params.addRule(RelativeLayout.BELOW, holder.imageViews.get(index).getId());
			this.positionClicked = -1;
			notifyDataSetChanged();
		}
		else{
			holder.imageViews.get(index).setVisibility(View.GONE);
			holder.menuLayouts.get(index).setVisibility(View.VISIBLE);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.separators.get(index).getLayoutParams();
			params.addRule(RelativeLayout.BELOW, holder.menuLayouts.get(index).getId());
			this.positionClicked = totalPosition;
			notifyDataSetChanged();
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
    
    public void nodeClicked (long handle, int totalPosition){
    	MegaNode n = megaApi.getNodeByHandle(handle);
    	
    	if (n.isFolder()){
			
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
			else if (type == ManagerActivity.SHARED_WITH_ME_ADAPTER){
				((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
			}
			nodes = megaApi.getChildren(n, orderGetChildren);
			setNodes(nodes);
			listFragment.setSelection(0);
		}
		else{
			if (MimeType.typeForName(n.getName()).isImage()){
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
			else if (MimeType.typeForName(n.getName()).isVideo() || MimeType.typeForName(n.getName()).isAudio() ){
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
		  		String mimeType = MimeType.typeForName(file.getName()).getType();
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
		else if (type == ManagerActivity.SHARED_WITH_ME_ADAPTER){
			((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
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
    	if(megaApi.getParentNode(nodeT).getHandle()==parentHandle){    		
    		notifyDataSetChanged();    		
    	}
    } 
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserNewGridAdapter", log);
	}
}
