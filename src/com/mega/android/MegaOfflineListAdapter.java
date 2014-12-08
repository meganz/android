package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mega.android.utils.ThumbnailUtils;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;

public class MegaOfflineListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
 
	int positionClicked;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	public DatabaseHandler dbH;

	ArrayList<MegaOffline> mOffList = new ArrayList<MegaOffline>();	
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	OfflineFragment fragment;
	//ArrayList<MegaOffline> mOffList;
	
	boolean multipleSelect;
	
	/*public static view holder class*/
    public class ViewHolderOfflineList {
    	CheckBox checkbox;
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;

        LinearLayout optionsLayout;
        ImageView optionDownload;
		ImageView optionProperties;
		ImageView optionMore;		
		ImageView optionPublicLink;
        ImageView optionDelete;
        
        LinearLayout optionsLayoutOffline;
        ImageView optionDeleteOffline;
        TextView offlineText;
        
        int currentPosition;
        String currentPath;
        String currentHandle;
    }
    
    private class OfflineThumbnailAsyncTask extends AsyncTask<String, Void, Bitmap>{

    	ViewHolderOfflineList holder;
    	String currentPath;
    	
    	public OfflineThumbnailAsyncTask(ViewHolderOfflineList holder) {
    		log("OfflineThumbnailAsyncTask::OfflineThumbnailAsyncTask");
			this.holder = holder;
		}
    	
		@Override
		protected Bitmap doInBackground(String... params) {
			log("OfflineThumbnailAsyncTask::doInBackground");
			currentPath = params[0];
			File currentFile = new File(currentPath);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			
			ExifInterface exif;
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			try {
				exif = new ExifInterface(currentFile.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			} catch (IOException e) {}  
			
			// Calculate inSampleSize
		    options.inSampleSize = Util.calculateInSampleSize(options, 270, 270);
		    
		    // Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			if (thumb != null){
				thumb = Util.rotateBitmap(thumb, orientation);
				long handle = Long.parseLong(holder.currentHandle);
				ThumbnailUtils.setThumbnailCache(handle, thumb);
				return thumb;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap thumb){
			log("OfflineThumbnailAsyncTask::onPostExecute");
			if (thumb != null){
				if (holder.currentPath.compareTo(currentPath) == 0){
					holder.imageView.setImageBitmap(thumb);
					Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
					holder.imageView.startAnimation(fadeInAnimation);
				}
			}
		}    	
    }
	
	public MegaOfflineListAdapter(OfflineFragment _fragment, Context _context, ArrayList<MegaOffline> _mOffList, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
		log("MegaOfflineListAdapter");
		this.fragment = _fragment;
		this.context = _context;
		this.mOffList = _mOffList;

		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
	}
	
	public void setNodes(ArrayList<MegaOffline> mOffList){
		log("setNodes");
		this.mOffList = mOffList;
		positionClicked = -1;	
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("getView");
		View v;		
	
		listFragment = (ListView) parent;
		
		final int _position = position;
		
		ViewHolderOfflineList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
		
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_offline_list, parent, false);
			holder = new ViewHolderOfflineList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.offline_list_item_layout);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.offline_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.imageView = (ImageView) convertView.findViewById(R.id.offline_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.offline_list_filename);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.offline_list_filesize);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.offline_list_three_dots);
			
			
			holder.optionsLayout = (LinearLayout) convertView.findViewById(R.id.offline_list_options);			
			holder.optionDownload = (ImageView) convertView.findViewById(R.id.offline_list_option_download);
			holder.optionProperties = (ImageView) convertView.findViewById(R.id.offline_list_option_properties);
			holder.optionPublicLink = (ImageView) convertView.findViewById(R.id.offline_list_option_public_link);
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.offline_list_option_delete);
			holder.optionMore = (ImageView) convertView.findViewById(R.id.offline_list_option_overflow);
			
	        holder.optionsLayoutOffline = (LinearLayout) convertView.findViewById(R.id.offline_list_options_no_connection);	
	        holder.optionDeleteOffline = (ImageView) convertView.findViewById(R.id.offline_list_option_delete_no_connection);
	        holder.offlineText = (TextView) convertView.findViewById(R.id.offline_list_option_no_connection);
		
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderOfflineList) convertView.getTag();
		}
		
		if (!multipleSelect){
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		}
		else{
			holder.checkbox.setVisibility(View.VISIBLE);
//			holder.arrowSelection.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			
			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				holder.checkbox.setChecked(true);
			}
			else{
				holder.checkbox.setChecked(false);
			}
		}
				
		MegaOffline currentNode = (MegaOffline) getItem(position);
		
		File currentFile = null;
		if (Environment.getExternalStorageDirectory() != null){
			currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath()+currentNode.getName());
		}
		else{
			currentFile = context.getFilesDir();
		}
		
		holder.currentPath = currentFile.getAbsolutePath();
		holder.currentHandle = currentNode.getHandle();
		holder.currentPosition = position;
		
		holder.textViewFileName.setText(currentNode.getName());
		
		int folders=0;
		int files=0;
		if (currentFile.isDirectory()){
			
			File[] fList = currentFile.listFiles();
			for (File f : fList){
				
				if (f.isDirectory()){
					folders++;						
				}
				else{
					files++;
				}
			}
			
			String info = "";
			if (folders > 0){
				info = folders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, folders);
				if (files > 0){
					info = info + ", " + files + " " + context.getResources().getQuantityString(R.plurals.general_num_files, folders);
				}
			}
			else {
				info = files +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, files);
			}			
					
			holder.textViewFileSize.setText(info);			
		}
		else{
			long nodeSize = currentFile.length();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
		}
		
		holder.imageView.setImageResource(MimeType.typeForName(currentNode.getName()).getIconResourceId());
		if (currentFile.isFile()){
			if (MimeType.typeForName(currentNode.getName()).isImage()){
				Bitmap thumb = null;
								
				if (currentFile.exists()){
					thumb = ThumbnailUtils.getThumbnailFromCache(Long.parseLong(currentNode.getHandle()));
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						try{
							new OfflineThumbnailAsyncTask(holder).execute(currentFile.getAbsolutePath());
						}
						catch(Exception e){
							//Too many AsyncTasks
						}
					}
				}
			}
		}
		else{
			holder.imageView.setImageResource(R.drawable.mime_folder);
		}
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
//				holder.arrowSelection.setVisibility(View.VISIBLE);
				
				if (Util.isOnline(context)){
					//With connection
					LayoutParams params = holder.optionsLayout.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
					listFragment.smoothScrollToPosition(_position);
					
				}
				else{
					//No connection
					LayoutParams params = holder.optionsLayoutOffline.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
					listFragment.smoothScrollToPosition(_position);
				}
				
			}
			else{
//				
				if (Util.isOnline(context)){
					//With connection
					LayoutParams params = holder.optionsLayout.getLayoutParams();
					params.height = 0;
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				}
				else{
					//No connection
					LayoutParams params = holder.optionsLayoutOffline.getLayoutParams();
					params.height = 0;
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				}	
			}
		}
		else{
			if (Util.isOnline(context)){
				//With connection
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
			else{
				//No connection
				LayoutParams params = holder.optionsLayoutOffline.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}	
		}
		
		holder.optionDownload.setTag(holder);
		holder.optionDownload.setOnClickListener(this);
		
		holder.optionMore.setTag(holder);
		holder.optionMore.setOnClickListener(this);
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		
		holder.optionPublicLink.setTag(holder);
		holder.optionPublicLink.setOnClickListener(this);
		
		holder.optionDelete.setTag(holder);
		holder.optionDelete.setOnClickListener(this);
		
		holder.optionDeleteOffline.setTag(holder);
		holder.optionDeleteOffline.setOnClickListener(this);
		
		if (Util.isOnline(context)){
			//With connection
			holder.optionsLayout.setVisibility(View.VISIBLE);
			holder.optionsLayoutOffline.setVisibility(View.GONE);
			
		}
		else{
			//No connection
			holder.optionsLayout.setVisibility(View.GONE);
			holder.optionsLayoutOffline.setVisibility(View.VISIBLE);
			holder.offlineText.setText("No connection");
		}
		return convertView;
	}

	@Override
	public boolean isEnabled(int position) {
		log("isEnabled");
		return super.isEnabled(position);
	}

	@Override
    public int getCount() {
		log("getCount");
		return mOffList.size();
    }
 
    @Override
    public Object getItem(int position) {
    	log("getItem");
        return mOffList.get(position);
    }
 
    @Override
    public long getItemId(int position) {
    	log("getItemId");
        return position;
    }    
    
    public int getPositionClicked (){
    	log("getPositionClicked");
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	log("setPositionClicked");
    	positionClicked = p;
    }

	@Override
	public void onClick(View v) {
		log("onClick");
		ViewHolderOfflineList holder = (ViewHolderOfflineList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaOffline mOff = (MegaOffline) getItem(currentPosition);
		
		switch (v.getId()){
			case R.id.offline_list_option_download:{
				positionClicked = -1;
				notifyDataSetChanged();				
				if (Util.isOnline(context)){
					String path = mOff.getPath() + mOff.getName();
					fragment.download(path);	
				}
				break;
			}
			case R.id.offline_list_option_properties:{
				
				positionClicked = -1;
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = mOff.getPath() + mOff.getName();
					fragment.showProperties(path);
				}

				break;
			}
			case R.id.offline_list_option_delete:{
				setPositionClicked(-1);
				notifyDataSetChanged();				
									
				deleteOffline(context, mOff);
								
				fragment.refreshPaths(mOff);
				
				break;
			}
			case R.id.offline_list_option_public_link:{
				setPositionClicked(-1);
				notifyDataSetChanged();	
				if (Util.isOnline(context)){
					String path = mOff.getPath() + mOff.getName();
					fragment.getLink(path);
				}					
				break;
			}
			case R.id.offline_list_option_delete_no_connection:{
				setPositionClicked(-1);
				notifyDataSetChanged();										
				deleteOffline(context, mOff);								
				fragment.refreshPaths(mOff);
				
				break;
			}
			case R.id.offline_list_option_overflow:{
				
				if (Util.isOnline(context)){
					String path = mOff.getPath() + mOff.getName();
					if(fragment.isFolder(path))
					{
						showOverflowFolder(mOff);
					}
					else{
						showOverflowFile(mOff);
					}
				}		
			}
			case R.id.offline_list_three_dots:{
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
	
	private void showOverflowFolder(MegaOffline mOff){
		final MegaOffline mOffFinal = mOff;
		AlertDialog moreOptionsDialog;
		
		final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_text, android.R.id.text1, new String[] {context.getString(R.string.context_share_folder), context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy)});
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("More options");
		builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case 0:{
						setPositionClicked(-1);
						notifyDataSetChanged();	
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.shareFolder(path);
						}
						
						break;
					}
					case 1:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.rename(path);
						}
						break;
					}
					case 2:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.move(path);
						}
						break;
					}
					case 3:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.copy(path);
						}
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
		Util.brandAlertDialog(moreOptionsDialog);
	}
	
	private void showOverflowFile(MegaOffline mOff){
		final MegaOffline mOffFinal = mOff;
		AlertDialog moreOptionsDialog;
		
		final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_text, android.R.id.text1, new String[] {context.getString(R.string.context_rename), context.getString(R.string.context_move), context.getString(R.string.context_copy)});
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("More options");
		builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case 0:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.rename(path);
						}
						break;
					}
					case 1:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.move(path);
						}
						break;
					}
					case 2:{
						setPositionClicked(-1);
						notifyDataSetChanged();
						if (Util.isOnline(context)){
							String path = mOffFinal.getPath() + mOffFinal.getName();
							fragment.copy(path);
						}
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
		Util.brandAlertDialog(moreOptionsDialog);
	}
	
	/*
	 * Get path at specified position
	 */
	public String getPathAt(int position) {
		log("getPathAt");
//		try {
//			if(paths != null){
//				return paths.get(position);
//			}
//		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
	
	public boolean isMultipleSelect() {
		log("isMultipleSelect");
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		log("setMultipleSelect");
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	private int deleteOffline(Context context,MegaOffline node){
		
		log("deleteOffline");

//		dbH = new DatabaseHandler(context);
		dbH = DatabaseHandler.getDbHandler(context);

		ArrayList<MegaOffline> mOffListParent=new ArrayList<MegaOffline>();
		ArrayList<MegaOffline> mOffListChildren=new ArrayList<MegaOffline>();			
		MegaOffline parentNode = null;	
		
		//Delete children
		mOffListChildren=dbH.findByParentId(node.getId());
		if(mOffListChildren.size()>0){
			//The node have childrens, delete
			deleteChildrenDB(mOffListChildren);			
		}
		
		int parentId = node.getParentId();
		log("Finding parents...");
		//Delete parents
		if(parentId!=-1){
			mOffListParent=dbH.findByParentId(parentId);
			
			log("Same Parent?:" +mOffListParent.size());
			
			if(mOffListParent.size()<1){
				//No more node with the same parent, keep deleting				

				parentNode = dbH.findById(parentId);
				log("Recursive parent: "+parentNode.getName());
				if(parentNode != null){
					deleteOffline(context, parentNode);	
						
				}	
			}			
		}	
		
		log("Remove the node physically");
		//Remove the node physically
		File destination = null;								

		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + node.getPath());
		}
		else{
			destination = context.getFilesDir();
		}	

		try{
			File offlineFile = new File(destination, node.getName());	
			log("Delete in phone: "+node.getName());
			Util.deleteFolderAndSubfolders(context, offlineFile);
		}
		catch(Exception e){
			log("EXCEPTION: deleteOffline - adapter");
		};		
		
		log("Delete in DB: "+node.getId());
		dbH.removeById(node.getId());		
		
		return 1;		
	}	

	private void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){
		
		log("deleteChildenDB: "+mOffListChildren.size());
		MegaOffline mOffDelete=null;
	
		for(int i=0; i<mOffListChildren.size(); i++){	
			
			mOffDelete=mOffListChildren.get(i);
			
			log("Children "+i+ ": "+ mOffDelete.getName());
			ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
			if(mOffListChildren2.size()>0){
				//The node have children, delete				
				deleteChildrenDB(mOffListChildren2);				
			}	
			
			int lines = dbH.removeById(mOffDelete.getId());		
			log("Borradas; "+lines);
		}		
	}	
	
	
	private static void log(String log) {
		Util.log("MegaOfflineListAdapter", log);
	}
}
