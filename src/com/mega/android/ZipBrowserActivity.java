package com.mega.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.mega.sdk.MegaNode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ZipBrowserActivity extends PinActivity implements OnClickListener, OnItemClickListener, OnItemLongClickListener{
	
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	
	ListView listView;
	ZipListAdapter adapterList;
	ActionBar aB;
//	List<String> filePaths;
	ZipFile myZipFile;
	String pathZip;
	List<ZipEntry> zipNodes;
	String currentFolder;
	String currentPath;
	int depth;
	
	public void onCreate (Bundle savedInstanceState){
		
		log("onCreate");
		
		super.onCreate(savedInstanceState);
		
		depth=3;
		
		zipNodes = new ArrayList<ZipEntry>();
			
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			pathZip = extras.getString("PATH_ZIP");
		}
		currentPath = pathZip;
		
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
			log("Paso 1: " +currentFolder);
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
				
				log("Elemeto: " +element.getName());
				
				if(element.isDirectory()){
					log("Directorio");
					if(!element.getName().equals(currentFolder+"/")){
						log("Remove Comprimida");
						String[] pE = element.getName().split("/");
						if(pE.length<depth){
							log("Anado: " +element.getName());
							zipNodes.add(element);
						}
					}
					
				}
				else{
					
					log("Fichero");
					String[] pE = element.getName().split("/");
					if(pE.length==depth-1){
						log("Anado: " +element.getName());
						zipNodes.add(element);
					}
				}				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
				
		if (adapterList == null){
			adapterList = new ZipListAdapter(this, listView, aB, zipNodes, currentFolder);
			
		}
		else{
			//adapterList.setParentHandle(parentHandle);
			//adapterList.setNodes(nodes);
		}
		

		//adapterList.setPositionClicked(-1);
		//adapterList.setMultipleSelect(false);

		listView.setAdapter(adapterList);		
		
	}
	
	public static void log(String log) {
		Util.log("ZipBrowserActivity", log);
	}


	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		log("onItemClick");
		
		depth=depth+1;
		
	
		ZipEntry currentNode = zipNodes.get(position);
		
		
		currentPath=currentNode.getName();
		
		
		log("-----currentPath:"+ currentPath);		
		
		
		if(currentNode.isDirectory()){
			
			
			listDirectory(currentPath);		
			
			
		}
		else{
			//Unzip todo para verlo
			log("Fichero");
		}
		
		this.setFolder(currentPath);
		adapterList.setNodes(zipNodes);
		
//		String[] parts = pathZip.split("/");
//		if(parts.length>0){
//
//			currentFolder= parts[parts.length-1];
//			log("Paso 1: " +currentFolder);
//			parts = currentFolder.split(".");
//			
//			currentFolder= currentFolder.replace(".zip", "");
//			
//		}else{
//			currentFolder= pathZip;
//		}
//				
//		try {
//			myZipFile = new ZipFile(pathZip);
//			
//			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
//			while (zipEntries.hasMoreElements()) {
//				
//				ZipEntry element = zipEntries.nextElement();	
//				
//				log("Elemeto: " +element.getName());
//				
//				if(element.isDirectory()){
//					log("Directorio");
//					if(!element.getName().equals(currentFolder+"/")){
//						log("Remove Comprimida");
//						String[] pE = element.getName().split("/");
//						if(pE.length<3){
//							log("Anado: " +element.getName());
//							zipNodes.add(element);
//						}
//					}
//					
//				}
//				else{
//					
//					log("Fichero");
//					String[] pE = element.getName().split("/");
//					if(pE.length==2){
//						log("Anado: " +element.getName());
//						zipNodes.add(element);
//					}
//				}				
//				
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 	
//		
		
	}
	
	private void listDirectory (String directory){
		
		zipNodes.removeAll(zipNodes);
		
		Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
		while (zipEntries.hasMoreElements()) {
			
			ZipEntry element = zipEntries.nextElement();	
			
			log("Elemento: " +element.getName());
			
			if(element.getName().startsWith(directory)){
				
				log("Entro");
				if(element.isDirectory()){
					log("Directorio");
					
					if(!element.getName().equals(directory)){
						log("No soy la propia carpeta");
						String[] pE = element.getName().split("/");
						log("Tam pE:"+pE.length+" depth:" +depth);
						if(pE.length<depth){
							log("Anado: " +element.getName());
							zipNodes.add(element);
						}
					}
										
					
				}
				else{
					log("Fichero");											
					
					String[] pE = element.getName().split("/");
					log("Tam pE:"+pE.length+" depth:" +depth);
					if(pE.length<depth){
						log("Anado: " +element.getName());
						zipNodes.add(element);
					}
					
				}
			}
		}
		
		
	}
	
	public void onBackPressed(){
		
		depth=depth-1;
		
		log("CurrentPath: "+currentPath);
		
		int index = currentPath.lastIndexOf("/");
		
		currentPath=currentPath.substring(0, currentPath.length()-1);
		
		index = currentPath.lastIndexOf("/");
		
		currentPath = currentPath.substring(0, index+1);
		
		log("New----CurrentPath: "+currentPath);
		
		
		listDirectory(currentPath);		
		//
		this.setFolder(currentPath);
		adapterList.setNodes(zipNodes);
		

	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private void setFolder(String folder){
		
		String[] parts = folder.split("/");
		if(parts.length>0){

			currentFolder= parts[parts.length-1];
			log("Paso 1: " +currentFolder);
			
						
		}else{
			currentFolder= pathZip;
		}
		
		aB.setTitle("ZIP "+currentFolder);
		log("setFolder: "+currentFolder);
		adapterList.setFolder(currentFolder);
	}

}
