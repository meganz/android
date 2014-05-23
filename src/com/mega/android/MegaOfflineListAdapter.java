package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MegaOfflineListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;

	int positionClicked;

	ArrayList<String> paths = new ArrayList<String>();	
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	OfflineFragment fragment;
	
	boolean multipleSelect;
	
	/*public static view holder class*/
    public class ViewHolderOfflineList {
    	CheckBox checkbox;
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageView optionOpen;
//        ImageView optionProperties;
        ImageView optionDelete;
        int currentPosition;
        String currentPath;
    }
    
    private class OfflineThumbnailAsyncTask extends AsyncTask<String, Void, Bitmap>{

    	ViewHolderOfflineList holder;
    	String currentPath;
    	
    	public OfflineThumbnailAsyncTask(ViewHolderOfflineList holder) {
			this.holder = holder;
		}
    	
		@Override
		protected Bitmap doInBackground(String... params) {

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
				String [] s = currentFile.getName().split("_");
				long handle = Long.parseLong(s[0]);
//				long handle = Long.parseLong(currentFile.getParentFile().getName());
				ThumbnailUtils.setThumbnailCache(handle, thumb);
				return thumb;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap thumb){
			if (thumb != null){
				if (holder.currentPath.equals(currentPath)){
					holder.imageView.setImageBitmap(thumb);
					Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
					holder.imageView.startAnimation(fadeInAnimation);
				}
			}
		}    	
    }
	
	public MegaOfflineListAdapter(OfflineFragment _fragment, Context _context, ArrayList<String> _paths, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
		this.fragment = _fragment;
		this.context = _context;
		this.paths = _paths;

		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
	}
	
	public void setPaths(ArrayList<String> paths){
		this.paths = paths;
		positionClicked = -1;	
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
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
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.offline_list_options);
			holder.optionOpen = (ImageView) convertView.findViewById(R.id.offline_list_option_open);
			holder.optionOpen.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionOpen.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.offline_list_option_delete);
			holder.optionDelete.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.offline_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);
			
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
		
		String currentPath = (String) getItem(position);
		File currentFile = new File(currentPath);
		
		holder.currentPath = currentPath;
		
		long fileSize = currentFile.length();
		holder.textViewFileSize.setText(Util.getSizeString(fileSize));
		holder.imageView.setImageResource(MimeType.typeForName(currentFile.getName()).getIconResourceId());
		
		if (MimeType.typeForName(currentFile.getName()).isImage()){
			Bitmap thumb = null;
			String [] s = currentFile.getName().split("_");
			if (s.length > 0){
				long handle = Long.parseLong(s[0]);
				
				String fileName = "";
				for (int i=1;i<s.length-1;i++){
					fileName += s[i] + "_";
				}
				fileName += s[s.length-1];
				holder.textViewFileName.setText(fileName);
				
				thumb = ThumbnailUtils.getThumbnailFromCache(handle);
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
				
				holder.optionOpen.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionOpen.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
				holder.optionDelete.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
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
		
		holder.optionOpen.setTag(holder);
		holder.optionOpen.setOnClickListener(this);
		
//		holder.optionProperties.setTag(holder);
//		holder.optionProperties.setOnClickListener(this);
		
		holder.optionDelete.setTag(holder);
		holder.optionDelete.setOnClickListener(this);
		
		return convertView;
	}

	@Override
	public boolean isEnabled(int position) {
		return super.isEnabled(position);
	}

	@Override
    public int getCount() {
        return paths.size();
    }
 
    @Override
    public Object getItem(int position) {
        return paths.get(position);
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
		ViewHolderOfflineList holder = (ViewHolderOfflineList) v.getTag();
		int currentPosition = holder.currentPosition;
		String currentPath = (String) getItem(currentPosition);
		File currentFile = new File(currentPath);
		
		switch (v.getId()){
			case R.id.offline_list_option_open:{
				positionClicked = -1;
				notifyDataSetChanged();
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
				if (ManagerActivity.isIntentAvailable(context, viewIntent)){
					context.startActivity(viewIntent);
				}
				else{
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
					if (ManagerActivity.isIntentAvailable(context, intentShare)){
						context.startActivity(intentShare);
					}
				}
				break;
			}
//			case R.id.offline_list_option_properties:{
//				Intent i = new Intent(context, FilePropertiesActivity.class);
//				i.putExtra("handle", n.getHandle());
//			
//				if (n.isFolder()){
//					i.putExtra("imageId", R.drawable.mime_folder);
//				}
//				else{
//					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
//				}				
//				i.putExtra("name", n.getName());
//				context.startActivity(i);							
//				positionClicked = -1;
//				notifyDataSetChanged();
//				break;
//			}
			case R.id.offline_list_option_delete:{
				setPositionClicked(-1);
				notifyDataSetChanged();
				
				try{
					Util.deleteFolderAndSubfolders(context, currentFile.getParentFile());
				}
				catch(Exception e){};
				
				fragment.refreshPaths();
				break;
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
	
	/*
	 * Get path at specified position
	 */
	public String getPathAt(int position) {
		try {
			if(paths != null){
				return paths.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
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
		Util.log("MegaOfflineListAdapter", log);
	}
}
