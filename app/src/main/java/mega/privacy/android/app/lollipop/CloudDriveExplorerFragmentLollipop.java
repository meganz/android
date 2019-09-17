package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
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
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	Button optionButton;
	Button cancelButton;
	View separator;

	ArrayList<Long> nodeHandleMoveCopy;

	Stack<Integer> lastPositionStack;

	Handler handler;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

			if(modeCloud==FileExplorerActivityLollipop.SELECT){
				if(selectFile) {
					if (((FileExplorerActivityLollipop) context).multiselect) {
						activateButton(true);
					}
				}
			}
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();

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
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_explorer_multiaction, menu);
			changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			if (selected.size() != 0) {
				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				MegaNode nodeS = megaApi.getNodeByHandle(parentHandle);

				if(selected.size() == megaApi.getNumChildFiles(nodeS)){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

				}else{
					if(modeCloud==FileExplorerActivityLollipop.SELECT){
						if(selectFile){
							if(((FileExplorerActivityLollipop)context).multiselect){
								MegaNode node = megaApi.getNodeByHandle(parentHandle);
								if(selected.size() == megaApi.getNumChildFiles(node)){
									menu.findItem(R.id.cab_menu_select_all).setVisible(false);
								}else{
									menu.findItem(R.id.cab_menu_select_all).setVisible(true);
								}
							}
						}
					}else{
						menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					}

					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

//					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//					unselect.setTitle(getString(R.string.action_unselect_all));
//					unselect.setVisible(true);

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
		logDebug("newInstance");
		CloudDriveExplorerFragmentLollipop fragment = new CloudDriveExplorerFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		logDebug("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		if (megaApi.getRootNode() == null){
			return;
		}
		
		parentHandle = -1;
		dbH = DatabaseHandler.getDbHandler(context);
		lastPositionStack = new Stack<>();
		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("onCreateView");
				
		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = getScaleW(metrics, density);
	    float scaleH = getScaleH(metrics, density);

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
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if (listView.canScrollVertically(-1)){
					((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
				}
				else {
					((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
				}
			}
		});
		
		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
		emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

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
		if(chosenNode == null) {
			logWarning("chosenNode is NULL");
		
			if(megaApi.getRootNode()!=null){
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode());
			}

		}else if(chosenNode.getType() == MegaNode.TYPE_ROOT) {
			logDebug("chosenNode is ROOT");
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(chosenNode);

		}else {
			logDebug("ChosenNode not null and not ROOT");
			
			MegaNode parentNode = megaApi.getParentNode(chosenNode);
			if(parentNode!=null){
				logDebug("ParentNode NOT NULL");
				MegaNode grandParentNode = megaApi.getParentNode(parentNode);
				while(grandParentNode!=null){
					parentNode=grandParentNode;
					grandParentNode = megaApi.getParentNode(parentNode);
				}
				if(parentNode.getType() == MegaNode.TYPE_ROOT){
					nodes = megaApi.getChildren(chosenNode);
					logDebug("chosenNode is: " + chosenNode.getName());
				}
				else{
					logDebug("Parent node exists but is not Cloud!");
					parentHandle = megaApi.getRootNode().getHandle();
					nodes = megaApi.getChildren(megaApi.getRootNode());
				}
				
			}
			else{
				logWarning("parentNode is NULL");
				parentHandle = megaApi.getRootNode().getHandle();
				nodes = megaApi.getChildren(megaApi.getRootNode());
			}		
			
		}
		
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);


		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));

			MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
			if(parent != null){
				if(parent.getHandle() == chosenNode.getHandle()) {
					activateButton(false);
				}else{
					activateButton(true);
				}
			}else{
				activateButton(true);
			}

			nodeHandleMoveCopy = ((FileExplorerActivityLollipop)context).getNodeHandleMoveCopy();
			setDisableNodes(nodeHandleMoveCopy);

		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));

			MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
			if(parent != null){
				if(parent.getHandle() == chosenNode.getHandle()) {
					activateButton(false);
				}else{
					activateButton(true);
				}
			}else{
				activateButton(true);
			}

		}
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionButton.setText(getString(R.string.add_to_cloud).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
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
					activateButton(false);
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
				logDebug("Mode SELECT FILE ON");
			}

//			if(((FileExplorerActivityLollipop)context).multiselect){
				adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, listView, selectFile);
			logDebug("SetOnItemClickListener");
				adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {

					@Override
					public void onItemClick(View view, int position) {
						logDebug("Item click listener trigger!!");
						itemClick(view, position);
					}
				});
//			}
//			else{
//
//				adapter = new MegaExplorerLollipopAdapter(context, nodes, parentHandle, listView, selectFile);
//				log("SetOnItemClickListener");
//				adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {
//
//					@Override
//					public void onItemClick(View view, int position) {
//						log("item click listener trigger!!");
//						itemClick(view, position);
//					}
//				});
//			}
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

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			} else {
//				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//				emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				}
				String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);
			}

		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);



		}

		return v;
	}
	
