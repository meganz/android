package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MegaOfflineGridAdapter extends BaseAdapter implements OnClickListener {
	
	OfflineFragment fragment;
	Context context;
	
	ArrayList<String> paths = new ArrayList<String>();	
	int positionClicked;
		
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	private class OfflineThumbnailAsyncTask extends AsyncTask<String, Void, Bitmap>{

		ViewHolderOfflineGrid holder;
		String currentPath;
    	int cell = 0;
    	
    	public OfflineThumbnailAsyncTask(ViewHolderOfflineGrid holder, int cell) {
			this.holder = holder;
			this.cell = cell;
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
				long handle = Long.parseLong(currentFile.getParentFile().getName());
				ThumbnailUtils.setThumbnailCache(handle, thumb);
				return thumb;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap thumb){
			if (thumb != null){
				if (cell == 0){
					if (holder.currentPath1.equals(currentPath)){
						holder.imageView1.setImageBitmap(thumb);
						Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
						holder.imageView1.startAnimation(fadeInAnimation);
					}
				}
				else if (cell == 1){
					if (holder.currentPath2.equals(currentPath)){
						holder.imageView2.setImageBitmap(thumb);
						Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
						holder.imageView2.startAnimation(fadeInAnimation);
					}
				}
			}
		}    	
    }
		
	public MegaOfflineGridAdapter(OfflineFragment _fragment, Context _context, ArrayList<String> _paths, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
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
	
	/*private view holder class*/
    public class ViewHolderOfflineGrid {
        ImageButton imageView1;
        TextView textViewFileName1;
        RelativeLayout itemLayout1;
        ImageButton imageView2;
        TextView textViewFileName2;
        RelativeLayout itemLayout2;
        TextView textViewFileSize1;
        TextView textViewFileSize2;
        ImageButton imageButtonThreeDots1;
        ImageButton imageButtonThreeDots2;
        ImageView arrowSelection1;
        RelativeLayout optionsLayout1;
        ImageButton optionOpen1;
        ImageView optionDelete1;
        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
        ImageButton optionOpen2;
        ImageView optionDelete2;
        int currentPosition;
        String currentPath1;
        String currentPath2;
    }
    
    ViewHolderOfflineGrid holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View v;
		
		listFragment = (ListView) parent;
		final int _position = position;
		positionG = position;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if ((position % 2) == 0){
			v = inflater.inflate(R.layout.item_offline_grid, parent, false);
			holder = new ViewHolderOfflineGrid();
			holder.itemLayout1 = (RelativeLayout) v.findViewById(R.id.offline_grid_item_layout1);
			holder.itemLayout2 = (RelativeLayout) v.findViewById(R.id.offline_grid_item_layout2);
			
			//Set width and height itemLayout1
			RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			holder.itemLayout1.setLayoutParams(paramsIL1);
			
			//Set width and height itemLayout2
			RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.offline_grid_item_layout1);
			paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.offline_grid_separator_final);
			paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
	        anim.setDuration(0);
	        
	        holder.itemLayout2.startAnimation(anim);
			holder.itemLayout2.setLayoutParams(paramsIL2);
			
			holder.imageView1 = (ImageButton) v.findViewById(R.id.offline_grid_thumbnail1);
            holder.imageView2 = (ImageButton) v.findViewById(R.id.offline_grid_thumbnail2);
            
            
			RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			holder.imageView1.setLayoutParams(paramsIV1);
			
			RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			holder.imageView2.setLayoutParams(paramsIV2);

			holder.textViewFileName1 = (TextView) v.findViewById(R.id.offline_grid_filename1);
			holder.textViewFileName1.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName1.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
			holder.textViewFileName1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			holder.textViewFileName1.setSingleLine(true);
			holder.textViewFileName2 = (TextView) v.findViewById(R.id.offline_grid_filename2);
			holder.textViewFileName2.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName2.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
			holder.textViewFileName2.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			holder.textViewFileName2.setSingleLine(true);
			
			holder.textViewFileSize1 = (TextView) v.findViewById(R.id.offline_grid_filesize1);
			holder.textViewFileSize2 = (TextView) v.findViewById(R.id.offline_grid_filesize2);
			
			holder.imageButtonThreeDots1 = (ImageButton) v.findViewById(R.id.offline_grid_three_dots1);
			holder.imageButtonThreeDots2 = (ImageButton) v.findViewById(R.id.offline_grid_three_dots2);
			
			holder.optionsLayout1 = (RelativeLayout) v.findViewById(R.id.offline_grid_options1);
			holder.optionOpen1 = (ImageButton) v.findViewById(R.id.offline_grid_option_open1);
			holder.optionOpen1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDelete1 = (ImageView) v.findViewById(R.id.offline_grid_option_delete1);
			holder.optionDelete1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection1 = (ImageView) v.findViewById(R.id.offline_grid_arrow_selection1);
			holder.arrowSelection1.setVisibility(View.GONE);

			holder.optionsLayout2 = (RelativeLayout) v.findViewById(R.id.offline_grid_options2);
			holder.optionOpen2 = (ImageButton) v.findViewById(R.id.offline_grid_option_open2);
			holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDelete2 = (ImageView) v.findViewById(R.id.offline_grid_option_delete2);
			holder.optionDelete2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection2 = (ImageView) v.findViewById(R.id.offline_grid_arrow_selection2);
			holder.arrowSelection2.setVisibility(View.GONE);

			holder.currentPosition = position;

			String currentPath1 = (String) getItem(position);
			File currentFile1 = new File(currentPath1);
			
			holder.currentPath1 = currentPath1;
			
			long fileSize1 = currentFile1.length();
			holder.textViewFileName1.setText(currentFile1.getName());
			holder.textViewFileSize1.setText(Util.getSizeString(fileSize1));
			holder.imageView1.setImageResource(MimeType.typeForName(currentFile1.getName()).getIconResourceId());
			
			if (MimeType.typeForName(currentFile1.getName()).isImage()){
				Bitmap thumb1 = null;
				long handle = Long.parseLong(currentFile1.getParentFile().getName());
				
				thumb1 = ThumbnailUtils.getThumbnailFromCache(handle);
				if (thumb1 != null){
					holder.imageView1.setImageBitmap(thumb1);
				}
				else{
					try{
						new OfflineThumbnailAsyncTask(holder, 0).execute(currentFile1.getAbsolutePath());
					}
					catch(Exception e){
						//Too many AsyncTasks
					}		
				}
			}
			
			String currentPath2 = "";
			if (position < (getCount()-1)){
				currentPath2 = (String) getItem(position+1);
				File currentFile2 = new File(currentPath2);
				
				holder.currentPath2 = currentPath2;
				
				long fileSize2 = currentFile2.length();
				holder.textViewFileName2.setText(currentFile2.getName());
				holder.textViewFileSize2.setText(Util.getSizeString(fileSize2));
				holder.imageView2.setImageResource(MimeType.typeForName(currentFile2.getName()).getIconResourceId());
				
				if (MimeType.typeForName(currentFile2.getName()).isImage()){
					
					Bitmap thumb2 = null;
					long handle = Long.parseLong(currentFile2.getParentFile().getName());
					
					thumb2 = ThumbnailUtils.getThumbnailFromCache(handle);
					if (thumb2 != null){
						holder.imageView2.setImageBitmap(thumb2);
					}
					else{
						try{
							new OfflineThumbnailAsyncTask(holder, 1).execute(currentFile2.getAbsolutePath());
						}
						catch(Exception e){
							//Too many AsyncTasks
						}			
					}
				}
				
				holder.itemLayout2.setVisibility(View.VISIBLE);				
			}
			else{
				holder.itemLayout2.setVisibility(View.GONE);
			}
			
			holder.imageView1.setTag(holder);
			holder.imageView1.setOnClickListener(this);
			
			holder.imageView2.setTag(holder);
			holder.imageView2.setOnClickListener(this);
			
			holder.imageButtonThreeDots1.setTag(holder);
			holder.imageButtonThreeDots1.setOnClickListener(this);
			
			holder.imageButtonThreeDots2.setTag(holder);
			holder.imageButtonThreeDots2.setOnClickListener(this);
			
			if (positionClicked != -1){
				if (positionClicked == position){
					holder.arrowSelection1.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout1.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					
					holder.optionOpen1.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionOpen1.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.optionProperties.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
//					((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionDelete1.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionDelete1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
				else if (positionClicked == (position+1)){
					holder.arrowSelection2.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout2.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					holder.optionOpen2.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionOpen2.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.optionProperties.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
//					((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionDelete2.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionDelete2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
				}
				else{
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
					
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
			}
			else{
				holder.arrowSelection1.setVisibility(View.GONE);
				LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
				params1.height = 0;
				
				holder.arrowSelection2.setVisibility(View.GONE);
				LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
				params2.height = 0;
			}
			
			holder.optionOpen1.setTag(holder);
			holder.optionOpen1.setOnClickListener(this);
			
			holder.optionOpen2.setTag(holder);
			holder.optionOpen2.setOnClickListener(this);
			
			holder.optionDelete1.setTag(holder);
			holder.optionDelete1.setOnClickListener(this);
			
			holder.optionDelete2.setTag(holder);
			holder.optionDelete2.setOnClickListener(this);
		}
		else{
			v = inflater.inflate(R.layout.item_file_empty_grid, parent, false);
		}
		
		return v;
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

		ViewHolderOfflineGrid holder = (ViewHolderOfflineGrid) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch (v.getId()){
			case R.id.offline_grid_thumbnail1:{
				File currentFile = new File((String)getItem(currentPosition));
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
			case R.id.offline_grid_thumbnail2:{
				
				File currentFile = new File((String)getItem(currentPosition+1));
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
			case R.id.offline_grid_option_open1:{
				File currentFile = new File((String)getItem(currentPosition));
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
			case R.id.offline_grid_option_open2:{
				
				File currentFile = new File((String)getItem(currentPosition+1));
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
			case R.id.offline_grid_three_dots1:{
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
			case R.id.offline_grid_three_dots2:{
				if (positionClicked == -1){
					positionClicked = currentPosition+1;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == (currentPosition+1)){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition+1;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.offline_grid_option_delete1:{
				File currentFile = new File((String)getItem(currentPosition));
				setPositionClicked(-1);
				notifyDataSetChanged();
				
				try{
					Util.deleteFolderAndSubfolders(context, currentFile.getParentFile());
				}
				catch(Exception e){};
				
				fragment.refreshPaths();
				break;
			}
			case R.id.offline_grid_option_delete2:{
				File currentFile = new File((String)getItem(currentPosition+1));
				setPositionClicked(-1);
				notifyDataSetChanged();
				
				try{
					Util.deleteFolderAndSubfolders(context, currentFile.getParentFile());
				}
				catch(Exception e){};
				
				fragment.refreshPaths();
				break;
			}
		}
	}
	
	private static void log(String log) {
		Util.log("MegaOfflineGridAdapter", log);
	}
}
