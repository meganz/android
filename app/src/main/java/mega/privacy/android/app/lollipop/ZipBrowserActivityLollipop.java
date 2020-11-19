package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.audioplayer.AudioPlayerActivity;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.ZipListAdapterLollipop;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;


public class ZipBrowserActivityLollipop extends PinActivityLollipop{
	public static ImageView imageDrag;
	int[] screenPosition;

	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_HANDLE_ZIP ="HANDLE_ZIP";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String ACTION_OPEN_ZIP_FILE = "OPEN_ZIP_FILE";

    MegaApplication app;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;
    ProgressDialog temp = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	boolean folderzipped = false;

	RelativeLayout zipLayout;
	//ListView listView;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;

	ZipListAdapterLollipop adapterList;
	Toolbar tB;
	ActionBar aB;
	ZipFile myZipFile;
	String pathZip;
	List<ZipEntry> zipNodes;
	String currentFolder;
	String currentPath;
	int depth;
	String downloadLocationDefaultPath;

	ZipBrowserActivityLollipop zipBrowserActivityLollipop;

	DisplayMetrics outMetrics;
	
	/*
	 * Background task to unzip the file.zip
	 */
	private class UnZipTask extends AsyncTask<String, Void, String> {
		String pathZipTask;
		int position;
		
		UnZipTask(String _pathZipTask, int _position){
			this.pathZipTask = _pathZipTask;
			this.position = _position;
		}
		
		@Override
		protected String doInBackground(String... params) {			
	
			this.unpackZip();		
			return "SUCCESS";
		}

		@Override
		protected void onPostExecute(String info) {
			//Open the file
			logDebug("onPostExecute");
			
			if (temp.isShowing()){
				try{
					temp.dismiss();
					openFile(position);
				}
				catch(Exception e) {
					e.printStackTrace();					
				}				
			}			
		}
		
