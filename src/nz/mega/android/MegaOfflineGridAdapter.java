package nz.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;


public class MegaOfflineGridAdapter extends BaseAdapter implements OnClickListener {
	
	OfflineFragment fragment;
	Context context;
	int positionClicked;

	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	public DatabaseHandler dbH;
	String pathNavigation = null;
	ArrayList<MegaOffline> mOffList = new ArrayList<MegaOffline>();	
		
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	boolean multipleSelect = false;
	
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
			String numHolder = params[1];
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
				long handle = -1;
				thumb = Util.rotateBitmap(thumb, orientation);
				if(numHolder.equals("1")){
					log("Entro opor el 1: "+numHolder);
					handle = Long.parseLong(holder.currentHandle1);
				}
				else{
					log("Entro opor el 2: "+numHolder);
					handle = Long.parseLong(holder.currentHandle2);
				}
				
				//long handle = Long.parseLong(currentFile.getParentFile().getName());
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
	
		
	public MegaOfflineGridAdapter(OfflineFragment _fragment, Context _context, ArrayList<MegaOffline> _mOffList, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
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
		this.mOffList = mOffList;
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
//        ImageView arrowSelection1;
        RelativeLayout optionsLayout1;
        ImageButton optionOpen1;
        ImageView optionDelete1;
//        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
        ImageButton optionOpen2;
        ImageView optionDelete2;
        int currentPosition;
        String currentPath1;
        String currentPath2;
        String currentHandle1;
        String currentHandle2;
    }
    
    ViewHolderOfflineGrid holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("MegaOfflineGridAdapter:getView");
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
			paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);			
			paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);

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
//			holder.arrowSelection1 = (ImageView) v.findViewById(R.id.offline_grid_arrow_selection1);
//			holder.arrowSelection1.setVisibility(View.GONE);

			holder.optionsLayout2 = (RelativeLayout) v.findViewById(R.id.offline_grid_options2);
			holder.optionOpen2 = (ImageButton) v.findViewById(R.id.offline_grid_option_open2);
			holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDelete2 = (ImageView) v.findViewById(R.id.offline_grid_option_delete2);
			holder.optionDelete2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//			holder.arrowSelection2 = (ImageView) v.findViewById(R.id.offline_grid_arrow_selection2);
//			holder.arrowSelection2.setVisibility(View.GONE);

			holder.currentPosition = position;

////////////////////// Node 1			
			
			MegaOffline currentNode1 = (MegaOffline) getItem(position);
			
			File currentFile1 = null;
			if (Environment.getExternalStorageDirectory() != null){
				currentFile1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode1.getPath()+currentNode1.getName());
			}
			else{
				currentFile1 = context.getFilesDir();
			}
			
			holder.currentPath1 = currentFile1.getAbsolutePath();
			holder.currentHandle1 = currentNode1.getHandle();
			holder.currentPosition = position;
			
			holder.textViewFileName1.setText(currentNode1.getName());
			
			int folders=0;
			int files=0;
			if (currentFile1.isDirectory()){
				
				File[] fList = currentFile1.listFiles();
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
						
				holder.textViewFileSize1.setText(info);			
			}
			else{
				long nodeSize = currentFile1.length();
				holder.textViewFileSize1.setText(Util.getSizeString(nodeSize));		
			}
						
			holder.imageView1.setImageResource(MimeTypeThumbnail.typeForName(currentNode1.getName()).getIconResourceId());
			if (currentFile1.isFile()){
				if (MimeTypeThumbnail.typeForName(currentNode1.getName()).isImage()){
					Bitmap thumb = null;
									
					if (currentFile1.exists()){
						thumb = ThumbnailUtils.getThumbnailFromCache(Long.parseLong(currentNode1.getHandle()));
						if (thumb != null){
							holder.imageView1.setImageBitmap(thumb);
						}
						else{
							try{
								new OfflineThumbnailAsyncTask(holder,0).execute(new String[] { currentFile1.getAbsolutePath(),"1" });
							}
							catch(Exception e){
								//Too many AsyncTasks
							}
						}
					}
				}
			}
			else{
				holder.imageView1.setImageResource(R.drawable.folder_thumbnail);
			}	
			
