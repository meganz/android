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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


public class IncomingSharesExplorerFragmentLollipop extends Fragment implements OnClickListener{

	private DisplayMetrics outMetrics;
	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
	ArrayList<MegaNode> searchNodes = null;

	long parentHandle = -1;
	
	MegaExplorerLollipopAdapter adapter;
    private FastScroller fastScroller;
	
	int modeCloud;
	boolean selectFile;

	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	View separator;
	Button optionButton;
	Button cancelButton;
	LinearLayout optionsBar;

	Stack<Integer> lastPositionStack;

	Handler handler;
	public ActionMode actionMode;

	int orderParent = megaApi.ORDER_DEFAULT_ASC;
	int order = megaApi.ORDER_DEFAULT_ASC;

	public NewHeaderItemDecoration headerItemDecoration;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

			if(isMultiselect()){
				activateButton(true);
			}
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
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
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_explorer_multiaction, menu);
			Util.changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			Util.changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			if (selected.size() != 0) {
				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				MegaNode nodeS = megaApi.getNodeByHandle(parentHandle);

				if(selected.size() == megaApi.getNumChildFiles(nodeS)){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

				}else{
					if(isMultiselect()){
						MegaNode node = megaApi.getNodeByHandle(parentHandle);
						if(selected.size() == megaApi.getNumChildFiles(node)){
							menu.findItem(R.id.cab_menu_select_all).setVisible(false);
						}else{
							menu.findItem(R.id.cab_menu_select_all).setVisible(true);
						}
					}else{
						menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					}

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

	public static IncomingSharesExplorerFragmentLollipop newInstance() {
		log("newInstance");
		IncomingSharesExplorerFragmentLollipop fragment = new IncomingSharesExplorerFragmentLollipop();
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

		lastPositionStack = new Stack<>();

		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	public void checkScroll() {
		if (recyclerView == null) {
			return;
		}
		if (recyclerView.canScrollVertically(-1)){
			((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
		}
		else {
			((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");

		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
		
		separator = (View) v.findViewById(R.id.separator);
		
		optionsBar = (LinearLayout) v.findViewById(R.id.options_explorer_layout);

		optionButton = (Button) v.findViewById(R.id.action_text);
		optionButton.setOnClickListener(this);

		cancelButton = (Button) v.findViewById(R.id.cancel_text);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));

		fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);
		if (((FileExplorerActivityLollipop) context).isList) {
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			v.findViewById(R.id.file_grid_view_browser).setVisibility(View.GONE);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
		}
		else {
			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			v.findViewById(R.id.file_list_view_browser).setVisibility(View.GONE);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
		}

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);

			}
		});

		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
		emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);
		parentHandle = ((FileExplorerActivityLollipop)context).parentHandleIncoming;

		modeCloud = ((FileExplorerActivityLollipop)context).getMode();
		selectFile = ((FileExplorerActivityLollipop)context).isSelectFile();

		MegaPreferences prefs = Util.getPreferences(context);

		if(prefs != null) {
			if (prefs.getPreferredSortOthers()!=null) {
				orderParent = Integer.parseInt(prefs.getPreferredSortOthers());
			}
			if (prefs.getPreferredSortCloud() != null) {
				order = Integer.parseInt(prefs.getPreferredSortCloud());
			}
		}

		getNodes();

		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		if (adapter == null){
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, recyclerView, selectFile);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setSelectFile(selectFile);
		}
		adapter.setNodes(nodes);
		adapter.setPositionClicked(-1);
		recyclerView.setAdapter(adapter);
		fastScroller.setRecyclerView(recyclerView);

		findDisabledNodes();

		
		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));

			if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
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
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionButton.setText(getString(R.string.add_to_cloud).toUpperCase(Locale.getDefault()));
		}
		else if (isMultiselect()) {
			optionButton.setText(getString(R.string.context_send));
			if (adapter != null && adapter.getSelectedItemCount() > 0){
				activateButton(true);
			}
			else {
				activateButton(false);
			}
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}
		else{
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}



		log("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		setOptionsBarVisibility();
		showEmptyScreen();

		return v;
	}

	private void setOptionsBarVisibility() {
		if (!isMultiselect() && (((FileExplorerActivityLollipop)context).deepBrowserTree <= 0 || selectFile)){
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
		}
		else{
			separator.setVisibility(View.VISIBLE);
			optionsBar.setVisibility(View.VISIBLE);
		}
	}

	private void getNodes() {
		if (parentHandle == -1){
			findNodes();
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode, order);
		}
	}

	private void showEmptyScreen() {
		if (adapter == null) {
			return;
		}

		if (adapter.getItemCount() != 0){
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);

		}else{
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			recyclerView.setVisibility(View.GONE);
			if (parentHandle==-1) {
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
	}
	
	public void findNodes(){
		log("findNodes");
		((FileExplorerActivityLollipop)context).setDeepBrowserTree(0);

		setOptionsBarVisibility();

		ArrayList<MegaUser> contacts = megaApi.getContacts();
		nodes.clear();
		for (int i=0;i<contacts.size();i++){			
			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
			if(nodeContact!=null && !nodeContact.isEmpty()){
				nodes.addAll(nodeContact);
				if (orderParent  == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByMailDescending(nodes);
				}
			}
		}
	}

	public void sortByMailDescending(ArrayList<MegaNode> nodes){
		log("sortByNameDescending");
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();

		for (int i=0;i<nodes.size();i++){
			if(nodes.get(i) == null) {
				continue;
			}
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}

		Collections.reverse(folderNodes);
		Collections.reverse(fileNodes);

		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
	}

	public void findDisabledNodes (){
		log("findDisabledNodes");
		if (((FileExplorerActivityLollipop) context).multiselect) {
			return;
		}
		ArrayList<Long> disabledNodes = new ArrayList<Long>();

		for (int i=0;i<nodes.size();i++){
			MegaNode folder = nodes.get(i);
			int accessLevel = megaApi.getAccess(folder);

			if(selectFile){
				if(accessLevel!=MegaShare.ACCESS_FULL) {
					disabledNodes.add(folder.getHandle());
				}
			}
			else{
				if(accessLevel==MegaShare.ACCESS_READ) {
					disabledNodes.add(folder.getHandle());
				}
			}
		}

		this.setDisableNodes(disabledNodes);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.action_text:{
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
				((FileExplorerActivityLollipop) context).finishActivity();
				break;
			}
		}
	}

	public void navigateToFolder(long handle) {
		log("navigateToFolder");

		((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
		log("((FileExplorerActivityLollipop)context).deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		setOptionsBarVisibility();

		int lastFirstVisiblePosition = 0;
		if (((FileExplorerActivityLollipop) context).isList()) {
			lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
		}
		else {
			lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
		}

		log("Push to stack "+lastFirstVisiblePosition+" position");
		lastPositionStack.push(lastFirstVisiblePosition);

		setParentHandle(handle);
		nodes.clear();
		adapter.setNodes(nodes);
		recyclerView.scrollToPosition(0);

		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
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
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}

		if (modeCloud == FileExplorerActivityLollipop.COPY){
			activateButton(true);
		}
	}

    public void itemClick(View view, int position) {
		log("------------------itemClick: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		ArrayList<MegaNode> clickNodes;

		if (((FileExplorerActivityLollipop) context).isSearchExpanded() && searchNodes != null) {
			clickNodes = searchNodes;
			((FileExplorerActivityLollipop) context).collapseSearchView();
		}
		else {
			clickNodes = nodes;
		}

		if (clickNodes.get(position).isFolder()){
			if(selectFile && ((FileExplorerActivityLollipop)context).multiselect && adapter.isMultipleSelect()){
				hideMultipleSelect();
			}
			((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
			log("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
			setOptionsBarVisibility();

			int lastFirstVisiblePosition = 0;
			if (((FileExplorerActivityLollipop) context).isList) {
				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			}
			else {
				lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
			}

			log("Push to stack "+lastFirstVisiblePosition+" position");
			lastPositionStack.push(lastFirstVisiblePosition);

			setParentHandle(clickNodes.get(position).getHandle());
			nodes = megaApi.getChildren(nodes.get(position), order);
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			recyclerView.scrollToPosition(0);
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
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
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (modeCloud == FileExplorerActivityLollipop.COPY){
					activateButton(true);
				}
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if (modeCloud == FileExplorerActivityLollipop.COPY){
					if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
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
		}
		else
		{
			//Is file
			if(selectFile)
			{
				MegaNode n = clickNodes.get(position);
				if(((FileExplorerActivityLollipop)context).multiselect){
					log("select file and allow multiselection");
					int togglePosition = position;
					if (!clickNodes.equals(nodes)) {
						MegaNode node;
						for (int i=0; i<nodes.size(); i++) {
							node = nodes.get(i);
							if (node != null && node.getHandle() == n.getHandle()) {
								togglePosition = i;
							}
						}
					}
					if (adapter.getSelectedItemCount() == 0) {
						log("activate the actionMode");
						activateActionMode();
						adapter.toggleSelection(togglePosition);
						updateActionModeTitle();
					}
					else {
						log("add to selectedNodes");
						adapter.toggleSelection(togglePosition);

						List<MegaNode> selectedNodes = adapter.getSelectedNodes();
						if (selectedNodes.size() > 0){
							updateActionModeTitle();
						}
					}
				}
				else{
					//Seleccionar el fichero para enviar...
					log("Selected node to send: "+n.getName());
					((FileExplorerActivityLollipop) context).buttonClick(n.getHandle());

				}
			}
		}
		((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
	}

	public int onBackPressed(){

		log("deepBrowserTree "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		((FileExplorerActivityLollipop)context).decreaseDeepBrowserTree();

		if(((FileExplorerActivityLollipop)context).deepBrowserTree==0){
			setParentHandle(-1);
//			uploadButton.setText(getString(R.string.choose_folder_explorer));
			findNodes();
			findDisabledNodes();

			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);

			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				log("Pop of the stack "+lastVisiblePosition+" position");
			}
			log("Scroll to "+lastVisiblePosition+" position");

			if(lastVisiblePosition>=0){
				if (((FileExplorerActivityLollipop) context).isList()) {
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				else {
					gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
			}
			setOptionsBarVisibility();

			if (adapter.getItemCount() != 0){
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				recyclerView.setVisibility(View.VISIBLE);
			}
			else{
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				recyclerView.setVisibility(View.GONE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
			return 3;
		}
		else if (((FileExplorerActivityLollipop)context).deepBrowserTree>0){
			parentHandle = adapter.getParentHandle();

			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){

				setParentHandle(parentNode.getHandle());
				nodes = megaApi.getChildren(parentNode, order);

				if (modeCloud == FileExplorerActivityLollipop.COPY){
					if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
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

				addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
				adapter.setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					log("Pop of the stack "+lastVisiblePosition+" position");
				}
				log("Scroll to "+lastVisiblePosition+" position");

				if(lastVisiblePosition>=0){
					if (((FileExplorerActivityLollipop) context).isList()) {
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else {
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				if (adapter.getItemCount() != 0){
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					recyclerView.setVisibility(View.VISIBLE);
				}
				else{
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
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					recyclerView.setVisibility(View.GONE);
				}
				((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
				return 2;
			}

			setOptionsBarVisibility();

			return 2;
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
			((FileExplorerActivityLollipop)context).deepBrowserTree=0;
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
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
		Util.log("IncomingSharesExplorerFragmentLollipop", log);
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	private void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		((FileExplorerActivityLollipop)context).setParentHandleIncoming(parentHandle);
		((FileExplorerActivityLollipop) context).changeTitle();
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (adapter != null){
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRootNode().getHandle()==parentHandle) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public void activateButton(boolean show){
		optionButton.setEnabled(show);
		if(show){
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
		}else{
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
		}
	}

	public void selectAll(){
		log("selectAll");
		if (adapter != null){
			adapter.selectAll();

			updateActionModeTitle();
		}
	}

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
		adapter.clearSelections();
		if (actionMode != null) {
			actionMode.finish();
		}

		if(isMultiselect()){
			activateButton(false);
		}

	}

	public void orderNodes (int order) {
		if (parentHandle == -1) {
			this.orderParent = order;
			findNodes();
		}
		else {
			this.order = order;
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), order);
		}

		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		adapter.setNodes(nodes);
	}

	public boolean isFolder(int position){
		MegaNode node = nodes.get(position);
		if(node == null || node.isFolder()){
			return true;
		}
		else{
			return false;
		}
	}

	boolean isMultiselect() {
		if (modeCloud==FileExplorerActivityLollipop.SELECT && selectFile && ((FileExplorerActivityLollipop) context).multiselect) {
			return true;
		}
		return false;
	}

	public void search (String s) {
		if (megaApi == null || s == null) {
			return;
		}
		log("search getparentHandle: "+getParentHandle());
		if (getParentHandle() == -1) {
			searchNodes = new ArrayList<>();
			for (MegaNode node : nodes) {
				if (node == null) continue;
				if (node.getName().toLowerCase().contains(s.toLowerCase())){
					searchNodes.add(node);
				}
			}
		}
		else {
			MegaNode parent = megaApi.getNodeByHandle(getParentHandle());
			if (parent == null) {
				return;
			}
			searchNodes = megaApi.search(parent, s, true, order);
		}
		if (searchNodes != null && adapter != null) {
			addSectionTitle(searchNodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(searchNodes);
		}

		showEmptyScreen();
	}

	public void closeSearch() {
		searchNodes = null;
		if (adapter == null) {
			return;
		}
		getNodes();
		addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
		adapter.setNodes(nodes);
		showEmptyScreen();
	}

	private void addSectionTitle(List<MegaNode> nodes,int type) {
		Map<Integer, String> sections = new HashMap<>();
		int placeholderCount;
		int folderCount = 0;
		int fileCount = 0;
		for (MegaNode node : nodes) {
			if(node == null) {
				continue;
			}
			if (node.isFolder()) {
				folderCount++;
			}
			if (node.isFile()) {
				fileCount++;
			}
		}

		if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
			int spanCount = 2;
			if (recyclerView instanceof NewGridRecyclerView) {
				spanCount = ((NewGridRecyclerView)recyclerView).getSpanCount();
			}
			if(folderCount > 0) {
				for (int i = 0;i < spanCount;i++) {
					sections.put(i,getString(R.string.general_folders));
				}
			}

			if(fileCount > 0 ) {
				placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);
				if (placeholderCount == 0) {
					for (int i = 0;i < spanCount;i++) {
						sections.put(folderCount + i,getString(R.string.general_files));
					}
				} else {
					for (int i = 0;i < spanCount;i++) {
						sections.put(folderCount + placeholderCount + i,getString(R.string.general_files));
					}
				}
			}
		} else {
			sections.put(0,getString(R.string.general_folders));
			sections.put(folderCount,getString(R.string.general_files));
		}

		if (headerItemDecoration == null) {
			headerItemDecoration = new NewHeaderItemDecoration(context);
			recyclerView.addItemDecoration(headerItemDecoration);
		}
		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
	}

	public FastScroller getFastScroller() {
	    return fastScroller;
    }
}
