package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.opengl.Visibility;
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
import android.widget.ImageButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileBrowserFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserListAdapter adapterList;
	MegaBrowserGridAdapter adapterGrid;
	
	MegaApiAndroid megaApi;
		
	List<ItemFileBrowser> rowItems;
	
	long parentHandle = -1;
	boolean isList = true;
	
	NodeList nodes;
	//HASTA AQUI 
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		rowItems = new ArrayList<ItemFileBrowser>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		if (parentHandle == -1){
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode);
			
			if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
				aB.setTitle(getString(R.string.section_cloud_drive));	
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			}
			else{
				aB.setTitle(parentNode.getName());					
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
			}
			((ManagerActivity)context).supportInvalidateOptionsMenu();
		}	
		
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
			listView.setOnItemClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			
			if (adapterList == null){
				adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterList.setParentHandle(parentHandle);
				adapterList.setNodes(nodes);
			}
			
			if (parentHandle == megaApi.getRootNode().getHandle()){
				aB.setTitle(getString(R.string.section_cloud_drive));
			}
			else{
				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
			}
			
			adapterList.setPositionClicked(-1);

			listView.setAdapter(adapterList);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.file_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);
	        
			if (adapterGrid == null){
				adapterGrid = new MegaBrowserGridAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
				adapterGrid.setNodes(nodes);
			}
			
			if (parentHandle == megaApi.getRootNode().getHandle()){
				aB.setTitle(getString(R.string.section_cloud_drive));
			}
			else{
				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
			}
			
			adapterGrid.setPositionClicked(-1);
			
			listView.setAdapter(adapterGrid);
			
			return v;
		}		
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
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (isList){
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				adapterList.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position));
				adapterList.setNodes(nodes);
				listView.setSelection(0);
				
				//If folder has no files
				if (adapterList.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRootNode().getHandle()==n.getHandle()) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeType.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					startActivity(intent);
				}
				else{
					adapterList.setPositionClicked(-1);
					adapterList.notifyDataSetChanged();
					((ManagerActivity) context).onFileClick(nodes.get(position));
				}
			}
		}
    }
	
	public int onBackPressed(){

		if (isList){
			parentHandle = adapterList.getParentHandle();
			
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
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
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);
					return 2;
				}
				else{
					return 0;
				}
			}
		}
		else{
			parentHandle = adapterGrid.getParentHandle();
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
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
					adapterGrid.setNodes(nodes);
					listView.setSelection(0);
					adapterGrid.setParentHandle(parentHandle);
					return 2;
				}
				else{
					return 0;
				}
			}
		}
	}
	
	public long getParentHandle(){
		if (isList){
			return adapterList.getParentHandle();
		}
		else{
			return adapterGrid.getParentHandle();
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (isList){
			if (adapterGrid != null){
				adapterGrid.setParentHandle(parentHandle);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setParentHandle(parentHandle);
			}
		}
	}
	
	public ListView getListView(){
		return listView;
	}
	
	public void setNodes(NodeList nodes){
		this.nodes = nodes;
		if (isList){
			if (adapterList != null){
				adapterList.setNodes(nodes);
				if (adapterList.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRootNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setNodes(nodes);
				if (adapterGrid.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRootNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}
		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setPositionClicked(positionClicked);
			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	private static void log(String log) {
		Util.log("FileBrowserFragment", log);
	}

}
