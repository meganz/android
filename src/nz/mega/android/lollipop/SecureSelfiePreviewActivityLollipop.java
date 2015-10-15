package nz.mega.android.lollipop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.MimeTypeList;
import nz.mega.android.R;
import nz.mega.android.UploadHereDialog;
import nz.mega.android.utils.Util;
import nz.mega.components.TouchImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SecureSelfiePreviewActivityLollipop extends PinActivityLollipop implements OnClickListener{
	
	private Display display;
	private DisplayMetrics outMetrics;
	private float density;
	private float scaleW;
	private float scaleH;
	
	private boolean aBshown = true;
	
	ProgressDialog statusDialog;
	
	private TextView fileNameTextView;
	private ImageView discardIcon;
	private ImageView uploadIcon;
	
    private RelativeLayout topLayout;
    
    private TouchImageView secureSelfieImage;
	public UploadHereDialog uploadDialog;
    
    String filePath;
	File imgFile;
	
	static SecureSelfiePreviewActivityLollipop secureSelfiePreviewActivity;
    
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_SELFIE_FOLDER = 1005;
	
	MegaNode node;
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("secureSelfiePreviewActivity onCreate");
		super.onCreate(savedInstanceState);

		secureSelfiePreviewActivity = this;
		
		MegaApplication app = (MegaApplication)getApplication();
		
		setContentView(R.layout.activity_secure_selfie_preview);
		
		secureSelfieImage = (TouchImageView) findViewById(R.id.secure_selfie_viewer_image);
		secureSelfieImage.setOnClickListener(this);
		secureSelfieImage.setVisibility(View.VISIBLE);
		
		discardIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_discard);
		discardIcon.setVisibility(View.VISIBLE);
		discardIcon.setOnClickListener(this);		
		
		uploadIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_upload);
		uploadIcon.setVisibility(View.VISIBLE);
		uploadIcon.setOnClickListener(this);		
		
	    topLayout = (RelativeLayout) findViewById(R.id.secure_selfie_viewer_layout_top);
		
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
//		    ActionBar actionBar = getSupportActionBar();
//		    if (actionBar != null){
//		    	actionBar.hide();
//		    }
//
//		}
		
//		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
//	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//	    }
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);			
			
		filePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR + "/picture.jpg";
		imgFile = new File(filePath);

		if(imgFile.exists()){

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap preview = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
			
			ExifInterface exif;
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			try {
				exif = new ExifInterface(imgFile.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			} catch (IOException e) {}  
			
			// Calculate inSampleSize
		    options.inSampleSize = Util.calculateInSampleSize(options, 1000, 1000);
		    
		    // Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    preview = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
		    
		    if (preview != null){
				preview = Util.rotateBitmap(preview, orientation);
				secureSelfieImage.setImageBitmap(preview);
			}		    
		}
	}

	@Override
	public void onClick(View v) {		
		log("onClick");
		
		switch (v.getId()){
//			case R.id.secure_selfie_viewer_icon:{
//				//Delete image
//				if(imgFile.exists()){
//					imgFile.delete();
//				}				
//				finish();
//				break;
//			}
//			case R.id.secure_selfie_viewer_discard:{				
//				log("Option discard");
//				if(imgFile.exists()){
//					imgFile.delete();
//				}	
//				finish();
//				break;
//			}
			case R.id.secure_selfie_viewer_discard:{
				//Delete image
				Intent intent = new Intent(this, ManagerActivityLollipop.class);
				intent.setAction(ManagerActivityLollipop.ACTION_TAKE_SELFIE);
				//intent.putExtra("IMAGE_PATH", imagePath);
				//Delete image
				if(imgFile.exists()){
					imgFile.delete();
				}
				startActivity(intent);
				finish();
				break;
			}
			case R.id.secure_selfie_viewer_upload:{
				
				String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
				log("Name: "+name);
				String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR + "/"+name;						
				log("----NEW Name: "+newPath);
				File newFile = new File(newPath);
				imgFile.renameTo(newFile);
				
				showFileChooser(newPath);		
				
				break;
			}
			case R.id.secure_selfie_viewer_image:{
				log("click on secure_selfie_viewer_image");
				
				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics ();
			    display.getMetrics(outMetrics);
			    float density  = getResources().getDisplayMetrics().density;
				
			    float scaleW = Util.getScaleW(outMetrics, density);
			    float scaleH = Util.getScaleH(outMetrics, density);

				if (aBshown){
					log("Hide topLayout");
					TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
					animTop.setDuration(1000);
					animTop.setFillAfter( true );
//					topLayout.setAnimation(animTop);
					topLayout.startAnimation(animTop);					
					aBshown = false;
				}
				else{					
					log("Shooow topLayout");
					TranslateAnimation animTop = new TranslateAnimation(0, 0, Util.px2dp(-48, outMetrics), 0);
					animTop.setDuration(1000);
					animTop.setFillAfter( true );
//					topLayout.setAnimation(animTop);
					topLayout.startAnimation(animTop);
					aBshown = true;
				}

				
//				RelativeLayout activityLayout = (RelativeLayout) activity.findViewById(R.id.full_image_viewer_parent_layout);
//				activityLayout.invalidate();

				
				break;
			}
		}

	}
	
	public void showFileChooser(String imagePath){
		log("showMove");
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_UPLOAD_SELFIE);
		intent.putExtra("IMAGE_PATH", imagePath);
		startActivity(intent);
		finish();
	}
	
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

	}	
	
	@Override
	public void onBackPressed() {
		
		if(imgFile.exists()){
			imgFile.delete();
		}			
		super.onBackPressed();
	}
	
//	@Override
//	public void onRestoreInstanceState (Bundle savedInstanceState){
//		super.onRestoreInstanceState(savedInstanceState);
//		
//		adapterType = savedInstanceState.getInt("adapterType");
//		
//		if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
//			
//		}
//		else{
//			aBshown = savedInstanceState.getBoolean("aBshown");
//			adapterMega.setaBshown(aBshown);
//			overflowVisible = savedInstanceState.getBoolean("overflowVisible");
//			adapterMega.setMenuVisible(overflowVisible);
//		}
//		
//		if (!aBshown){
//			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
//			animBottom.setDuration(0);
//			animBottom.setFillAfter( true );
//			bottomLayout.setAnimation(animBottom);
//			
//			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
//			animTop.setDuration(0);
//			animTop.setFillAfter( true );
//			topLayout.setAnimation(animTop);
//		}
//		
//		if (overflowVisible){
//			overflowMenuList.setVisibility(View.VISIBLE);
//		}
//		else{
//			overflowMenuList.setVisibility(View.GONE);
//		}
//	}	
		
}
