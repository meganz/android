package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.ZipListAdapterLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.utils.Util.context;


public class ZipBrowserActivityLollipop extends PinActivityLollipop implements OnItemClickListener, OnItemLongClickListener{
	
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_HANDLE_ZIP ="HANDLE_ZIP";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String ACTION_OPEN_ZIP_FILE = "OPEN_ZIP_FILE";

    MegaApplication app;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;
    ProgressDialog temp = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	boolean folderzipped = false;
    
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
	
	/*
	 * Background task to unzip the file.zip
	 */
	private class UnZipTask extends AsyncTask<String, Void, String> {
		String pathZipTask;
		int position;
		
		UnZipTask(Context _context, String _pathZipTask, int _position){
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
			log("onPostExecute");
			
			if (temp.isShowing()){
				try{
					temp.dismiss();
										
					openFile(position);
				}
				catch(Exception e)
				{
					e.printStackTrace();					
				}				
			}			
		}
		
		private boolean unpackZip()
		{			
			
			//TODO:Comprobar el flag, de ahora en adelante si está unzip tener en cuenta que estará en la carpeta correspondiente
			
			String destination;		
			
			if(folderzipped){
				int index = pathZip.lastIndexOf("/");
				destination = pathZip.substring(0, index+1);
				log("Destination1: "+destination);
			}
			else{
				int index = pathZip.lastIndexOf(".");
				destination = pathZip.substring(0, index);		
				destination = destination +"/";
				log("Destination2: "+destination);
				File f = new File(destination);
				f.mkdirs();				
			}								
				
			try 
			{
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
			catch(IOException e)
			{
				e.printStackTrace();
				return false;
			}			
		
			return true;
		}
	}	
	
	@SuppressLint("NewApi") @Override
	public void onCreate (Bundle savedInstanceState){		
		log("onCreate");
		
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication)getApplication();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				
		depth=3;
		
		zipNodes = new ArrayList<ZipEntry>();
			
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			pathZip = extras.getString(EXTRA_PATH_ZIP);					
		}
		
		currentPath = pathZip;
		downloadLocationDefaultPath = Util.downloadDIR;
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
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

		recyclerView = (RecyclerView) findViewById(R.id.zip_list_view_browser);
		recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
		recyclerView.setClipToPadding(false);
		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		try {
			myZipFile = new ZipFile(pathZip);			
	
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				
				ZipEntry element = zipEntries.nextElement();	
				
				log(element.getName());
			}
				
		} catch (IOException e) {

			e.printStackTrace();
		} 

			
		String[] parts = pathZip.split("/");
		if(parts.length>0){

			currentFolder= parts[parts.length-1];
			parts = currentFolder.split("\\.");
			
			currentFolder= currentFolder.replace(".zip", "");
			
		}else{
			currentFolder= pathZip;
		}
		
		folderzipped = false;
		
		try {
			myZipFile = new ZipFile(pathZip);			
	
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				
				ZipEntry element = zipEntries.nextElement();				
				
				if(element.getName().startsWith(currentFolder+"/")){
					folderzipped=true;					
				}
				else{
					folderzipped=false;	
					
				}				
			}
	
		} catch (IOException e) {

			e.printStackTrace();
		} 	
		
		if(folderzipped){	
			aB.setTitle("ZIP "+currentFolder);
			
			try {
				myZipFile = new ZipFile(pathZip);			
		
				Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
				while (zipEntries.hasMoreElements()) {
					
					ZipEntry element = zipEntries.nextElement();	
		
					if(element.isDirectory()){

						if(!element.getName().equals(currentFolder+"/")){

							String[] pE = element.getName().split("/");
							if(pE.length<depth){
								zipNodes.add(element);
							}
						}					
					}
					else{
						
						String[] pE = element.getName().split("/");
						if(pE.length==depth-1){
							zipNodes.add(element);
						}
					}					
				}
			} catch (IOException e) {

				e.printStackTrace();
			} 	
			
		}
		else{
			aB.setTitle("ZIP "+currentFolder);
			//currentFolder="";
			
			try {
				myZipFile = new ZipFile(pathZip);			
		
				Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
				while (zipEntries.hasMoreElements()) {
					
					ZipEntry element = zipEntries.nextElement();	
										
					if(element.isDirectory()){

						String[] pE = element.getName().split("/");
						if(pE.length<3){
							zipNodes.add(element);
						}
											
					}
					else{
						
						String[] pE = element.getName().split("/");
						if(pE.length==1){
							zipNodes.add(element);
						}
					}					
				}
			} catch (IOException e) {

				e.printStackTrace();
			} 	
		}
		

		if (adapterList == null){
			adapterList = new ZipListAdapterLollipop(this, recyclerView, aB, zipNodes, currentFolder);
		}
		else{
//			adapterList.setParentHandle(parentHandle);
//			adapterList.setNodes(nodes);
		}		

		recyclerView.setAdapter(adapterList);


		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		log("OnOptionsItemSelected");
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
		log("openFile");
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		String downloadLocationDefaultPath = Util.downloadDIR;
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}		
		
		String absolutePath= downloadLocationDefaultPath+"/"+currentPath;
		if(!folderzipped){
			log("folderzipped = "+folderzipped);
			int index = pathZip.lastIndexOf(".");
			absolutePath = pathZip.substring(0, index);
			absolutePath = absolutePath +"/"+currentPath;
		}
		else{
			log("folderzipped = "+folderzipped);
		}
		
		log("The absolutePath of the file to open is: "+absolutePath);
		 
		if (MimeTypeList.typeForName(absolutePath).isImage()){
			log("isImage");
			Intent intent = new Intent(this, FullScreenImageViewerLollipop.class);
			intent.putExtra("position", position);
			intent.putExtra("adapterType", Constants.ZIP_ADAPTER);
			intent.putExtra("parentNodeHandle", -1L);			
			File currentFile = new File(absolutePath);			
			intent.putExtra("offlinePathDirectory", currentFile.getParent());
			startActivity(intent);
			overridePendingTransition(0,0);
		}
