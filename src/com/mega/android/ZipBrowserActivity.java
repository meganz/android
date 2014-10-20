package com.mega.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.mega.android.pdfViewer.OpenPDFActivity;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaNode;

public class ZipBrowserActivity extends PinActivity implements OnClickListener, OnItemClickListener, OnItemLongClickListener{
	
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_HANDLE_ZIP ="HANDLE_ZIP";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String ACTION_OPEN_ZIP_FILE = "OPEN_ZIP_FILE";

    MegaApplication app;
    private MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;
    ProgressDialog temp = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
//	private static boolean activityVisible;
	
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	ListView listView;
	long totalSize; 
	ZipListAdapter adapterList;
	ActionBar aB;
//	List<String> filePaths;
	ZipFile myZipFile;
	String pathZip;
	long handleZip;
	List<ZipEntry> zipNodes;
	String currentFolder;
	String currentPath;
	int depth;
	String downloadLocationDefaultPath;	
	
	/*
	 * Background task to unzip the file.zip
	 */
	private class UnZipTask extends AsyncTask<String, Void, String> {
		Context context;
		String pathZipTask;
		int position;
		
		UnZipTask(Context _context, String _pathZipTask, int _position){
			this.context = context;
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
			int index = pathZip.lastIndexOf("/");
			String destination = pathZip.substring(0, index+1);			
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
	
	@Override
	public void onCreate (Bundle savedInstanceState){		
		log("onCreate");
		
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				
		depth=3;
		totalSize=0;
		
		zipNodes = new ArrayList<ZipEntry>();
			
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			pathZip = extras.getString(EXTRA_PATH_ZIP);
			handleZip = extras.getLong(EXTRA_HANDLE_ZIP);			
		}
		
		currentPath = pathZip;
		downloadLocationDefaultPath = Util.downloadDIR;
		
		aB = getSupportActionBar();		
		aB = getSupportActionBar();
		
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(true);
		aB.setTitle(getString(R.string.zip_browser_activity));

		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);	
		
		setContentView(R.layout.activity_zip_browser);
		
		listView = (ListView) findViewById(R.id.zip_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemsCanFocus(false);
		
		String[] parts = pathZip.split("/");
		if(parts.length>0){

			currentFolder= parts[parts.length-1];
			parts = currentFolder.split(".");
			
			currentFolder= currentFolder.replace(".zip", "");
			
		}else{
			currentFolder= pathZip;
		}
		
		aB.setTitle("ZIP "+currentFolder);
				
		try {
			myZipFile = new ZipFile(pathZip);
			
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {
				
				ZipEntry element = zipEntries.nextElement();	
				
				totalSize= totalSize+element.getSize();
				
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
						
		if (adapterList == null){
			adapterList = new ZipListAdapter(this, listView, aB, zipNodes, currentFolder);
			
		}
		else{
			//adapterList.setParentHandle(parentHandle);
			//adapterList.setNodes(nodes);
		}		

		listView.setAdapter(adapterList);		
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
		log("The absolutePath of the file to open is: "+absolutePath);
		 
		if (MimeType.typeForName(absolutePath).isImage()){
			Intent intent = new Intent(this, FullScreenImageViewer.class);
			intent.putExtra("position", position);
			intent.putExtra("adapterType", ManagerActivity.ZIP_ADAPTER);
			intent.putExtra("parentNodeHandle", -1L);			
			File currentFile = new File(absolutePath);			
			intent.putExtra("offlinePathDirectory", currentFile.getParent());
			startActivity(intent);
					
		}
		else if(MimeType.typeForName(absolutePath).isPdf()){
			
		    File pdfFile = new File(absolutePath);
		    
		    Intent intentPdf = new Intent();
		    intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
		    intentPdf.setClass(this, OpenPDFActivity.class);
		    intentPdf.setAction("android.intent.action.VIEW");
			this.startActivity(intentPdf);
			
		}
		else{							
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeType.typeForName(absolutePath).getType());
			if (isIntentAvailable(this, viewIntent))
				startActivity(viewIntent);
			else{
				Intent intentShare = new Intent(Intent.ACTION_SEND);
				intentShare.setDataAndType(Uri.fromFile(new File(absolutePath)), MimeType.typeForName(absolutePath).getType());
				if (isIntentAvailable(this, intentShare))
					startActivity(intentShare);
				String toastMessage = getString(R.string.already_downloaded) + ": " + absolutePath;
				Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
			}								
			return;
		}		
	}
	
	public static boolean isIntentAvailable(Context ctx, Intent intent) {
		log("isIntentAvailable");
		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
	}	
	public static void log(String log) {
		Util.log("ZipBrowserActivity", log);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {

		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		log("onItemClick");			
	
		ZipEntry currentNode = zipNodes.get(position);		
		currentPath=currentNode.getName();
		
		log("Dentro del comprimida: "+currentPath);
		
		if(currentNode.isDirectory()){		
			depth=depth+1;	
			listDirectory(currentPath);	
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);				
		}
		else{	
			
			int index = pathZip.lastIndexOf("/");
			index = pathZip.lastIndexOf(".");
			String checkFolder = pathZip.substring(0, index);
			log("checkFolder: "+checkFolder);

			File check = new File(checkFolder);
			if(check.exists()){
				log("Ya está descomprimido");
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

	
	private void listDirectory (String directory){
		log("listDirectory: "+directory);
		
		zipNodes.clear();
		
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
	
	public void onBackPressed(){
		
		depth=depth-1;
		
		log("Depth: "+depth);
		
		if(depth<3){
			super.onBackPressed();
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

	@Override
	public void onClick(View v) {

		
	}
	
	private void setFolder(String folder){
		
		String[] parts = folder.split("/");
		if(parts.length>0){
			currentFolder= parts[parts.length-1];							
		}else{
			currentFolder= pathZip;
		}
		
		aB.setTitle("ZIP "+currentFolder);
		log("setFolder: "+currentFolder);
		adapterList.setFolder(currentFolder);
	}

	public String countFiles(String directory){
		log("countFiles: "+directory);
		
		int index = pathZip.lastIndexOf("/");
		String toEliminate = pathZip.substring(0, index+1);
		
		String currentPathCount= currentPath.replace(".zip", "");
		
		currentPathCount= currentPathCount.replace(toEliminate, "");
		if(currentPathCount.lastIndexOf("/")==currentPathCount.length()-1){
			currentPathCount=currentPathCount+directory+"/";
		}
		else{
			currentPathCount=currentPathCount+"/"+directory+"/";
		}
		
		int numFolders=0;
		int numFiles=0;
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
