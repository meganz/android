package com.mega.android;

import java.util.ArrayList;
import java.util.HashMap;

import com.mega.android.utils.ThumbnailUtils;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.ShareList;
import com.mega.sdk.TransferList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MegaShareInOutAdapter extends BaseAdapter implements OnClickListener {

	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	//NodeList nodes;

	long parentHandle = -1;

	ArrayList<MegaShareIn> megaShareInList;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	HashMap<Long, MegaTransfer> mTHash = null;
	
	MegaTransfer currentTransfer = null;

	//boolean multipleSelect;
	int type = ManagerActivity.FILE_BROWSER_ADAPTER;

	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	/* public static view holder class */
	public class ViewHolderInOutShareList {
		public CheckBox checkbox;
		public ImageView imageView;
		public TextView textViewFileName;
		public TextView textViewFileSize;
		public ImageButton imageButtonThreeDots;
		public RelativeLayout itemLayout;
		//public ImageView arrowSelection;
		public RelativeLayout optionsLayout;
		public ImageView optionDownload;
		public ImageView optionProperties;
		public ProgressBar transferProgressBar;
		public ImageView optionRename;
		public ImageView optionPublicLink;
		public ImageView optionDelete;
		public int currentPosition;
		public long document;
		//public TextView textViewOwner;
	}

	public MegaShareInOutAdapter(Context _context, ArrayList<MegaShareIn> _megaShareInList,long _parentHandle, ListView listView, ImageView emptyImageView,TextView emptyTextView, ActionBar aB, int type) {
		this.context = _context;
		this.megaShareInList = _megaShareInList;
		this.parentHandle = _parentHandle;

		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;

		this.positionClicked = -1;

		this.type = type;

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
		}
	}

	public void setNodes(ArrayList<MegaShareIn> _megaShareInList) {
		this.megaShareInList = _megaShareInList;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		listFragment = (ListView) parent;
		final int _position = position;

		ViewHolderInOutShareList holder = null;

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_list, parent,false);
			holder = new ViewHolderInOutShareList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.file_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);			
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225 * scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.transferProgressBar = (ProgressBar) convertView.findViewById(R.id.transfers_list__browser_bar);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);

			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_options);
			
			//holder.textViewOwner = (TextView) convertView.findViewById(R.id.file_list_owner);				
					
			holder.optionDownload = (ImageView) convertView.findViewById(R.id.file_list_option_download);
			holder.optionDownload.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);	
			((TableRow.LayoutParams) holder.optionDownload.getLayoutParams()).setMargins(Util.px2dp((11 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);
			
			holder.optionProperties = (ImageView) convertView.findViewById(R.id.file_list_option_properties);
			holder.optionProperties.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((10 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);
			
			holder.optionRename = (ImageView) convertView.findViewById(R.id.file_list_option_rename);
			holder.optionRename.getLayoutParams().width = Util.px2dp((40 * scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionRename.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);
			
			holder.optionPublicLink = (ImageView) convertView.findViewById(R.id.file_list_option_public_link);
			holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
			((TableRow.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);
			
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.file_list_option_delete);
			holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);
//			
//			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.file_list_arrow_selection);
//			holder.arrowSelection.setVisibility(View.GONE);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolderInOutShareList) convertView.getTag();
		}

//		if (!multipleSelect) {
//			holder.checkbox.setVisibility(View.GONE);
//			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
//		} else {
//			holder.checkbox.setVisibility(View.VISIBLE);
////			holder.arrowSelection.setVisibility(View.GONE);
//			holder.imageButtonThreeDots.setVisibility(View.GONE);
//
//			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
//			if (checkedItems.get(position, false) == true) {
//				holder.checkbox.setChecked(true);
//			} else {
//				holder.checkbox.setChecked(false);
//			}
//		}
		
		holder.transferProgressBar.setVisibility(View.GONE);
		holder.textViewFileSize.setVisibility(View.VISIBLE);

		holder.currentPosition = position;

//		HashMap<MegaUser,NodeList> _inSHash
		
		
		
		MegaShareIn mUserShare = (MegaShareIn) getItem(position);
		MegaNode node = mUserShare.getNode();
		MegaUser megaUser = mUserShare.getUser();

		Bitmap thumb = null;

		String data = node.getName()+" ("+megaUser.getEmail()+")";
		holder.textViewFileName.setText(data);
		//holder.textViewOwner.setText(megaUser.getEmail());

		if (node.isFolder()) {
			holder.textViewFileSize.setText(getInfoFolder(node));			
			
			holder.imageView.setImageResource(R.drawable.mime_folder_shared);
				
		} else {
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
					}
				}
				
				if(mTHash.size() == 0){
					holder.transferProgressBar.setVisibility(View.GONE);		
					holder.textViewFileSize.setVisibility(View.VISIBLE);	
				}
			}			
			
			holder.imageView.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());

			if (node.hasThumbnail()) {
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					holder.imageView.setImageBitmap(thumb);
				} else {
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						holder.imageView.setImageBitmap(thumb);
					} else {
						try {
							thumb = ThumbnailUtils.getThumbnailFromMegaListShare(node,context, holder, megaApi, this);
							
						} catch (Exception e) {
						} // Too many AsyncTasks

						if (thumb != null) {
							holder.imageView.setImageBitmap(thumb);
						}
					}
				}
			} else {
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null) {
					holder.imageView.setImageBitmap(thumb);
				} else {
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null) {
						holder.imageView.setImageBitmap(thumb);
					} else {
						try {
							ThumbnailUtils.createThumbnailListShare(context, node,holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks
					}
				}
			}
		}
		holder.imageButtonThreeDots.setVisibility(View.GONE);
		holder.checkbox.setVisibility(View.GONE);
		holder.optionsLayout.setVisibility(View.GONE);

