package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import mega.privacy.android.app.SearchNodesTask;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;

import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.SearchNodesTask.setSearchProgressView;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class IncomingSharesExplorerFragmentLollipop extends RotatableFragment implements OnClickListener{

	private DisplayMetrics outMetrics;
	private Context context;
	private MegaApiAndroid megaApi;
	private ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
	private ArrayList<MegaNode> searchNodes = null;

	private long parentHandle = -1;

	private MegaExplorerLollipopAdapter adapter;
    private FastScroller fastScroller;

	private int modeCloud;
	private boolean selectFile;

	private RelativeLayout contentLayout;
	private RecyclerView recyclerView;
	private LinearLayoutManager mLayoutManager;
	private CustomizedGridLayoutManager gridLayoutManager;

	private ImageView emptyImageView;
	private LinearLayout emptyTextView;
	private TextView emptyTextViewFirst;

	private TextView contentText;
	private View separator;
	private Button optionButton;
	private Button cancelButton;
	private LinearLayout optionsBar;

	private Stack<Integer> lastPositionStack;

	private Handler handler;
	private ActionMode actionMode;

	private int orderParent = megaApi.ORDER_DEFAULT_ASC;
	private int order = megaApi.ORDER_DEFAULT_ASC;

	private NewHeaderItemDecoration headerItemDecoration;

	private SearchNodesTask searchNodesTask;
	private ProgressBar searchProgressBar;
	private boolean shouldResetNodes = true;

	@Override
	protected RotatableAdapter getAdapter() {
		return adapter;
	}

	@Override
	public void activateActionMode(){
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());

			if(isMultiselect()){
				activateButton(true);
			}
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
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
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_explorer_multiaction, menu);
			changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			if (!((FileExplorerActivityLollipop) context).shouldReopenSearch()) {
				((FileExplorerActivityLollipop) context).clearQuerySearch();
				getNodes();
				setNodes(nodes);
			}
			clearSelections();
			adapter.setMultipleSelect(false);
			changeStatusBarColorActionMode(context, ((FileExplorerActivityLollipop) context).getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();

			if (selected.size() != 0) {
				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				MegaNode node = megaApi.getNodeByHandle(parentHandle);

				if(selected.size() == megaApi.getNumChildFiles(node)){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);

				}else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				}

				unselect.setTitle(getString(R.string.action_unselect_all));
				unselect.setVisible(true);
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			return false;
		}
	}

	public static IncomingSharesExplorerFragmentLollipop newInstance() {
		logDebug("newInstance");
		IncomingSharesExplorerFragmentLollipop fragment = new IncomingSharesExplorerFragmentLollipop();
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
		logDebug("onCreateView");

		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);

		contentLayout = v.findViewById(R.id.content_layout);
		searchProgressBar = v.findViewById(R.id.progressbar);
		
		separator = v.findViewById(R.id.separator);
		
		optionsBar = v.findViewById(R.id.options_explorer_layout);

		optionButton = v.findViewById(R.id.action_text);
		optionButton.setOnClickListener(this);

		cancelButton = v.findViewById(R.id.cancel_text);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));

		fastScroller = v.findViewById(R.id.fastscroll);
		if (((FileExplorerActivityLollipop) context).isList()) {
			recyclerView = v.findViewById(R.id.file_list_view_browser);
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

		contentText = v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = v.findViewById(R.id.file_list_empty_image);
		emptyTextView = v.findViewById(R.id.file_list_empty_text);
		emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);
		parentHandle = ((FileExplorerActivityLollipop)context).getParentHandleIncoming();

		modeCloud = ((FileExplorerActivityLollipop)context).getMode();
		selectFile = ((FileExplorerActivityLollipop)context).isSelectFile();

		MegaPreferences prefs = getPreferences(context);

		if(prefs != null) {
			if (prefs.getPreferredSortOthers()!=null) {
				orderParent = Integer.parseInt(prefs.getPreferredSortOthers());
			}
			if (prefs.getPreferredSortCloud() != null) {
				order = Integer.parseInt(prefs.getPreferredSortCloud());
			}
		}

		getNodes();

		if (adapter == null){
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, recyclerView, selectFile);
		}
		else{
			adapter.setListFragment(recyclerView);
			adapter.setParentHandle(parentHandle);
			adapter.setSelectFile(selectFile);
		}

        recyclerView.setAdapter(adapter);
        fastScroller.setRecyclerView(recyclerView);
        setNodes(nodes);

		findDisabledNodes();

		
		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));

			if (((FileExplorerActivityLollipop)context).getDeepBrowserTree() > 0){
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

		logDebug("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).getDeepBrowserTree());
		setOptionsBarVisibility();

        if (((FileExplorerActivityLollipop) context).shouldRestartSearch()) {
            search(((FileExplorerActivityLollipop) context).getQuerySearch());
        }
		return v;
	}

	private void setOptionsBarVisibility() {
		if (!isMultiselect() && (((FileExplorerActivityLollipop)context).getDeepBrowserTree() <= 0 || selectFile)){
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

		if (adapter.getItemCount() != 0) {
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);
		} else {
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			recyclerView.setVisibility(View.GONE);

			String textToShow;

			if (parentHandle == -1) {
				if (isScreenInPortrait(context)) {
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				} else {
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}

				textToShow = String.format(context.getString(R.string.context_empty_incoming));
			} else {
				if (isScreenInPortrait(context)) {
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				}

				textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
			}

			try {
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			} catch (Exception e) {
				logWarning("Error formatting string", e);
			}

			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}

			emptyTextViewFirst.setText(result);
			emptyTextViewFirst.setVisibility(View.VISIBLE);
		}
	}

	private void findNodes(){
		logDebug("findNodes");
		((FileExplorerActivityLollipop)context).setDeepBrowserTree(0);

		setOptionsBarVisibility();
		nodes = megaApi.getInShares();

		if (orderParent  == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByMailDescending(nodes);
		}
	}


	private void sortByMailDescending(ArrayList<MegaNode> nodes){
		logDebug("sortByNameDescending");
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

	private void findDisabledNodes (){
		logDebug("findDisabledNodes");
		if (((FileExplorerActivityLollipop) context).isMultiselect()) {
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
				if(((FileExplorerActivityLollipop)context).isMultiselect()){
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
		logDebug("navigateToFolder");

		((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
		logDebug("((FileExplorerActivityLollipop)context).deepBrowserTree value: "+((FileExplorerActivityLollipop)context).getDeepBrowserTree());
		setOptionsBarVisibility();

		int lastFirstVisiblePosition = 0;
		if (((FileExplorerActivityLollipop) context).isList()) {
			lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
		}
		else {
			lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
		}

		logDebug("Push to stack " + lastFirstVisiblePosition + " position");
		lastPositionStack.push(lastFirstVisiblePosition);

		setParentHandle(handle);
		nodes.clear();
		setNodes(nodes);

		recyclerView.scrollToPosition(0);

		if (modeCloud == FileExplorerActivityLollipop.COPY){
			activateButton(true);
		}
	}

    public void itemClick(View view, int position) {
		ArrayList<MegaNode> clickNodes;

		if (searchNodes != null) {
			clickNodes = searchNodes;
			shouldResetNodes = false;
			((FileExplorerActivityLollipop) context).setQueryAfterSearch();
			((FileExplorerActivityLollipop) context).collapseSearchView();
		}
		else {
			clickNodes = nodes;
		}

		MegaNode n = clickNodes.get(position);

		if (n.isFolder()){
		    searchNodes = null;
		    ((FileExplorerActivityLollipop) context).setShouldRestartSearch(false);

			if(selectFile && ((FileExplorerActivityLollipop)context).isMultiselect() && adapter.isMultipleSelect()){
				hideMultipleSelect();
			}
			((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
			logDebug("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).getDeepBrowserTree());
			setOptionsBarVisibility();

			int lastFirstVisiblePosition = 0;
			if (((FileExplorerActivityLollipop) context).isList()) {
				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			}
			else {
				lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
			}

			logDebug("Push to stack " + lastFirstVisiblePosition + " position");
			lastPositionStack.push(lastFirstVisiblePosition);

			setParentHandle(n.getHandle());
			setNodes(megaApi.getChildren(nodes.get(position), order));
			recyclerView.scrollToPosition(0);

			if (adapter.getItemCount() == 0 && modeCloud == FileExplorerActivityLollipop.COPY) {
				activateButton(true);
			} else if (modeCloud == FileExplorerActivityLollipop.COPY && ((FileExplorerActivityLollipop) context).getDeepBrowserTree() > 0) {
				MegaNode parent = ((FileExplorerActivityLollipop) context).parentMoveCopy();
				if (parent != null && parent.getHandle() == parentHandle) {
					activateButton(false);
				} else {
					activateButton(true);
				}
			}
		}
		else if(selectFile) {
			if(((FileExplorerActivityLollipop)context).isMultiselect()){
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
					activateActionMode();
					adapter.toggleSelection(togglePosition);
					updateActionModeTitle();
				}
				else {
					adapter.toggleSelection(togglePosition);

					List<MegaNode> selectedNodes = adapter.getSelectedNodes();
					if (selectedNodes.size() > 0){
						updateActionModeTitle();
					}
				}
			}
			else{
				((FileExplorerActivityLollipop) context).buttonClick(n.getHandle());

			}
		}
		((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
		shouldResetNodes = true;
	}

	public int onBackPressed(){
		logDebug("deepBrowserTree "+((FileExplorerActivityLollipop)context).getDeepBrowserTree());
		((FileExplorerActivityLollipop)context).decreaseDeepBrowserTree();

		if(((FileExplorerActivityLollipop)context).getDeepBrowserTree()==0){
			setParentHandle(-1);
//			uploadButton.setText(getString(R.string.choose_folder_explorer));
			findNodes();
			findDisabledNodes();

			setNodes(nodes);

			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				logDebug("Pop of the stack " + lastVisiblePosition + " position");
			}
			logDebug("Scroll to " + lastVisiblePosition + " position");

			if(lastVisiblePosition>=0){
				if (((FileExplorerActivityLollipop) context).isList()) {
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				else {
					gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
			}
			setOptionsBarVisibility();
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
			return 3;
		}
		else if (((FileExplorerActivityLollipop)context).getDeepBrowserTree()>0){
			parentHandle = adapter.getParentHandle();

			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){

				setParentHandle(parentNode.getHandle());
				nodes = megaApi.getChildren(parentNode, order);

				if (modeCloud == FileExplorerActivityLollipop.COPY){
					if (((FileExplorerActivityLollipop)context).getDeepBrowserTree() > 0){
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

				setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){
					if (((FileExplorerActivityLollipop) context).isList()) {
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else {
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
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
			((FileExplorerActivityLollipop)context).setDeepBrowserTree(0);
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
			return 0;
		}
	}
	
	/*
	 * Disable nodes from the list
	 */
	private void setDisableNodes(ArrayList<Long> disabledNodes) {
		adapter.setDisableNodes(disabledNodes);
	}

	public long getParentHandle() {
		return parentHandle;
	}
	
	private void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		((FileExplorerActivityLollipop)context).setParentHandleIncoming(parentHandle);
		((FileExplorerActivityLollipop) context).changeTitle();
	}
	
	private void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (adapter != null){
			addSectionTitle(nodes, ((FileExplorerActivityLollipop) context).getItemType());
			adapter.setNodes(nodes);
			showEmptyScreen();
		}
	}
	
	private RecyclerView getRecyclerView(){
		return recyclerView;
	}

	private void activateButton(boolean show){
		optionButton.setEnabled(show);
		if(show){
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
		}else{
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
		}
	}

	private void selectAll(){
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

	@Override
	protected void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}

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
		actionMode.invalidate();
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
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

		setNodes(nodes);
	}

	public boolean isFolder(int position){
		MegaNode node = nodes.get(position);

		return node == null || node.isFolder();
	}

	boolean isMultiselect() {
		return modeCloud == FileExplorerActivityLollipop.SELECT && selectFile && ((FileExplorerActivityLollipop) context).isMultiselect();
	}

	public void search (String s) {
		if (megaApi == null || s == null || !shouldResetNodes) {
			return;
		}

		setProgressView(true);
		cancelPreviousAsyncTask();
		searchNodesTask = new SearchNodesTask(context,
				this,
				s,
				-1,
				nodes);
		searchNodesTask.execute();
	}

	private void cancelPreviousAsyncTask() {
		if (searchNodesTask != null) {
			searchNodesTask.cancelSearch();
			searchNodesTask.cancel(true);
		}
	}

	public void setProgressView(boolean inProgress) {
		setSearchProgressView(contentLayout, searchProgressBar, recyclerView, inProgress);
	}

	public void setSearchNodes(ArrayList<MegaNode> nodes) {
		if (adapter == null) return;

		searchNodes = nodes;
		((FileExplorerActivityLollipop) context).setShouldRestartSearch(true);
		addSectionTitle(searchNodes, ((FileExplorerActivityLollipop) context).getItemType());
		adapter.setNodes(searchNodes);
		showEmptyScreen();
	}

	public void closeSearch(boolean collapsedByClick) {
		setProgressView(false);
		cancelPreviousAsyncTask();
		if (!collapsedByClick) {
            searchNodes = null;
        }
		if (shouldResetNodes) {
			getNodes();
			setNodes(nodes);
		}
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
		} else {
			recyclerView.removeItemDecoration(headerItemDecoration);
		}

		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
		recyclerView.addItemDecoration(headerItemDecoration);
	}

	public FastScroller getFastScroller() {
	    return fastScroller;
    }

	public void setHeaderItemDecoration(NewHeaderItemDecoration headerItemDecoration) {
		this.headerItemDecoration = headerItemDecoration;
	}
}
