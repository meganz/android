package nz.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nz.mega.android.utils.Util;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SecureSelfiePreviewActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface{
	
	private Display display;
	private DisplayMetrics outMetrics;
	private float density;
	private float scaleW;
	private float scaleH;
	
	private boolean aBshown = true;
	
	ProgressDialog statusDialog;
	
	private TextView fileNameTextView;
	private ImageView actionBarIcon;
	private ImageView discardIcon;
	private ImageView repeatIcon;
	private ImageView uploadIcon;
	
	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
    
    private ImageView secureSelfieImage;
	public UploadHereDialog uploadDialog;
    
    String filePath;
	File imgFile;
	
	static SecureSelfiePreviewActivity secureSelfiePreviewActivity;
    private MegaApiAndroid megaApi;

    private String path;    
    
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_SELFIE_FOLDER = 1005;
	
	MegaNode node;
	
	boolean shareIt = true;
	boolean moveToRubbish = false;
	
	private static int EDIT_TEXT_ID = 1;
	private Handler handler;
	
	private AlertDialog renameDialog;
	
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("secureSelfiePreviewActivity");
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		secureSelfiePreviewActivity = this;
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
		    requestWindowFeature(Window.FEATURE_NO_TITLE); 
		    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.activity_secure_selfie_preview);
		
		secureSelfieImage = (ImageView) findViewById(R.id.secure_selfie_viewer_image);
		secureSelfieImage.setVisibility(View.VISIBLE);
		
		actionBarIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_icon);
		actionBarIcon.setOnClickListener(this);
		
		discardIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_discard);
		discardIcon.setVisibility(View.VISIBLE);
		discardIcon.setOnClickListener(this);		
		
		uploadIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_upload);
		uploadIcon.setVisibility(View.VISIBLE);
		uploadIcon.setOnClickListener(this);		
		
		repeatIcon = (ImageView) findViewById(R.id.secure_selfie_viewer_repeat);
		repeatIcon.setOnClickListener(this);
		repeatIcon.setVisibility(View.VISIBLE);
		
		bottomLayout = (RelativeLayout) findViewById(R.id.secure_selfie_viewer_layout_bottom);
	    topLayout = (RelativeLayout) findViewById(R.id.secure_selfie_viewer_layout_top);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
		    ActionBar actionBar = getSupportActionBar();
		    if (actionBar != null){
		    	actionBar.hide();
		    }

		}
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }
		
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
		
		switch (v.getId()){
			case R.id.secure_selfie_viewer_icon:{
				//Delete image
				if(imgFile.exists()){
					imgFile.delete();
				}				
				finish();
				break;
			}
			case R.id.secure_selfie_viewer_discard:{
				if(imgFile.exists()){
					imgFile.delete();
				}	
				finish();
				break;
			}
			case R.id.secure_selfie_viewer_repeat:{
				//Delete image
				Intent intent = new Intent(this, ManagerActivity.class);
				intent.setAction(ManagerActivity.ACTION_TAKE_SELFIE);
				//intent.putExtra("IMAGE_PATH", imagePath);
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
		}

	}
	
	public void showFileChooser(String imagePath){
		log("showMove");
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_UPLOAD_SELFIE);
		intent.putExtra("IMAGE_PATH", imagePath);
		//startActivity(intent);
		startActivityForResult(intent, REQUEST_CODE_SELECT_SELFIE_FOLDER);
	}
	
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult - FINISH ----------------------------------------------");
		
		if (requestCode == REQUEST_CODE_SELECT_SELFIE_FOLDER && resultCode == RESULT_OK) {
			
			finish();
		}
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
//		if ((adapterType == ManagerActivity.OFFLINE_ADAPTER) || (adapterType == ManagerActivity.ZIP_ADAPTER)){
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
		
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}	
		
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		
		node = megaApi.getNodeByHandle(request.getNodeHandle());
		
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_EXPORT){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				
				final String link = request.getLink();
				
				AlertDialog getLinkDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.context_get_link_menu));
				
				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
				ImageView thumb = (ImageView) dialoglayout.findViewById(R.id.dialog_link_thumbnail);
				TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
				TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
				
				String urlString = "";
				String keyString = "";
				String [] s = link.split("!");
				if (s.length == 3){
					urlString = s[0] + "!" + s[1];
					keyString = s[2];
				}
				if (node.isFolder()){
					thumb.setImageResource(R.drawable.folder_thumbnail);
				}
				else{
					thumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
				}
				
				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics();
				display.getMetrics(outMetrics);
				float density = getResources().getDisplayMetrics().density;

				float scaleW = Util.getScaleW(outMetrics, density);
				float scaleH = Util.getScaleH(outMetrics, density);
				
				url.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
				key.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
				
				url.setText(urlString);
				key.setText(keyString);
				
				
				builder.setView(dialoglayout);
				
				builder.setPositiveButton(getString(R.string.context_send_link), new android.content.DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_TEXT, link);
						startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
					}
				});
				
				builder.setNegativeButton(getString(R.string.context_copy_link), new android.content.DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(link);
						} else {
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
				            clipboard.setPrimaryClip(clip);
						}

					}
				});
				
				getLinkDialog = builder.create();
				getLinkDialog.show();
				Util.brandAlertDialog(getLinkDialog);
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_link), Toast.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_correctly_renamed), Toast.LENGTH_SHORT).show();
//				nameView.setText(megaApi.getNodeByHandle(request.getNodeHandle()).getName());
			}			
			else{
				Toast.makeText(this, getString(R.string.context_no_renamed), Toast.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
					finish();
				}
				else{
					Toast.makeText(this, getString(R.string.context_no_moved), Toast.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
					finish();
				}
				else{
					Toast.makeText(this, getString(R.string.context_no_moved), Toast.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			
			
			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog.isShowing()){
					try { 
						statusDialog.dismiss();	
					} 
					catch (Exception ex) {}
					Toast.makeText(this, getString(R.string.context_correctly_removed), Toast.LENGTH_SHORT).show();
				}
				finish();
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_removed), Toast.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_correctly_copied), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_copied), Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());	
	}
	
	public static void log(String message) {
		Util.log("SecureSelfiePreviewActivity", message);
	}

//	@Override
//	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		
////		overflowMenuList.setVisibility(View.GONE);
////		overflowVisible = false;
////		adapterMega.setMenuVisible(overflowVisible);
//		
//		switch(position){
//			case 0:{
//				shareIt = false;
//		    	getPublicLinkAndShareIt();
//				break;
//			}			
//		}
//	}	
	
	
	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
}
