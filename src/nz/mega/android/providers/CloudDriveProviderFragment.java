package nz.mega.android.providers;

import java.util.ArrayList;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.ManagerActivity;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toast;


public class CloudDriveProviderFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	MegaProviderAdapter adapter;
	
	MegaPreferences prefs;
	DatabaseHandler dbH;
	
	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	
	long [] hashes;

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
	
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		if (megaApi.getRootNode() == null){
			return;
		}
		
		parentHandle = -1;
		dbH = DatabaseHandler.getDbHandler(context);		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
				
		View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);		
		
		listView = (ListView) v.findViewById(R.id.provider_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		
		contentText = (TextView) v.findViewById(R.id.provider_content_text);
		contentText.setVisibility(View.GONE);
		
		emptyImageView = (ImageView) v.findViewById(R.id.provider_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.provider_list_empty_text);
	
		if (parentHandle == -1)
		{			
			//Find in the database the last parentHandle
			prefs = dbH.getPreferences();
			if (prefs != null) {
				String lastFolder = prefs.getLastFolderCloud();
				if(lastFolder != null) {
					if (lastFolder.compareTo("") != 0){
						parentHandle = Long.parseLong(lastFolder);
					}
				}
			}
		}
			
		
		MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);
		if(chosenNode == null)
		{
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
			changeActionBarTitle(context.getString(R.string.section_cloud_drive));
			changeBackVisibility(false);
		}
		else
		{
			nodes = megaApi.getChildren(chosenNode);
			if(chosenNode.getType() != MegaNode.TYPE_ROOT)
			{
				changeActionBarTitle(chosenNode.getName());	
				changeBackVisibility(true);
			}
			else
			{
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
				changeBackVisibility(false);
			}
		}
		
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setParentHandle(parentHandle);
		}
		
//		if (modeCloud == FileExplorerActivity.MOVE) {
//			uploadButton.setText(getString(R.string.general_move_to) + " " + actionBarTitle );
//		}
//		else if (modeCloud == FileExplorerActivity.COPY){
//			uploadButton.setText(getString(R.string.general_copy_to) + " " + actionBarTitle );
//		}
//		else if (modeCloud == FileExplorerActivity.UPLOAD){
//			uploadButton.setText(getString(R.string.action_upload));
//		}
//		else if (modeCloud == FileExplorerActivity.IMPORT){
//			uploadButton.setText(getString(R.string.general_import_to) + " " + actionBarTitle );
//		}
//		else if (modeCloud == FileExplorerActivity.SELECT){
//			uploadButton.setText(getString(R.string.general_select) + " " + actionBarTitle );
//		}
//		else if(modeCloud == FileExplorerActivity.UPLOAD_SELFIE){
//			uploadButton.setText(getString(R.string.action_upload) + " " + actionBarTitle );
//		}	
//				
		if (adapter == null){
			adapter = new MegaProviderAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
		}
		
		adapter.setPositionClicked(-1);		
		
		listView.setAdapter(adapter);		
		
		return v;
	}
	
//	public void setMode(int mode){
//		log("setMode: "+mode);
//		modeCloud=mode;
//		log("setMode: "+modeCloud);
//	}	
	
	
	public void changeActionBarTitle(String folder){
		((FileProviderActivity) context).changeTitle(folder);
	}
	
	public void changeBackVisibility(boolean backVisibility){
		((FileProviderActivity) context).changeBackVisibility(backVisibility);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
//			case R.id.file_explorer_button:{				
//				dbH.setLastCloudFolder(Long.toString(parentHandle));
//				((FileProviderActivity) context).buttonClick(parentHandle);
//			}
		}
	}

	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
		log("onItemClick");
		
		if (nodes.get(position).isFolder()){
					
			MegaNode n = nodes.get(position);			
			
			String path=n.getName();	
			String[] temp;
			temp = path.split("/");
			name = temp[temp.length-1];
		
			parentHandle = nodes.get(position).getHandle();
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setParentHandle(parentHandle);
			}
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
		else{
			//File selected to download
			MegaNode n = nodes.get(position);
			hashes = new long[1];
			hashes[0]=n.getHandle();
			((FileProviderActivity) context).downloadTo(n.getSize(), hashes);
		}
	}	

	public int onBackPressed(){
		log("onBackPressed");
		
		parentHandle = adapter.getParentHandle();
		
		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
		if (parentNode != null){
			
			if(parentNode.getType()==MegaNode.TYPE_ROOT){
				parentHandle=-1;
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
				changeBackVisibility(false);
			}
			else{
				String path=parentNode.getName();	
				String[] temp;
				temp = path.split("/");
				name = temp[temp.length-1];
				changeActionBarTitle(name);
				changeBackVisibility(true);
				
				parentHandle = parentNode.getHandle();
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			
			
			nodes = megaApi.getChildren(parentNode);
			adapter.setNodes(nodes);
			listView.setSelection(0);
			adapter.setParentHandle(parentHandle);
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setParentHandle(parentHandle);
			}
			
			return 2;
		}
		else{
			return 0;
		}
	}
	
	
	private static void log(String log) {
		Util.log("CloudDriveProviderFragment", log);
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setParentHandle(parentHandle);
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
