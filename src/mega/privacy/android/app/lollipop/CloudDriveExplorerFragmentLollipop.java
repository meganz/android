package mega.privacy.android.app.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class CloudDriveExplorerFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	MegaExplorerLollipopAdapter adapter;
	
	int modeCloud;
	boolean selectFile=false;
	MegaPreferences prefs;
	DatabaseHandler dbH;
	
	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	LinearLayout optionsBar;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	TextView optionText;
	TextView cancelText;
	View separator;

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
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
		    modeCloud = bundle.getInt("MODE", FileExplorerActivityLollipop.COPY);	
		    selectFile = bundle.getBoolean("SELECTFILE", false);
		}
		log("onCreate mode: "+modeCloud);
//		first=true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
				
		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(metrics, density);
	    float scaleH = Util.getScaleH(metrics, density);
	    float scaleText;
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
		
		separator = (View) v.findViewById(R.id.separator);
		
		optionsBar = (LinearLayout) v.findViewById(R.id.options_explorer_layout);
		optionText = (TextView) v.findViewById(R.id.action_text);
		optionText.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = optionText.getLayoutParams();		
		paramsb2.height = Util.scaleHeightPx(48, metrics);
		paramsb2.width = Util.scaleWidthPx(73, metrics);
		optionText.setLayoutParams(paramsb2);
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)optionText.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, metrics), 0, Util.scaleWidthPx(8, metrics), 0); 
		optionText.setLayoutParams(optionTextParams);
		
		cancelText = (TextView) v.findViewById(R.id.cancel_text);
		cancelText.setOnClickListener(this);		
		cancelText.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));
		android.view.ViewGroup.LayoutParams paramsb1 = cancelText.getLayoutParams();		
		paramsb1.height = Util.scaleHeightPx(48, metrics);
		paramsb1.width = Util.scaleWidthPx(73, metrics);
		cancelText.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)cancelText.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, metrics), 0, Util.scaleWidthPx(8, metrics), 0); 
		cancelText.setLayoutParams(cancelTextParams);		
				
		listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		
		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);
		
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
		
		if(modeCloud==FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			parentHandle = -1;
		}
		else{
			if (parentHandle == -1)
			{			
				//Find in the database the last parentHandle
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(context);
				}
				if (dbH != null){
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
			}
		}		
		
		MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);
		if(chosenNode == null)
		{
			log("chosenNode is NULL");
		
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
			changeActionBarTitle(context.getString(R.string.section_cloud_drive));
		}
		else if(chosenNode.getType() == MegaNode.TYPE_ROOT)
		{
			log("chosenNode is ROOT");
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(chosenNode);
			changeActionBarTitle(context.getString(R.string.section_cloud_drive));
		}
		else {
			log("ChosenNode not null and not ROOT");
			
			MegaNode parentNode = megaApi.getParentNode(chosenNode);			
			if(parentNode!=null){
				log("ParentNode NOT NULL");
				MegaNode grandParentNode = megaApi.getParentNode(parentNode);
				while(grandParentNode!=null){
					parentNode=grandParentNode;
					grandParentNode = megaApi.getParentNode(parentNode);
				}
				if(parentNode.getType() == MegaNode.TYPE_ROOT){
					nodes = megaApi.getChildren(chosenNode);
					changeActionBarTitle(chosenNode.getName());
					log("chosenNode is: "+chosenNode.getName());
				}
				else{
					log("Parent node exists but is not Cloud!");
					parentHandle = megaApi.getRootNode().getHandle();
					nodes = megaApi.getChildren(megaApi.getRootNode());
					changeActionBarTitle(context.getString(R.string.section_cloud_drive));
				}
				
			}
			else{
				log("parentNode is NULL");
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode());
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
			}		
			
		}
		
		if (context instanceof FileExplorerActivityLollipop){
			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
		}		
		
		if (adapter == null){
			adapter = new MegaExplorerLollipopAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, selectFile);			
			
			adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {
				
				@Override
				public void onItemClick(View view, int position) {
					itemClick(view, position);
				}
			});
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
			adapter.setSelectFile(selectFile);
		}
		
		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionText.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));			
		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionText.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));	
		}
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionText.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));	
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionText.setText(getString(R.string.general_import).toUpperCase(Locale.getDefault()));	
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionText.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));	
		}
		else if(modeCloud == FileExplorerActivityLollipop.UPLOAD_SELFIE){
			optionText.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}	
		else {
			optionText.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));	
		}	
		
		if(selectFile)
		{
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
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
		((FileExplorerActivityLollipop) context).changeTitle(folder);
	}
	
//	public void setBackVisibility(boolean backVisibility){
//		((FileExplorerActivityLollipop) context).setBackVisibility(backVisibility);
//	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.action_text:{				
				dbH.setLastCloudFolder(Long.toString(parentHandle));
				((FileExplorerActivityLollipop) context).buttonClick(parentHandle);
			}
			case R.id.cancel_text:{				
				((FileExplorerActivityLollipop) context).finish();
			}
		}
	}

    public void itemClick(View view, int position) {
		log("itemClick");
		
		if (nodes.get(position).isFolder()){
					
			MegaNode n = nodes.get(position);			
			
			String path=n.getName();	
			String[] temp;
			temp = path.split("/");
			name = temp[temp.length-1];

			if(n.getType() != MegaNode.TYPE_ROOT)
			{
				changeActionBarTitle(name);
			}
			else
			{
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
			}
			
			parentHandle = nodes.get(position).getHandle();
			if (context instanceof FileExplorerActivityLollipop){
				((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
			}
			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(nodes.get(position));
			adapter.setNodes(nodes);
			listView.scrollToPosition(0);
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
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
		else
		{
			//Is file
			if(selectFile)
			{
				//Send file
				MegaNode n = nodes.get(position);
				log("Selected node to send: "+n.getName());
				if(nodes.get(position).isFile()){
					MegaNode nFile = nodes.get(position);
					((FileExplorerActivityLollipop) context).buttonClick(nFile.getHandle());
				}
				
			}
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
			}
			else{
				String path=parentNode.getName();	
				String[] temp;
				temp = path.split("/");
				name = temp[temp.length-1];

				changeActionBarTitle(name);
				
				parentHandle = parentNode.getHandle();
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);			
			
			nodes = megaApi.getChildren(parentNode);
			adapter.setNodes(nodes);
			listView.scrollToPosition(0);
			adapter.setParentHandle(parentHandle);
			if (context instanceof FileExplorerActivityLollipop){
				((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
			}
			
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
		Util.log("CloudDriveExplorerFragmentLollipop", log);
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		
		if (context instanceof FileExplorerActivityLollipop){
			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
		}
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
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
	
	public RecyclerView getListView(){
		return listView;
	}
}