		private boolean unpackZip() {
			//TODO:Comprobar el flag, de ahora en adelante si está unzip tener en cuenta que estará en la carpeta correspondiente
			
			String destination;		
			
			if(folderzipped){
				int index = pathZip.lastIndexOf("/");
				destination = pathZip.substring(0, index+1);
				logDebug("Destination: " + destination);
			}
			else{
				int index = pathZip.lastIndexOf(".");
				destination = pathZip.substring(0, index);		
				destination = destination +"/";
				logDebug("Destination: " + destination);
				File f = new File(destination);
				f.mkdirs();				
			}								
				
			try {
				FileInputStream fin = new FileInputStream(pathZipTask);
				ZipInputStream zin = new ZipInputStream(new BufferedInputStream(fin));
				ZipEntry ze = null; 

				while((ze = zin.getNextEntry()) != null) {
					if(ze.isDirectory()) {
						File f = new File(destination + ze.getName());
						f.mkdirs();
					} else { 
						byte[] buffer2 = new byte[1024];
						FileOutputStream fout = new FileOutputStream(destination + ze.getName());
						for(int c = zin.read(buffer2); c > 0; c = zin.read(buffer2)) {
							fout.write(buffer2,0,c);
						}
						zin.closeEntry();
						fout.close();
					}
				}
				zin.close();
			} 
			catch(IOException e) {
				e.printStackTrace();
				return false;
			}			
		
			return true;
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int position;
			int adapterType;
			int actionType;
			ImageView imageDrag = null;

			if (intent != null) {
				position = intent.getIntExtra("position", -1);
				adapterType = intent.getIntExtra("adapterType", 0);
				actionType = intent.getIntExtra("actionType", -1);

				if (position != -1) {
					if (adapterType == ZIP_ADAPTER) {
						if (actionType == UPDATE_IMAGE_DRAG) {
							imageDrag = getImageDrag(position);
							if (zipBrowserActivityLollipop.imageDrag != null) {
								zipBrowserActivityLollipop.imageDrag.setVisibility(View.VISIBLE);
							}
							if (imageDrag != null) {
								zipBrowserActivityLollipop.imageDrag = imageDrag;
								zipBrowserActivityLollipop.imageDrag.setVisibility(View.GONE);
							}
						} else if (actionType == SCROLL_TO_POSITION) {
							updateScrollPosition(position);
						}
					}
				}

				if (imageDrag != null){
					int[] positionDrag = new int[2];
					int[] screenPosition = new int[4];
					imageDrag.getLocationOnScreen(positionDrag);

					screenPosition[0] = (imageDrag.getWidth() / 2) + positionDrag[0];
					screenPosition[1] = (imageDrag.getHeight() / 2) + positionDrag[1];
					screenPosition[2] = imageDrag.getWidth();
					screenPosition[3] = imageDrag.getHeight();

					Intent intent1 =  new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG);
					intent1.putExtra("screenPosition", screenPosition);
					sendBroadcast(intent1);
				}
			}
		}
	};

	public void updateScrollPosition(int position) {
		logDebug("Position: " + position);
		if (adapterList != null && mLayoutManager != null){
			mLayoutManager.scrollToPosition(position);
		}
	}

	public ImageView getImageDrag(int position) {
		logDebug("Position: " + position);
		if (adapterList != null && mLayoutManager != null){
			View v = mLayoutManager.findViewByPosition(position);
			if (v != null){
				return (ImageView) v.findViewById(R.id.file_list_thumbnail);
			}
		}

		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(receiver);
	}

	@SuppressLint("NewApi") @Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication)getApplication();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				
		depth=3;

		zipBrowserActivityLollipop = this;

		zipNodes = new ArrayList<ZipEntry>();
			
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			pathZip = extras.getString(EXTRA_PATH_ZIP);					
		}
		
		currentPath = pathZip;
		downloadLocationDefaultPath = getDownloadLocation();

		registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION));
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);	
		
		setContentView(R.layout.activity_zip_browser);
		tB = (Toolbar) findViewById(R.id.toolbar);
		tB.setVisibility(View.VISIBLE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setTitle(getString(R.string.zip_browser_activity));
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}
		zipLayout = (RelativeLayout) findViewById(R.id.zip_layout);
		recyclerView = (RecyclerView) findViewById(R.id.zip_list_view_browser);
		recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
		recyclerView.setClipToPadding(false);
		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
			
		String[] parts = pathZip.split("/");
		if(parts.length>0){
			currentFolder= parts[parts.length-1];
			currentFolder= currentFolder.replace(".zip", "");
			
		}
		else{
			currentFolder= pathZip;
		}
		
		folderzipped = false;

		aB.setTitle("ZIP "+currentFolder);

		try {
			myZipFile = new ZipFile(pathZip);

			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				try {
					ZipEntry element = zipEntries.nextElement();
					if (element.getName().startsWith(currentFolder+"/")) {
						folderzipped = true;
					}
					else {
						folderzipped = false;
					}

					String[] pE = element.getName().split("/");

					if(element.isDirectory()){
						if (folderzipped) {
							if(!element.getName().equals(currentFolder+"/")){
								if(pE.length<depth){
									zipNodes.add(element);
								}
							}
						}
						else {
							if(pE.length<3){
								zipNodes.add(element);
							}
						}
					}
					else{
						if (folderzipped) {
							if(pE.length==depth-1){
								zipNodes.add(element);
							}
						}
						else {
							if(pE.length==1){
								zipNodes.add(element);
							}
						}
					}
				} catch (IllegalArgumentException e) {
					logError("Fails exploring zip", e);
					e.printStackTrace();
//					Add unknown element
					zipNodes.add(new ZipEntry(getString(R.string.transfer_unknown)));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		orderZips();
		if (adapterList == null){
			adapterList = new ZipListAdapterLollipop(this, recyclerView, aB, zipNodes, currentFolder);
		}

		recyclerView.setAdapter(adapterList);
	}

	void orderZips () {
		Collections.sort(zipNodes, new Comparator<ZipEntry>(){

			public int compare(ZipEntry z1, ZipEntry z2) {
				String name1 = z1.getName();
				String name2 = z2.getName();
				int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
				if (res == 0) {
					res = name1.compareTo(name2);
				}
				return res;
			}
		});
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("OnOptionsItemSelected");
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				onBackPressed();
				return true;
			}
		}
		return true;
	}
	
	public void openFile(int position) {
        logDebug("Position: " + position);
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		String downloadLocationDefaultPath = getDownloadLocation();
		
		String absolutePath= downloadLocationDefaultPath+"/"+currentPath;
		if(!folderzipped){
            logDebug("folderzipped = " + folderzipped);
			int index = pathZip.lastIndexOf(".");
			absolutePath = pathZip.substring(0, index);
			absolutePath = absolutePath +"/"+currentPath;
		}
		else{
            logDebug("folderzipped = " + folderzipped);
		}

        logDebug("The absolutePath of the file to open is: " + absolutePath);

		File currentFile = new File(absolutePath);
		ZipEntry currentNode = zipNodes.get(position);

		if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
            logDebug("isImage");
			Intent intent = new Intent(this, FullScreenImageViewerLollipop.class);
			intent.putExtra("position", position);
			intent.putExtra("adapterType", ZIP_ADAPTER);
			intent.putExtra("isFolderLink", false);
			intent.putExtra("parentNodeHandle", -1L);
			intent.putExtra("offlinePathDirectory", absolutePath);
			intent.putExtra("orderGetChildren", orderGetChildren);
			intent.putExtra("screenPosition", screenPosition);
			startActivity(intent);
			overridePendingTransition(0,0);
		}
		else if (MimeTypeList.typeForName(currentFile.getName()).isVideoReproducible() || MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
            logDebug("Video file");

			Intent mediaIntent;
			boolean internalIntent;
			boolean opusFile = false;
			if (MimeTypeList.typeForName(currentFile.getName()).isVideoNotSupported() || MimeTypeList.typeForName(currentFile.getName()).isAudioNotSupported()) {
				mediaIntent = new Intent(Intent.ACTION_VIEW);
				internalIntent = false;
				String[] s = currentFile.getName().split("\\.");
				if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
					opusFile = true;
				}
			}
			else {
				internalIntent = true;
				if (MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
					mediaIntent = new Intent(this, AudioPlayerActivity.class);
				} else {
					mediaIntent = new Intent(this, AudioVideoPlayerLollipop.class);
				}
			}

			int index = currentNode.getName().lastIndexOf('/');
			String name = currentNode.getName().substring(index+1);
			mediaIntent.putExtra("FILENAME", name);
			mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, (long) name.hashCode());
			mediaIntent.putExtra("path", currentFile.getAbsolutePath());
			mediaIntent.putExtra("adapterType", ZIP_ADAPTER);
			mediaIntent.putExtra("position", position);
			mediaIntent.putExtra("parentNodeHandle", -1L);
			mediaIntent.putExtra("offlinePathDirectory", absolutePath);
			mediaIntent.putExtra("orderGetChildren", orderGetChildren);
			mediaIntent.putExtra("screenPosition", screenPosition);
			mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && absolutePath.contains(Environment.getExternalStorageDirectory().getPath())) {
				mediaIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			else{
				mediaIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			if (opusFile){
				mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
			}
			if (internalIntent){
				startActivity(mediaIntent);
			}
			else {
				if (isIntentAvailable(this, mediaIntent)){
					startActivity(mediaIntent);
				}
				else {
					showSnackbar(getString(R.string.intent_not_available));

					Intent intentShare = new Intent(Intent.ACTION_SEND);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					else {
						intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					if (isIntentAvailable(this, intentShare)) {
                        logDebug("Call to startActivity(intentShare)");
						startActivity(intentShare);
					}
				}
			}
			overridePendingTransition(0,0);
		}
		else if (MimeTypeList.typeForName(currentFile.getName()).isPdf()){
            logDebug("Pdf file");
			Intent pdfIntent = new Intent(this, PdfViewerActivityLollipop.class);

			pdfIntent.putExtra("inside", true);
//			pdfIntent.putExtra("HANDLE", Long.parseLong(currentNode.getHandle()));
			pdfIntent.putExtra("adapterType", ZIP_ADAPTER);
			pdfIntent.putExtra("path", currentFile.getAbsolutePath());
			pdfIntent.putExtra("screenPosition", screenPosition);
			pdfIntent.putExtra("offlinePathDirectory", absolutePath);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && absolutePath.contains(Environment.getExternalStorageDirectory().getPath())) {
				pdfIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			else{
				pdfIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(pdfIntent);
			overridePendingTransition(0,0);
		}
		else{
            logDebug("NOT Image, video, audio or pdf");
			try {
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
				} else {
					viewIntent.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
				}
				viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				if (isIntentAvailable(this, viewIntent))
					startActivity(viewIntent);
				else {
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
					} else {
						intentShare.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
					}
					intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					if (isIntentAvailable(this, intentShare))
						startActivity(intentShare);
					String toastMessage = getString(R.string.general_already_downloaded) + ": " + absolutePath;
					Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
				}
			}
			catch (Exception e){
				String toastMessage = getString(R.string.general_already_downloaded) + ": " + absolutePath;
				Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
			}
			return;
		}		
	}

	public void showSnackbar(String s){
		showSnackbar(zipLayout, s);
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
	}

	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
        logDebug("Position: " + position);
		this.screenPosition = screenPosition;
		this.imageDrag = imageView;

		ZipEntry currentNode = zipNodes.get(position);

		currentPath=currentNode.getName();

        logDebug("currentPath: " + currentPath);

		if(currentNode.isDirectory()){
			depth=depth+1;
			listDirectory(currentPath);
			this.setFolder(currentPath);
			orderZips();
			adapterList.setNodes(zipNodes);
		}
		else{
			String checkFolder = null;
			int index = pathZip.lastIndexOf(".");
			checkFolder = pathZip.substring(0, index);

			if(checkFolder!=null){
				File check = new File(checkFolder);

				if(check.exists()){
                    logDebug("Already unzipped");
					openFile(position);
				}
				else{
					UnZipTask unZipTask = new UnZipTask(pathZip, position);
					unZipTask.execute();
					try{
						temp = new ProgressDialog(this);
						temp.setMessage(getString(R.string.unzipping_process));
						temp.show();
					}
					catch(Exception e){
						return;
					}
				}

			}
		}
	}


	private void listDirectory (String directory){
        logDebug("Directory: " + directory);
		
		zipNodes.clear();

		Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
		while (zipEntries.hasMoreElements()) {

			try {
				ZipEntry element = zipEntries.nextElement();
				String[] pE = element.getName().split("/");
				if(element.getName().startsWith(directory)){
					if(element.isDirectory()){
						if (directory.isEmpty()) {
							if(pE.length<3){
								zipNodes.add(element);
							}
						}
						else {
							if(!element.getName().equals(directory)){
								if(pE.length<depth){
									zipNodes.add(element);
								}
							}
						}
					}
					else{
						if (directory.isEmpty()) {
							if(pE.length==1){
								zipNodes.add(element);
							}
						}
						else {
							if(pE.length<depth){
								zipNodes.add(element);
							}
						}
					}
				}
			} catch (IllegalArgumentException e) {
                logError("zipEntries.nextElement() fails listing directory", e);
			    e.printStackTrace();
                zipNodes.add(new ZipEntry(getString(R.string.transfer_unknown)));
			}
		}
	}
	
	public void onBackPressed(){
		
		depth = depth - 1;

        logDebug("Depth: " + depth);
        logDebug("currentPath: " + currentPath);
		
		if(depth<3){
			super.onBackPressed();
		} 
		else if (depth==3&&(!folderzipped)){
			currentPath="";
			listDirectory(currentPath);	
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);	
		}
		else{
			if(currentPath==null || currentPath.length()==0){
				currentPath=pathZip;
				int index = currentPath.lastIndexOf("/");		
				currentPath = currentPath.substring(0, index);
				index = currentPath.lastIndexOf("/");		
				currentPath = currentPath.substring(0, index+1);
				depth=3;
			}
			else{
				if(currentPath.endsWith("/")){
					currentPath=currentPath.substring(0, currentPath.length()-1);	
					int index = currentPath.lastIndexOf("/");		
					currentPath = currentPath.substring(0, index+1);
				}
				else{
					int index = currentPath.lastIndexOf("/");		
					currentPath = currentPath.substring(0, index);
					index = currentPath.lastIndexOf("/");		
					currentPath = currentPath.substring(0, index+1);
				}	
			}	
			
			listDirectory(currentPath);	
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);	
		}	
	}

	private void setFolder(String folder){
		
		String[] parts = folder.split("/");
		if(parts.length>0){
			currentFolder= parts[parts.length-1];							
		}
		else{
			currentFolder= pathZip;
		}
		
		if(currentFolder.length()>0){
			aB.setTitle("ZIP "+currentFolder);
		}
		else {
			parts = pathZip.split("/");
			if(parts.length>0){
				currentFolder= parts[parts.length-1];
				currentFolder = currentFolder.replace(".zip", "");
			}
			else{
				currentFolder= pathZip;
			}
			
			aB.setTitle("ZIP "+currentFolder);
		}
		//log("setFolder: "+currentFolder);
		adapterList.setFolder(currentFolder);
	}

	public String countFiles(String directory){
        logDebug("Directory: " + directory);
		
		int index = pathZip.lastIndexOf("/");
		String toEliminate = pathZip.substring(0, index+1);
		
		String currentPathCount= currentPath.replace(".zip", "");
		
		currentPathCount= currentPathCount.replace(toEliminate, "");
		
		int numFolders=0;
		int numFiles=0;

		if(depth==3&&!folderzipped){

			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				try {
					ZipEntry element = zipEntries.nextElement();

					if(element.getName().startsWith(directory+"/")){
						if(element.isDirectory()){
							//log("Directory: "+element.getName());
							if(!element.getName().equals(directory+"/")){
								numFolders++;
							}
						}
						else{
							numFiles++;
						}
					}
				} catch (IllegalArgumentException e) {
					logError("zipEntries.nextElement() fails counting files", e);
                    e.printStackTrace();
				}
			}
		}
		else{
			if(currentPathCount.lastIndexOf("/")==currentPathCount.length()-1){
				currentPathCount=currentPathCount+directory+"/";
			}
			else{				
				currentPathCount=currentPathCount+"/"+directory+"/";
			}

			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				try {
					ZipEntry element = zipEntries.nextElement();

					if(element.getName().startsWith(currentPathCount)){
						if(element.isDirectory()){
							//log("Directory: "+element.getName());
							if(!element.getName().equals(currentPathCount)){
								numFolders++;
							}
						}
						else{
							numFiles++;
						}
					}
				} catch (IllegalArgumentException e) {
                    logError("zipEntries.nextElement() fails counting files", e);
				    e.printStackTrace();
				}
			}
		}

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ this.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ this.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		} else {
			info = numFiles
					+ " "
					+ this.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		return info;
	}
}