//		holder.imageButtonThreeDots.setTag(holder);
//		holder.imageButtonThreeDots.setOnClickListener(this);

		if (positionClicked != -1) {
			if (positionClicked == position) {
				//				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				//				holder.imageButtonThreeDots.setImageResource(R.drawable.ic_three_dots);
				listFragment.smoothScrollToPosition(_position);

				// Choose the buttons to show depending on the type of
				// folder

				MegaNode n = (MegaNode) getItem(positionClicked);
				MegaNode folder = null;

				if (n.isFile())
					folder = megaApi.getParentNode(n);
				else
					folder = n;

				int accessLevel = megaApi.getAccess(folder);

				switch (accessLevel) {
					case MegaShare.ACCESS_FULL: {
	
						//						holder.optionDownload.setVisibility(View.VISIBLE);
						//						holder.optionProperties.setVisibility(View.VISIBLE);
						//						holder.optionPublicLink.setVisibility(View.GONE);
						//						holder.optionRename.setVisibility(View.VISIBLE);
						//						holder.optionDelete.setVisibility(View.VISIBLE);
						//
						//						holder.optionDownload.getLayoutParams().width = Util
						//								.px2dp((44 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionDownload
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((9 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						//						holder.optionProperties.getLayoutParams().width = Util
						//								.px2dp((44 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionProperties
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);						
						//						holder.optionRename.getLayoutParams().width = Util
						//								.px2dp((44 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionRename
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						//						holder.optionDelete.getLayoutParams().width = Util
						//								.px2dp((44 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionDelete
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						// holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_full_access));
						break;
					}
					case MegaShare.ACCESS_READ: {
						log("read");
						//						holder.optionDownload.setVisibility(View.VISIBLE);
						//						holder.optionProperties.setVisibility(View.VISIBLE);						
						//						holder.optionPublicLink.setVisibility(View.GONE);
						//						holder.optionRename.setVisibility(View.GONE);
						//						holder.optionDelete.setVisibility(View.GONE);
						//
						//						holder.optionDownload.getLayoutParams().width = Util
						//								.px2dp((100 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionDownload
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((9 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						//						holder.optionProperties.getLayoutParams().width = Util
						//								.px2dp((100 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionProperties
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						break;
					}
					case MegaShare.ACCESS_READWRITE: {
						log("readwrite");
						//						holder.optionDownload.setVisibility(View.VISIBLE);
						//						holder.optionProperties.setVisibility(View.VISIBLE);
						//						holder.optionPublicLink.setVisibility(View.GONE);
						//						holder.optionRename.setVisibility(View.VISIBLE);
						//						holder.optionDelete.setVisibility(View.GONE);
						//
						//						holder.optionDownload.getLayoutParams().width = Util
						//								.px2dp((70 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionDownload
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((9 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						//						holder.optionProperties.getLayoutParams().width = Util
						//								.px2dp((70 * scaleW), outMetrics);
						//						((TableRow.LayoutParams) holder.optionProperties
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
						//						((TableRow.LayoutParams) holder.optionRename
						//								.getLayoutParams()).setMargins(
						//								Util.px2dp((17 * scaleW), outMetrics),
						//								Util.px2dp((4 * scaleH), outMetrics), 0, 0);
	
	
					}
				}
			}
		}
			
		return convertView;
	}
				

	private String getInfoFolder(MegaNode n) {
		NodeList nL = megaApi.getChildren(n);

		int numFolders = 0;
		int numFiles = 0;

		for (int i = 0; i < nL.size(); i++) {
			MegaNode c = nL.get(i);
			if (c.isFolder()) {
				numFolders++;
			} else {
				numFiles++;
			}
		}

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

	@Override
	public boolean isEnabled(int position) {
		// if (position == 0){
		// return false;
		// }
		// else{
		// return true;
		// }
		return super.isEnabled(position);
	}

	@Override
	public int getCount() {
		return megaShareInList.size();
	}

	@Override
	public Object getItem(int position) {
		return megaShareInList.get(position);
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
	}

	@Override
	public void onClick(View v) {
		ViewHolderInOutShareList holder = (ViewHolderInOutShareList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaNode n = (MegaNode) getItem(currentPosition);

		switch (v.getId()) {
		case R.id.file_list_option_download: {
			positionClicked = -1;
			notifyDataSetChanged();
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(n.getHandle());
			if (type == ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ContactPropertiesMainActivity) context).onFileClick(handleList);
			} else if (type == ManagerActivity.FOLDER_LINK_ADAPTER) {
				((FolderLinkActivity) context).onFileClick(handleList);
			} else {
				((ManagerActivity) context).onFileClick(handleList);
			}
			break;
		}
		case R.id.file_list_option_properties: {
			Intent i = new Intent(context, FilePropertiesActivity.class);
			i.putExtra("handle", n.getHandle());

			if (n.isFolder()) {
				ShareList sl = megaApi.getOutShares(n);

				if (sl != null) {

					if (sl.size() > 0) {

						i.putExtra("imageId", R.drawable.mime_folder_shared);
					} else {
						i.putExtra("imageId", R.drawable.mime_folder);
					}
				} else {
					i.putExtra("imageId", R.drawable.mime_folder);
				}

			} else {
				i.putExtra("imageId", MimeType.typeForName(n.getName())
						.getIconResourceId());
			}
			i.putExtra("name", n.getName());
			context.startActivity(i);
			positionClicked = -1;
			notifyDataSetChanged();
			break;
		}
		case R.id.file_list_option_delete: {
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(n.getHandle());
			setPositionClicked(-1);
			notifyDataSetChanged();
			if (type != ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ManagerActivity) context).moveToTrash(handleList);
			} else {
				((ContactPropertiesMainActivity) context).moveToTrash(handleList);
			}
			break;
		}
		case R.id.file_list_option_public_link: {
			setPositionClicked(-1);
			notifyDataSetChanged();
			if ((type == ManagerActivity.FILE_BROWSER_ADAPTER)
					|| (type == ManagerActivity.SEARCH_ADAPTER)) {
				((ManagerActivity) context).getPublicLinkAndShareIt(n);
			}
			break;
		}
		case R.id.file_list_option_rename: {
			setPositionClicked(-1);
			notifyDataSetChanged();
			if (type != ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ManagerActivity) context).showRenameDialog(n, n.getName());
			}
			if (type == ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ContactPropertiesMainActivity) context).showRenameDialog(n,	n.getName());
			}

			break;
		}/*
		case R.id.file_list_option_move: {
			setPositionClicked(-1);
			notifyDataSetChanged();
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(n.getHandle());
			if (type != ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ManagerActivity) context).showMove(handleList);
			} else {
				((ContactPropertiesMainActivity) context).showMove(handleList);
			}
			break;
		}
		case R.id.file_list_option_copy: {
			positionClicked = -1;
			notifyDataSetChanged();
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(n.getHandle());
			if (type != ManagerActivity.CONTACT_FILE_ADAPTER) {
				((ManagerActivity) context).showCopy(handleList);
			} else {
				((ContactPropertiesMainActivity) context).showCopy(handleList);
			}
			break;
		}*/
		case R.id.file_list_three_dots: {
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
			break;
		}
		}
	}

	/*
	 * Get document at specified position
	 */
	public MegaNode getNodeAt(int position) {
		try {
			if (megaShareInList != null) {
				return megaShareInList.get(position).getNode();
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
		((ManagerActivity) context).setParentHandleSharedWithMe(parentHandle);
		
	}
	
	public boolean isMultipleSelect() {
//		return multipleSelect;
		return false;
	}

	public void setMultipleSelect(boolean multipleSelect) {
//		if (this.multipleSelect != multipleSelect) {
//			this.multipleSelect = multipleSelect;
//			notifyDataSetChanged();
//		}
	}

	public void setOrder(int orderGetChildren) {
		this.orderGetChildren = orderGetChildren;
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
    	
    	if (type == ManagerActivity.SHARED_WITH_ME_ADAPTER){
    		notifyDataSetChanged();
    	}
    }   
    

	private static void log(String log) {
		Util.log("MegaShareInOutAdapter", log);
	}
}
