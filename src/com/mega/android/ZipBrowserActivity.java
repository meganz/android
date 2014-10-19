package com.mega.android;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.android.pdfViewer.OpenPDFActivity;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;

public class ZipBrowserActivity extends PinActivity implements OnClickListener, OnItemClickListener, OnItemLongClickListener{
	
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_HANDLE_ZIP ="HANDLE_ZIP";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String ACTION_OPEN_ZIP_FILE = "OPEN_ZIP_FILE";

	
    private AlertDialog zipAlertDialog;
    MegaApplication app;
    private MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;
    
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
	
	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();
	}
	
	@Override
	protected void onPause(){
		log("onPause");
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		log("onDestroy");
		super.onDestroy();
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
		
		depth=depth+1;		
	
		ZipEntry currentNode = zipNodes.get(position);		
		currentPath=currentNode.getName();
		
		log("Dentro del comprimida: "+currentPath);
		
		if(currentNode.isDirectory()){		
			
			listDirectory(currentPath);	
			this.setFolder(currentPath);
			adapterList.setNodes(zipNodes);				
		}
		else{		
			this.setLocationDownload();
			
			//Unzip the file
			this.unpackZip();
			
			//Open the file
			
			//TODO: open file		
					
			
		}
		
			
	}

	
	private void listDirectory (String directory){
		
		zipNodes.removeAll(zipNodes);
		
		Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
		while (zipEntries.hasMoreElements()) {
			
			ZipEntry element = zipEntries.nextElement();	
			
			if(element.getName().startsWith(directory)){
				
				if(element.isDirectory()){
					log("Directorio");
					
					if(!element.getName().equals(directory)){
						String[] pE = element.getName().split("/");
						if(pE.length<depth){
							zipNodes.add(element);
						}
					}
				}
				else{			
					
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
		
		if(depth<3){
			super.onBackPressed();
		}
		else{
			
			if(currentPath==null || currentPath.length()==0){
				currentPath=pathZip;
				depth=3;
				listDirectory(currentPath);				
			}
			else{
				int index = currentPath.lastIndexOf("/");		
				currentPath=currentPath.substring(0, currentPath.length()-1);		
				index = currentPath.lastIndexOf("/");		
				currentPath = currentPath.substring(0, index+1);
				
				listDirectory(currentPath);	
			}			

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
	
	public void setLocationDownload(){
		log("setLocationDownload");
		
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
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
	}	
	
	private boolean unpackZip()
	{       
		MegaNode tempNode = megaApi.getNodeByHandle(handleZip);
		String absolutePath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), downloadLocationDefaultPath);

		log("LocalPAth para zip: "+absolutePath);
		log("NAme para zip: "+tempNode.getName());		

		int index = absolutePath.lastIndexOf("/");

		String destination = absolutePath.substring(0, index+1);

		index = absolutePath.lastIndexOf(".");

		String checkFolder = absolutePath.substring(0, index);

		log("checkFolder: "+checkFolder);

		File check = new File(checkFolder);
		if(check.exists()){
			log("Ya está descomprimido");
			return true;

		}
		else{
			try 
			{
				FileInputStream fin = new FileInputStream(absolutePath);
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
		}
		return true;
	}
}