//		else if(MimeType.typeForName(absolutePath).isPdf()){
//			
//		    File pdfFile = new File(absolutePath);
//		    
//		    Intent intentPdf = new Intent();
//		    intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//		    intentPdf.setClass(this, OpenPDFActivity.class);
//		    intentPdf.setAction("android.intent.action.VIEW");
//			this.startActivity(intentPdf);
//			
//		}
		else{
			log("NOT Image");
			try {
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
				} else {
					viewIntent.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
				}
				viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				if (MegaApiUtils.isIntentAvailable(this, viewIntent))
					startActivity(viewIntent);
				else {
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
					} else {
						intentShare.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeTypeList.typeForName(absolutePath).getType());
					}
					intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					if (MegaApiUtils.isIntentAvailable(this, intentShare))
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
	}	
	public static void log(String log) {
		Util.log("ZipBrowserActivityLollipop", log);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {

		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		log("onItemClick");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		ZipEntry currentNode = zipNodes.get(position);
		
		currentPath=currentNode.getName();
		
		log("onItemClick, currentPath: "+currentPath);
		
		if(currentNode.isDirectory()){		
			depth=depth+1;	
			listDirectory(currentPath);	
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);				
		}
		else{	
			
			String checkFolder = null;
			int index = pathZip.lastIndexOf(".");
			checkFolder = pathZip.substring(0, index);

			if(checkFolder!=null){
				File check = new File(checkFolder);
			
				if(check.exists()){
					log("Already unzipped");
					openFile(position);

				}
				else{
					UnZipTask unZipTask = new UnZipTask(this, pathZip, position);
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

	public void itemClick(int position) {
		log("itemClick: position: "+position);
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		ZipEntry currentNode = zipNodes.get(position);

		currentPath=currentNode.getName();

		log("onItemClick, currentPath: "+currentPath);

		if(currentNode.isDirectory()){
			depth=depth+1;
			listDirectory(currentPath);
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);
		}
		else{

			String checkFolder = null;
			int index = pathZip.lastIndexOf(".");
			checkFolder = pathZip.substring(0, index);

			if(checkFolder!=null){
				File check = new File(checkFolder);

				if(check.exists()){
					log("Already unzipped");
					openFile(position);

				}
				else{
					UnZipTask unZipTask = new UnZipTask(this, pathZip, position);
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
		log("listDirectory: "+directory);
		
		zipNodes.clear();
		
		if(directory.isEmpty()){
			
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				
				ZipEntry element = zipEntries.nextElement();	
				
				if(element.getName().startsWith(directory)){
					
					if(element.isDirectory()){

						String[] pE = element.getName().split("/");
						if(pE.length<3){
							zipNodes.add(element);
						}
											
					}
					else{
						
						String[] pE = element.getName().split("/");
						if(pE.length==1){
							zipNodes.add(element);
						}
					}
				}
			}
		}
		else{
		
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				
				ZipEntry element = zipEntries.nextElement();	
				
				if(element.getName().startsWith(directory)){
					
					if(element.isDirectory()){
						log("Directory: "+element.getName());
						
						if(!element.getName().equals(directory)){
							String[] pE = element.getName().split("/");						
							if(pE.length<depth){							
								zipNodes.add(element);
							}
						}
					}
					else{			
						log("File: "+element.getName());
						String[] pE = element.getName().split("/");					
						if(pE.length<depth){
							zipNodes.add(element);
						}					
					}
				}
			}
		}	
	}
	
	public void onBackPressed(){
		
		depth=depth-1;
		
		log("Depth: "+depth);
		
		log("onBackPressed, currentPath: "+currentPath);

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		
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
		}else{
			currentFolder= pathZip;
		}
		
		if(currentFolder.length()>0){
			aB.setTitle("ZIP "+currentFolder);
		}
		else {
			parts = pathZip.split("/");
			if(parts.length>0){

				currentFolder= parts[parts.length-1];
				parts = currentFolder.split("\\.");
				
				currentFolder= currentFolder.replace(".zip", "");
				
			}else{
				currentFolder= pathZip;
			}
			
			aB.setTitle("ZIP "+currentFolder);
		}
		//log("setFolder: "+currentFolder);
		adapterList.setFolder(currentFolder);
	}

	public String countFiles(String directory){
		log("countFiles: "+directory);
		
		int index = pathZip.lastIndexOf("/");
		String toEliminate = pathZip.substring(0, index+1);
		
		String currentPathCount= currentPath.replace(".zip", "");
		
		currentPathCount= currentPathCount.replace(toEliminate, "");
		
		int numFolders=0;
		int numFiles=0;	

		if(depth==3&&!folderzipped){

			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {

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
