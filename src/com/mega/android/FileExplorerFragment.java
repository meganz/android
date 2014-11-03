package com.mega.android;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;

public class FileExplorerFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	MegaExplorerAdapter adapter;
	
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (parentHandle == -1){
			if (megaApi.getRootNode() == null){
				return;
			}			
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		
		listView = (ListView) v.findViewById(R.id.file_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
		
		if (adapter == null){
			adapter = new MegaExplorerAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
		}
		
		adapter.setPositionClicked(-1);
		
		listView.setAdapter(adapter);		
		
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
		
		if (nodes.get(position).isFolder()){
			MegaNode n = nodes.get(position);
			
			parentHandle = nodes.get(position).getHandle();
			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(nodes.get(position));
			adapter.setNodes(nodes);
			listView.setSelection(0);
			
			//If folder has no files
			if (adapter.getCount() == 0){
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
	}
	
	public int onBackPressed(){
		
		parentHandle = adapter.getParentHandle();
		
		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
		if (parentNode != null){
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			
			parentHandle = parentNode.getHandle();
			nodes = megaApi.getChildren(parentNode);
			adapter.setNodes(nodes);
			listView.setSelection(0);
			adapter.setParentHandle(parentHandle);
			
			return 2;
		}
		else{
			return 0;
		}
	}
	
	/*
	 * Disable nodes from the list
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		adapter.setDisableNodes(disabledNodes);
	}
	
	private static void log(String log) {
		Util.log("FileExplorerFragment", log);
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
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getCount() == 0){
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
	
	public ListView getListView(){
		return listView;
	}
}
