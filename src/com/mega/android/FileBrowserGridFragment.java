package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserGridFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView gridView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserGridAdapter adapter;
	
	MegaApiAndroid megaApi;
	NodeList nodes;
	
	long parentHandle = -1;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (parentHandle == -1){
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
		
		gridView = (ListView) v.findViewById(R.id.file_grid_view_browser);
        gridView.setOnItemClickListener(null);
        gridView.setItemsCanFocus(false);
        
        emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);
        
		if (adapter == null){
			adapter = new MegaBrowserGridAdapter(context, nodes, parentHandle, gridView, emptyImageView, emptyTextView, aB);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
		}
		
		if (parentHandle == megaApi.getRootNode().getHandle()){
			aB.setTitle(getString(R.string.section_cloud_drive));
		}
		else{
			aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
		}
		
		adapter.setPositionClicked(-1);
		
		gridView.setAdapter(adapter);
		
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
//		Intent i = new Intent(context, FilePropertiesActivity.class);
//		i.putExtra("imageId", items.get(position).getImageId());
//		i.putExtra("name", items.get(position).getName());
//		startActivity(i);
    }
	
	public int onBackPressed(){
		
		parentHandle = adapter.getParentHandle();
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
			if (parentNode != null){
				gridView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
					aB.setTitle(getString(R.string.section_cloud_drive));	
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
				}
				else{
					aB.setTitle(parentNode.getName());					
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
				}
				
				((ManagerActivity)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				nodes = megaApi.getChildren(parentNode);
				adapter.setNodes(nodes);
				adapter.setParentHandle(parentHandle);
				return 2;
			}
			else{
				return 0;
			}
		}
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}
	
	private static void log(String log) {
		Util.log("FileBrowserGridFragment", log);
	}

}