//	public void setMode(int mode){
//		log("setMode: "+mode);
//		modeCloud=mode;
//		log("setMode: "+modeCloud);
//	}	


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
		logDebug("onClick");

		switch(v.getId()){
			case R.id.action_text:{
				dbH.setLastCloudFolder(Long.toString(parentHandle));
				if(((FileExplorerActivityLollipop)context).multiselect){
					logDebug("Send several files to chat");
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
				((FileExplorerActivityLollipop) context).finishActivity();
			}
			break;
		}
	}

	public void navigateToFolder(long handle) {
		logDebug("Handle: " + handle);

		int lastFirstVisiblePosition = 0;
		lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

		logDebug("Push to stack " + lastFirstVisiblePosition + " position");
		lastPositionStack.push(lastFirstVisiblePosition);

		parentHandle = handle;

		adapter.setParentHandle(parentHandle);
		nodes.clear();
		adapter.setNodes(nodes);
		listView.scrollToPosition(0);

		((FileExplorerActivityLollipop) context).changeTitle();

		//If folder has no files
		if (adapter.getItemCount() == 0){
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
//			emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//			emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
			}
			String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
			try{
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			}
			catch (Exception e){}
			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}
			emptyTextViewFirst.setText(result);
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.GONE);
		}

		if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
			activateButton(true);
		}
	}

    public void itemClick(View view, int position) {
		logDebug("Position: " + position);

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

			logDebug("Push to stack " + lastFirstVisiblePosition + " position");
			lastPositionStack.push(lastFirstVisiblePosition);
			
//			String path=n.getName();
//			String[] temp;
//			temp = path.split("/");
//			name = temp[temp.length-1];

			if(n.getType() != MegaNode.TYPE_ROOT)
			{
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

			((FileExplorerActivityLollipop) context).changeTitle();
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==n.getHandle()) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);


				} else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
					activateButton(true);
				}

			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){

					MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
					if(parent != null){
						if(parent.getHandle() == parentHandle) {
							activateButton(false);
						}else{
							activateButton(true);
						}
					}else{
						activateButton(true);
					}
				}
			}

		}
		else {
			//Is file
			if(selectFile)
			{
				if(((FileExplorerActivityLollipop)context).multiselect){
					logDebug("Select file and allow multiselection");

					if (adapter.getSelectedItemCount() == 0) {
						logDebug("Activate the actionMode");
						activateActionMode();
						adapter.toggleSelection(position);
						updateActionModeTitle();
					}
					else {
						logDebug("Add to selectedNodes");
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
					logDebug("Selected node to send: " + n.getName());
					if(nodes.get(position).isFile()){
						MegaNode nFile = nodes.get(position);
						((FileExplorerActivityLollipop) context).buttonClick(nFile.getHandle());
					}
				}

			}
			else{
				logWarning("Not select file enabled!");
			}
		}



	}	

	public int onBackPressed(){
		logDebug("onBackPressed");
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

				if(modeCloud==FileExplorerActivityLollipop.SELECT){
					if(!selectFile)
					{
						separator.setVisibility(View.GONE);
						optionsBar.setVisibility(View.GONE);
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

				((FileExplorerActivityLollipop) context).changeTitle();
			}
			else{
//				String path=parentNode.getName();
//				String[] temp;
//				temp = path.split("/");
//				name = temp[temp.length-1];

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
				((FileExplorerActivityLollipop) context).changeTitle();
			}

			if((modeCloud == FileExplorerActivityLollipop.MOVE) || (modeCloud == FileExplorerActivityLollipop.COPY)){
				MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
				if(parent != null){
					if(parent.getHandle() == parentNode.getHandle()) {
						activateButton(false);
					}else{
						activateButton(true);
					}
				}else{
					activateButton(true);

				}
			}

			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			nodes = megaApi.getChildren(parentNode);
			adapter.setNodes(nodes);
			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				logDebug("Pop of the stack " + lastVisiblePosition + " position");
			}
			logDebug("Scroll to " + lastVisiblePosition + " position");

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
		logDebug("Disabled nodes: " + disabledNodes.size());
		if (adapter == null){
			logWarning("Adapter is NULL");
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, listView, selectFile);

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

	public long getParentHandle(){
		logDebug("getParentHandle");
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		logDebug("Parent handle: " + parentHandle);
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		((FileExplorerActivityLollipop)context).setParentHandle(parentHandle);
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		logDebug("Nodes: " + nodes.size());
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==parentHandle) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				} else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
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
		logDebug("selectAll");
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
		logDebug("updateActionModeTitle");

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
			logError("Invalidate error", e);
			e.printStackTrace();
		}
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		logDebug("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		adapter.clearSelectedItems();
		if (actionMode != null) {
			actionMode.finish();
		}

		if(modeCloud==FileExplorerActivityLollipop.SELECT){
			if(selectFile) {
				if (((FileExplorerActivityLollipop) context).multiselect) {
					activateButton(false);
				}
			}
		}

	}

	public RecyclerView getListView(){
		return listView;
	}

	public void activateButton(boolean show){
		optionButton.setEnabled(show);
		if(show){
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
		}else{
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
		}
	}

}
