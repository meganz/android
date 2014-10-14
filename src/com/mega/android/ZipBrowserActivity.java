package com.mega.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
	
	public void onCreate (Bundle savedInstanceState){
		
		log("onCreate");
		
		super.onCreate(savedInstanceState);
		
		zipNodes = new ArrayList<ZipEntry>();
			
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			pathZip = extras.getString("PATH_ZIP");
		}
		
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
		
		try {
			myZipFile = new ZipFile(pathZip);
			
			Enumeration<? extends ZipEntry> zipEntries = myZipFile.entries();
			while (zipEntries.hasMoreElements()) {	
				
				
				zipNodes.add(zipEntries.nextElement());
				// you can do what ever you want on each zip file

				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
		
		if (adapterList == null){
			adapterList = new ZipListAdapter(this, listView, aB, zipNodes);
			
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
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