////////////////////////////////Node 2		
			
			MegaOffline currentNode2 = null;
			if (position < (getCount()-1)){
				
				currentNode2 = (MegaOffline) getItem(position+1);
				
				File currentFile2 = null;
				if (Environment.getExternalStorageDirectory() != null){
					currentFile2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode2.getPath()+currentNode2.getName());
				}
				else{
					currentFile2 = context.getFilesDir();
				}
				
				holder.currentPath2 = currentFile2.getAbsolutePath();
				holder.currentHandle2 = currentNode2.getHandle();
				holder.currentPosition = position;
				
				holder.textViewFileName2.setText(currentNode2.getName());				
				
				folders=0;
				files=0;
				if (currentFile2.isDirectory()){
					
					File[] fList = currentFile2.listFiles();
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
							
					holder.textViewFileSize2.setText(info);			
				}
				else{
					long nodeSize = currentFile2.length();
					holder.textViewFileSize2.setText(Util.getSizeString(nodeSize));
				}				
				
				holder.imageView2.setImageResource(MimeTypeThumbnail.typeForName(currentNode2.getName()).getIconResourceId());
				if (currentFile2.isFile()){
					if (MimeTypeThumbnail.typeForName(currentNode2.getName()).isImage()){
						Bitmap thumb = null;
										
						if (currentFile2.exists()){
							thumb = ThumbnailUtils.getThumbnailFromCache(Long.parseLong(currentNode2.getHandle()));
							if (thumb != null){
								holder.imageView2.setImageBitmap(thumb);
							}
							else{
								try{
									new OfflineThumbnailAsyncTask(holder,1).execute(new String[] { currentFile2.getAbsolutePath(),"2" });
								}
								catch(Exception e){
									//Too many AsyncTasks
								}
							}
						}
					}
				}
				else{
					holder.imageView2.setImageResource(R.drawable.folder_thumbnail);
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
//					holder.arrowSelection1.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout1.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					
					holder.optionOpen1.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionOpen1.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.optionProperties.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
//					((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionDelete1.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionDelete1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
				else if (positionClicked == (position+1)){
//					holder.arrowSelection2.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout2.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					holder.optionOpen2.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionOpen2.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.optionProperties.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
//					((TableRow.LayoutParams) holder.optionProperties.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
					holder.optionDelete2.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
					((TableRow.LayoutParams) holder.optionDelete2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
//					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
				}
				else{
//					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
					
//					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
			}
			else{
//				holder.arrowSelection1.setVisibility(View.GONE);
				LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
				params1.height = 0;
				
//				holder.arrowSelection2.setVisibility(View.GONE);
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
        return mOffList.size();
    }
 
    @Override
    public Object getItem(int position) {
        return mOffList.get(position);
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
		dbH = DatabaseHandler.getDbHandler(context);
				
		switch (v.getId()){
			case R.id.offline_grid_thumbnail1:{
				
				log("Nooo en la uno");
				MegaOffline mOff = (MegaOffline) getItem(currentPosition);
				String currentPath = mOff.getPath()+mOff.getName(); 
				File currentFile = new File(currentPath);		
				
				MegaOffline currentNode = mOffList.get(currentPosition);
				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";				
				((ManagerActivity)context).setPathNavigationOffline(pathNavigation);
				
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
				
				if(currentFile.exists()&&currentFile.isDirectory()){
										
					mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
					
					if (this.getCount() == 0){
						log("thum1");
						listFragment.setVisibility(View.GONE);
						emptyImageViewFragment.setVisibility(View.VISIBLE);
						emptyTextViewFragment.setVisibility(View.VISIBLE);						
					}
					else{
						for(int i=0; i<mOffList.size();i++){

							File offlineDirectory = null;
							if (Environment.getExternalStorageDirectory() != null){
								offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
							}
							else{
								offlineDirectory = context.getFilesDir();
							}	

							if (!offlineDirectory.exists()){
								//Updating the DB because the file does not exist														
								dbH.removeById(mOffList.get(i).getId());

								mOffList.remove(i);
							}			
						}
					}


					this.setNodes(mOffList);


					this.setPositionClicked(-1);
					//this.setMultipleSelect(false);
					notifyDataSetChanged();

				}else{
					//if(currentFile.exists()&&currentFile.isFile()){
						log("Open it!");
						if (MimeTypeThumbnail.typeForName(currentFile.getName()).isImage()){
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", currentPosition);
							intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
							intent.putExtra("parentNodeHandle", -1L);
							intent.putExtra("offlinePathDirectory", currentFile.getParent());
							context.startActivity(intent);
						}
						else{
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
							if (ManagerActivity.isIntentAvailable(context, viewIntent)){
								context.startActivity(viewIntent);
							}
							else{
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
								if (ManagerActivity.isIntentAvailable(context, intentShare)){
									context.startActivity(intentShare);
								}
							}
					//	}
						
					}
					
				}
				
				
				
				
				
				
//				if (MimeType.typeForName(currentFile.getName()).isImage()){
//					Intent intent = new Intent(context, FullScreenImageViewer.class);
//					intent.putExtra("position", currentPosition);
//					intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
//					intent.putExtra("parentNodeHandle", -1L);
//					context.startActivity(intent);
//				}
//				else{
//					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//					if (ManagerActivity.isIntentAvailable(context, viewIntent)){
//						context.startActivity(viewIntent);
//					}
//					else{
//						Intent intentShare = new Intent(Intent.ACTION_SEND);
//						intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//						if (ManagerActivity.isIntentAvailable(context, intentShare)){
//							context.startActivity(intentShare);
//						}
//					}
//				}
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.offline_grid_thumbnail2:{
				
				
				log("Entroooo");
				MegaOffline mOff = (MegaOffline) getItem(currentPosition+1);
				String currentPath = mOff.getPath()+mOff.getName(); 
				File currentFile = new File(currentPath);		
				
				MegaOffline currentNode = mOffList.get(currentPosition);
				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";				
				((ManagerActivity)context).setPathNavigationOffline(pathNavigation);
				
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
				
				if(currentFile.exists()&&currentNode.getType()==DB_FOLDER){
					
					log("Y por aqui?");

					mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
					if (this.getCount() == 0){
						
						listFragment.setVisibility(View.GONE);
						emptyImageViewFragment.setVisibility(View.VISIBLE);
						emptyTextViewFragment.setVisibility(View.VISIBLE);						
					}
					else{
						for(int i=0; i<mOffList.size();i++){

							File offlineDirectory = null;
							if (Environment.getExternalStorageDirectory() != null){
								offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
							}
							else{
								offlineDirectory = context.getFilesDir();
							}	

							if (!offlineDirectory.exists()){
								//Updating the DB because the file does not exist														
								dbH.removeById(mOffList.get(i).getId());

								mOffList.remove(i);
							}			
						}
					}

					this.setNodes(mOffList);


					this.setPositionClicked(-1);
					//this.setMultipleSelect(false);
					notifyDataSetChanged();

				}else{
					//if(currentFile.exists()&&currentFile.isFile()){
						log("Open it!");
						if (MimeTypeThumbnail.typeForName(currentFile.getName()).isImage()){
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", currentPosition);
							intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
							intent.putExtra("parentNodeHandle", -1L);
							intent.putExtra("offlinePathDirectory", currentFile.getParent());
							context.startActivity(intent);
						}
						else{
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
							if (ManagerActivity.isIntentAvailable(context, viewIntent)){
								context.startActivity(viewIntent);
							}
							else{
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
								if (ManagerActivity.isIntentAvailable(context, intentShare)){
									context.startActivity(intentShare);
								}
							}
						}
						
					//}
					
				}
				
				
				
				
//				MegaOffline mOff = (MegaOffline) getItem(currentPosition+1);
//				String currentPath = mOff.getPath()+mOff.getName(); 
//				File currentFile = new File(currentPath);
//				
//				if (MimeType.typeForName(currentFile.getName()).isImage()){
//					Intent intent = new Intent(context, FullScreenImageViewer.class);
//					intent.putExtra("position", currentPosition+1);
//					intent.putExtra("adapterType", ManagerActivity.OFFLINE_ADAPTER);
//					intent.putExtra("parentNodeHandle", -1L);
//					context.startActivity(intent);
//				}
//				else{
//					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//					if (ManagerActivity.isIntentAvailable(context, viewIntent)){
//						context.startActivity(viewIntent);
//					}
//					else{
//						Intent intentShare = new Intent(Intent.ACTION_SEND);
//						intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//						if (ManagerActivity.isIntentAvailable(context, intentShare)){
//							context.startActivity(intentShare);
//						}
//					}
//				}
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.offline_grid_option_open1:{
				File currentFile = new File((String)getItem(currentPosition));
				positionClicked = -1;
				notifyDataSetChanged();
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
				if (ManagerActivity.isIntentAvailable(context, viewIntent)){
					context.startActivity(viewIntent);
				}
				else{
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
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
				viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
				if (ManagerActivity.isIntentAvailable(context, viewIntent)){
					context.startActivity(viewIntent);
				}
				else{
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeThumbnail.typeForName(currentFile.getName()).getType());
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
				MegaOffline mOff = (MegaOffline) getItem(currentPosition);
				
				setPositionClicked(-1);
				notifyDataSetChanged();
				
				deleteOffline(context, mOff);
				
				fragment.refreshPaths();
				break;
			}
			case R.id.offline_grid_option_delete2:{
				MegaOffline mOff = (MegaOffline) getItem(currentPosition+1);
				setPositionClicked(-1);
				notifyDataSetChanged();
				
				deleteOffline(context, mOff);
				
				fragment.refreshPaths();
				break;
			}
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
	
	public boolean isMultipleSelect(){
		return multipleSelect;
	}
	
	private static void log(String log) {
		Util.log("MegaOfflineGridAdapter", log);
	}
}
