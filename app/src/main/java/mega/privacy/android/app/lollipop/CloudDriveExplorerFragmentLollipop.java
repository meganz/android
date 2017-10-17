package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class CloudDriveExplorerFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	public long parentHandle = -1;
	
	MegaExplorerLollipopAdapter adapter;
	
	int modeCloud;
	boolean selectFile=false;
	MegaPreferences prefs;
	DatabaseHandler dbH;

	public ActionMode actionMode;
	
//	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	LinearLayout optionsBar;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	Button optionButton;
	Button cancelButton;
	View separator;

	Stack<Integer> lastPositionStack;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();
			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

			switch(item.getItemId()){

				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_explorer_multiaction, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			if (selected.size() != 0) {

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			return false;
		}
	}


	public static CloudDriveExplorerFragmentLollipop newInstance() {
		log("newInstance");
		CloudDriveExplorerFragmentLollipop fragment = new CloudDriveExplorerFragmentLollipop();
		return fragment;
	}

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
		lastPositionStack = new Stack<>();
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

		separator = (View) v.findViewById(R.id.separator);
		
		optionsBar = (LinearLayout) v.findViewById(R.id.options_explorer_layout);
		optionButton = (Button) v.findViewById(R.id.action_text);
		optionButton.setOnClickListener(this);

		cancelButton = (Button) v.findViewById(R.id.cancel_text);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));

		listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		
		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);
		
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);

		modeCloud = ((FileExplorerActivityLollipop)context).getMode();
		selectFile = ((FileExplorerActivityLollipop)context).isSelectFile();

		parentHandle = ((FileExplorerActivityLollipop)context).parentHandleCloud;
		
		if(modeCloud==FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			parentHandle = -1;
		}
		else{
			if (parentHandle == -1)
			{
				//QA Report #6608 - do not remember last folder
				//Find in the database the last parentHandle
//				if (dbH == null){
//					dbH = DatabaseHandler.getDbHandler(context);
//				}
//				if (dbH != null){
//					prefs = dbH.getPreferences();
//					if (prefs != null) {
//
//						String lastFolder = prefs.getLastFolderCloud();
//						if(lastFolder != null) {
//							if (lastFolder.compareTo("") != 0){
//								parentHandle = Long.parseLong(lastFolder);
//							}
//						}
//					}
//				}
				parentHandle = megaApi.getRootNode().getHandle();
			}
		}		
		
		MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);
		if(chosenNode == null)
		{
			log("chosenNode is NULL");
		
			if(megaApi.getRootNode()!=null){
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode());
			}
			
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
		
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);


		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move));
		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionButton.setText(getString(R.string.general_import).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}
		else if(modeCloud == FileExplorerActivityLollipop.UPLOAD_SELFIE){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}	
		else {
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}	

		if(modeCloud==FileExplorerActivityLollipop.SELECT){
			if(selectFile)
			{
				if(((FileExplorerActivityLollipop)context).multiselect){
					separator.setVisibility(View.VISIBLE);
					optionsBar.setVisibility(View.VISIBLE);
					optionButton.setText(getString(R.string.context_send));
				}
				else{
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
			}
			else{
				if(parentHandle==-1||parentHandle==megaApi.getRootNode().getHandle()){
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
				else{
					separator.setVisibility(View.VISIBLE);
					optionsBar.setVisibility(View.VISIBLE);
				}
			}
		}
//		else{
//			if(selectFile)
//			{
//				separator.setVisibility(View.GONE);
//				optionsBar.setVisibility(View.GONE);
//			}
//		}

		if (adapter == null){
			if(selectFile){
				log("Mode SELECT FILE ON");
			}

			if(((FileExplorerActivityLollipop)context).multiselect){

				adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, listView, selectFile);
				log("SetOnItemClickListener");
				adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {

					@Override
					public void onItemClick(View view, int position) {
						log("item click listener trigger!!");
						itemClick(view, position);
					}
				});
			}
			else{
				adapter = new MegaExplorerLollipopAdapter(context, nodes, parentHandle, listView, selectFile);
				log("SetOnItemClickListener");
				adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {

					@Override
					public void onItemClick(View view, int position) {
						log("item click listener trigger!!");
						itemClick(view, position);
					}
				});
			}
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
			adapter.setSelectFile(selectFile);
		}

		adapter.setPositionClicked(-1);		
		
		listView.setAdapter(adapter);

		//If folder has no files
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

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		
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
//		((LauncherFileExplorerActivity) context).setBackVisibility(backVisibility);
//	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch(v.getId()){
			case R.id.action_text:{
				dbH.setLastCloudFolder(Long.toString(parentHandle));
				if(((FileExplorerActivityLollipop)context).multiselect){
					log("Send several files to chat");
					if(adapter.getSelectedItemCount()>0){
						long handles[] = adapter.getSelectedHandles();
						((FileExplorerActivityLollipop) context).buttonClick(handles);
					}
					else{
						((FileExplorerActivityLollipop) context).showSnackbar(getString(R.string.no_files_selected_warning));
					}

				}
				else{
					((FileExplorerActivityLollipop) context).buttonClick(parentHandle);
				}
				break;
			}
			case R.id.cancel_text:{
				((FileExplorerActivityLollipop) context).finish();
			}
			break;
		}
	}

	public void navigateToFolder(long handle) {
		log("navigateToFolder");

		int lastFirstVisiblePosition = 0;
		lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

		log("Push to stack "+lastFirstVisiblePosition+" position");
		lastPositionStack.push(lastFirstVisiblePosition);

		parentHandle = handle;

		MegaNode parentNode = megaApi.getNodeByHandle(handle);
		changeActionBarTitle(parentNode.getName());

		adapter.setParentHandle(parentHandle);
		nodes.clear();
		adapter.setNodes(nodes);
		listView.scrollToPosition(0);

		//If folder has no files
		if (adapter.getItemCount() == 0){
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			emptyImageView.setImageResource(R.drawable.ic_empty_folder);
			emptyTextView.setText(R.string.file_browser_empty_folder);
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}

    public void itemClick(View view, int position) {
		log("itemClick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (nodes.get(position).isFolder()){

			if(selectFile) {
				if(((FileExplorerActivityLollipop)context).multiselect){
					if(adapter.isMultipleSelect()){
						hideMultipleSelect();
					}
				}
			}

			MegaNode n = nodes.get(position);

			int lastFirstVisiblePosition = 0;
			lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

			log("Push to stack "+lastFirstVisiblePosition+" position");
			lastPositionStack.push(lastFirstVisiblePosition);
			
//			String path=n.getName();
//			String[] temp;
//			temp = path.split("/");
//			name = temp[temp.length-1];

			if(n.getType() != MegaNode.TYPE_ROOT)
			{
				changeActionBarTitle(n.getName());
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.VISIBLE);
						optionsBar.setVisibility(View.VISIBLE);

					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}

					}
				}
			}
			else
			{
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
			}
			
			parentHandle = nodes.get(position).getHandle();

			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);

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
				if(((FileExplorerActivityLollipop)context).multiselect){
					log("select file and allow multiselection");

					if (adapter.getSelectedItemCount() == 0) {
						log("activate the actionMode");
						activateActionMode();
						adapter.toggleSelection(position);
						updateActionModeTitle();
					}
					else {
						log("add to selectedNodes");
						adapter.toggleSelection(position);

						List<MegaNode> selectedNodes = adapter.getSelectedNodes();
						if (selectedNodes.size() > 0){
							updateActionModeTitle();
						}
					}

				}
				else{
					//Send file
					MegaNode n = nodes.get(position);
					log("Selected node to send: "+n.getName());
					if(nodes.get(position).isFile()){
						MegaNode nFile = nodes.get(position);
						((FileExplorerActivityLollipop) context).buttonClick(nFile.getHandle());
					}
				}
			}
			else{
				log("Not select file enabled!");
			}
		}
	}	

	public int onBackPressed(){
		log("onBackPressed");

		if(selectFile) {
			if(((FileExplorerActivityLollipop)context).multiselect){
				if(adapter.isMultipleSelect()){
					hideMultipleSelect();
				}
			}
		}
		
		parentHandle = adapter.getParentHandle();
		
		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
		if (parentNode != null){
			
			if(parentNode.getType()==MegaNode.TYPE_ROOT){
				parentHandle=-1;
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.VISIBLE);
						optionsBar.setVisibility(View.VISIBLE);
					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}
					}
				}
			}
			else{
//				String path=parentNode.getName();
//				String[] temp;
//				temp = path.split("/");
//				name = temp[temp.length-1];

				changeActionBarTitle(parentNode.getName());
				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.VISIBLE);
						optionsBar.setVisibility(View.VISIBLE);
					}
					else
					{
						if(((FileExplorerActivityLollipop)context).multiselect){
							separator.setVisibility(View.VISIBLE);
							optionsBar.setVisibility(View.VISIBLE);
							optionButton.setText(getString(R.string.context_send));
						}
						else{
							separator.setVisibility(View.GONE);
							optionsBar.setVisibility(View.GONE);
						}

					}
				}
				parentHandle = parentNode.getHandle();
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);			
			
			nodes = megaApi.getChildren(parentNode);
			adapter.setNodes(nodes);
			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				log("Pop of the stack "+lastVisiblePosition+" position");
			}
			log("Scroll to "+lastVisiblePosition+" position");

			if(lastVisiblePosition>=0){
				mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
			}
			adapter.setParentHandle(parentHandle);
			((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);

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
		log("setDisableNodes");
		if (adapter == null){
			log("Adapter is NULL");
			adapter = new MegaExplorerLollipopAdapter(context, nodes, parentHandle, listView, selectFile);

			adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {

				@Override
				public void onItemClick(View view, int position) {
					itemClick(view, position);
				}
			});
		}
//		else{
//			adapter.setParentHandle(parentHandle);
//			adapter.setNodes(nodes);
//			adapter.setSelectFile(selectFile);
//		}
		adapter.setDisableNodes(disabledNodes);
		adapter.setSelectFile(selectFile);
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
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
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

	public void selectAll(){
		log("selectAll");
		if (adapter != null){
			adapter.selectAll();

			updateActionModeTitle();
		}
	}

	public boolean isFolder(int position){
		MegaNode node = nodes.get(position);
		if(node.isFolder()){
			return true;
		}
		else{
			return false;
		}
	}

	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}

	private void updateActionModeTitle() {
		log("updateActionModeTitle");

		List<MegaNode> documents = adapter.getSelectedNodes();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}


		Resources res = getActivity().getResources();

		String title;
		int sum=files+folders;

		if (files == 0 && folders == 0) {
			title = Integer.toString(sum);
		} else if (files == 0) {
			title = Integer.toString(folders);
		} else if (folders == 0) {
			title = Integer.toString(files);
		} else {
			title = Integer.toString(sum);
		}
		actionMode.setTitle(title);


		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		adapter.clearSelectedItems();
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public RecyclerView getListView(){
		return listView;
	}
}
