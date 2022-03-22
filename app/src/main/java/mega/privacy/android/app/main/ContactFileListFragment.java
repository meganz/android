package mega.privacy.android.app.main;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.utils.Constants.BUFFER_COMP;
import static mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_CONTACT_FILE_LIST;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShares;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.listeners.FabButtonListener;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class ContactFileListFragment extends ContactFileBaseFragment {

    private ActionMode actionMode;
	CoordinatorLayout mainLayout;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	FloatingActionButton fab;
	Stack<Long> parentHandleStack = new Stack<>();
    int currNodePosition = -1;

    private final static String PARENT_HANDLE_STACK = "parentHandleStack";
    public void setCurrNodePosition(int currNodePosition) {
        this.currNodePosition = currNodePosition;
    }

	Handler handler;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapter.getSelectedNodes();

			switch (item.getItemId()) {
				case R.id.cab_menu_download: {
					((ContactFileListActivity)context).downloadFile(documents);
					break;
				}
				case R.id.cab_menu_copy: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i = 0; i < documents.size(); i++) {
						handleList.add(documents.get(i).getHandle());
					}

					((ContactFileListActivity)context).showCopy(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					break;
				}
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					showConfirmationLeaveIncomingShares(requireActivity(),
							(SnackbarShower) requireActivity(), handleList);
                    break;
				}
                case R.id.cab_menu_trash: {
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    ((ContactFileListActivity)(context)).askConfirmationMoveToRubbish(handleList);
                    break;
                }
				case R.id.cab_menu_rename: {
					MegaNode node = documents.get(0);
					showRenameNodeDialog(context, node, (SnackbarShower) getActivity(),
							(ActionNodeCallback) getActivity());
					break;
				}
			}
            return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			fab.hide();
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			setFabVisibility(megaApi.getNodeByHandle(parentHandle));
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();

			menu.findItem(R.id.cab_menu_share_link)
					.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size()));

			boolean areAllNotTakenDown = MegaNodeUtil.areAllNotTakenDown(selected);
			boolean showRename = false;
			boolean showMove = false;
			boolean showTrash = false;

			// Rename
			if(selected.size() == 1){
				if ((megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
					showRename = true;
				}
			}

			if (selected.size() > 0) {
				if ((megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
					showMove = true;
				}
			}

			if (selected.size() != 0) {
				showMove = false;
				// Rename
				if(selected.size() == 1) {

					if((megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
						showMove = true;
						showRename = true;
					}
					else if(megaApi.checkAccessErrorExtended(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
						showMove = false;
						showRename = false;
					}
				}
				else{
					showRename = false;
					showMove = false;
				}

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMoveErrorExtended(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showMove = false;
						break;
					}
				}

				if(!((ContactFileListActivity)context).isEmptyParentHandleStack()){
					showTrash = true;
				}
				for(int i=0; i<selected.size(); i++){
					if((megaApi.checkAccessErrorExtended(selected.get(i), MegaShare.ACCESS_FULL).getErrorCode() != MegaError.API_OK)){
						showTrash = false;
						break;
					}
				}

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			if (areAllNotTakenDown) {
				menu.findItem(R.id.cab_menu_download).setVisible(true);
				menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true);
				menu.findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_copy).setVisible(true);
			}

			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(false);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);

			return false;
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(PARENT_HANDLE_STACK, parentHandleStack);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			parentHandleStack = (Stack<Long>) savedInstanceState.getSerializable(PARENT_HANDLE_STACK);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		View v = null;
		handler = new Handler();
		if (userEmail != null){
			v = inflater.inflate(R.layout.fragment_contact_file_list, container, false);

			mainLayout = (CoordinatorLayout) v.findViewById(R.id.contact_file_list_coordinator_layout);

			fab = (FloatingActionButton) v.findViewById(R.id.floating_button_contact_file_list);
			fab.setOnClickListener(new FabButtonListener(context));
			fab.hide();

			contact = megaApi.getContact(userEmail);
			if(contact == null)
			{
				return null;
			}

			parentHandle = ((ContactFileListActivity) context).getParentHandle();
			if (parentHandle != -1) {
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				contactNodes = megaApi.getChildren(parentNode, orderGetChildren);
				((ContactFileListActivity)context).setTitleActionBar(parentNode.getName());
			} else {
				contactNodes = megaApi.getInShares(contact);
			}

			listView = (RecyclerView) v.findViewById(R.id.contact_file_list_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.setItemAnimator(noChangeRecyclerViewItemAnimator());

			Resources res = getResources();
			int valuePaddingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, res.getDisplayMetrics());
			int valuePaddingBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88, res.getDisplayMetrics());

			listView.setClipToPadding(false);
			listView.setPadding(0, valuePaddingTop, 0, valuePaddingBottom);
			listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.contact_file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_file_list_empty_text);
			if (contactNodes.size() != 0) {
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			} else {
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_incoming));
				try{
					textToShow = textToShow.replace(
							"[A]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
									+ "\'>"
					).replace("[/A]", "</font>").replace(
							"[B]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
									+ "\'>"
					).replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextView.setText(result);
			}

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, contactNodes, parentHandle,
						listView, CONTACT_FILE_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setNodes(contactNodes);
				adapter.setParentHandle(parentHandle);
			}

			adapter.setMultipleSelect(false);

			listView.setAdapter(adapter);
		}
        if(currNodePosition != -1 && parentHandle == -1 ) {
            itemClick(currNodePosition);
        }
		showFabButton(megaApi.getNodeByHandle(parentHandle));
		return v;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		observeDragSupportEvents(getViewLifecycleOwner(), listView, VIEWER_FROM_CONTACT_FILE_LIST);
	}

    public void checkScroll() {
        boolean withElevation = (listView != null && listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect());
		AppBarLayout abL = requireActivity().findViewById(R.id.app_bar_layout);
		Util.changeActionBarElevation(requireActivity(), abL, withElevation);
    }

	public void showOptionsPanel(MegaNode sNode){
		logDebug("Node handle: " + sNode.getHandle());
		((ContactFileListActivity)context).showOptionsPanel(sNode);
	}

	public void setNodes(long parentHandle) {
		if (megaApi.getNodeByHandle(parentHandle) != null) {
			this.parentHandle = parentHandle;
			((ContactFileListActivity) context).setParentHandle(parentHandle);
			adapter.setParentHandle(parentHandle);
			setNodes(megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), orderGetChildren));
		}
	}

	public void setNodes(ArrayList<MegaNode> nodes){
		this.contactNodes = nodes;
		if (adapter != null){
			adapter.setNodes(contactNodes);
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
				} else {

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_incoming));
					try{
						textToShow = textToShow.replace(
								"[A]", "<font color=\'"
										+ ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
										+ "\'>"
						).replace("[/A]", "</font>").replace(
								"[B]", "<font color=\'"
										+ ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
										+ "\'>"
						).replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextView.setText(result);
				}
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}

	public void itemClick(int position) {

		if (adapter.isMultipleSelect()){
			logDebug("Multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			if (contactNodes.get(position).isFolder()) {
				MegaNode n = contactNodes.get(position);

				int lastFirstVisiblePosition = 0;

				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");
				lastPositionStack.push(lastFirstVisiblePosition);

				((ContactFileListActivity)context).setTitleActionBar(n.getName());
				((ContactFileListActivity)context).supportInvalidateOptionsMenu();

				parentHandleStack.push(parentHandle);
				parentHandle = contactNodes.get(position).getHandle();
				adapter.setParentHandle(parentHandle);
				((ContactFileListActivity)context).setParentHandle(parentHandle);

				contactNodes = megaApi.getChildren(contactNodes.get(position));
				adapter.setNodes(contactNodes);
				listView.scrollToPosition(0);

				// If folder has no files
				if (adapter.getItemCount() == 0) {
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_incoming));
					try{
						textToShow = textToShow.replace(
								"[A]", "<font color=\'"
										+ ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
										+ "\'>"
						).replace("[/A]", "</font>").replace(
								"[B]", "<font color=\'"
										+ ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
										+ "\'>"
						).replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextView.setText(result);


				} else {
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				showFabButton(n);
			}
			else {
				if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isImage()) {
					Intent intent = ImageViewerActivity.getIntentForParentNode(
							requireContext(),
							megaApi.getParentNode(contactNodes.get(position)).getHandle(),
							orderGetChildren,
							contactNodes.get(position).getHandle()
					);
					putThumbnailLocation(intent, listView, position, VIEWER_FROM_CONTACT_FILE_LIST, adapter);
					startActivity(intent);
					((ContactFileListActivity) context).overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isVideoReproducible()	|| MimeTypeList.typeForName(contactNodes.get(position).getName()).isAudio()) {
					MegaNode file = contactNodes.get(position);
					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("Node Handle: " + file.getHandle());

					Intent mediaIntent;
					boolean internalIntent;
					boolean opusFile = false;
					if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()){
						mediaIntent = new Intent(Intent.ACTION_VIEW);
						internalIntent = false;
						String[] s = file.getName().split("\\.");
						if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
							opusFile = true;
						}
					}
					else {
						internalIntent = true;
						mediaIntent = getMediaIntent(context, contactNodes.get(position).getName());
					}
					mediaIntent.putExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL, contact.getEmail());
					mediaIntent.putExtra("position", position);
					mediaIntent.putExtra("adapterType", CONTACT_FILE_ADAPTER);
					if (megaApi.getParentNode(contactNodes.get(position)).getType() == MegaNode.TYPE_ROOT) {
						mediaIntent.putExtra("parentNodeHandle", -1L);
					} else {
						mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(contactNodes.get(position)).getHandle());
					}
					mediaIntent.putExtra("orderGetChildren", orderGetChildren);
					putThumbnailLocation(mediaIntent, listView, position, VIEWER_FROM_CONTACT_FILE_LIST, adapter);
					mediaIntent.putExtra("HANDLE", file.getHandle());
					mediaIntent.putExtra("FILENAME", file.getName());

					String localPath = getLocalFile(file);
					if (localPath != null){
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
							mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>BUFFER_COMP){
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
						}
						else{
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					if (opusFile){
						mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
					}
					if (internalIntent){
						startActivity(mediaIntent);
					}
					else {
						if (isIntentAvailable(context, mediaIntent)){
							startActivity(mediaIntent);
						}
						else{
							((ContactFileListActivity) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getString(R.string.intent_not_available));
							adapter.notifyDataSetChanged();
							((ContactFileListActivity)context).downloadFile(
									Collections.singletonList(contactNodes.get(position)));
						}
					}
					((ContactFileListActivity) context).overridePendingTransition(0,0);
				}else if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isPdf()){
					MegaNode file = contactNodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("NODE HANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

					Intent pdfIntent = new Intent(context, PdfViewerActivity.class);
					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("adapterType", CONTACT_FILE_ADAPTER);

					String localPath = getLocalFile(file);
					if (localPath != null){
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N	&& localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
							pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>BUFFER_COMP){
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
						}
						else{
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						pdfIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					pdfIntent.putExtra("HANDLE", file.getHandle());
					putThumbnailLocation(pdfIntent, listView, position, VIEWER_FROM_CONTACT_FILE_LIST, adapter);
					if (isIntentAvailable(context, pdfIntent)){
						startActivity(pdfIntent);
					}
					else{
						Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						((ContactFileListActivity)context).downloadFile(
								Collections.singletonList(contactNodes.get(position)));
					}
					((ContactFileListActivity) context).overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isURL()){
					manageURLNode(context, megaApi, contactNodes.get(position));
				} else if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isOpenableTextFile(contactNodes.get(position).getSize())) {
					manageTextFileIntent(requireContext(), contactNodes.get(position), CONTACT_FILE_ADAPTER);
				} else {
					adapter.notifyDataSetChanged();
					((ContactFileListActivity)context).downloadFile(
							Collections.singletonList(contactNodes.get(position)));
				}
			}
		}
	}

	public int onBackPressed() {
		logDebug("onBackPressed");

		parentHandle = adapter.getParentHandle();
		((ContactFileListActivity)context).setParentHandle(parentHandle);
		//If from ContactInfoActivity embedded list, return to ContactInfoActivity directly.
        if(currNodePosition != -1 && parentHandleStack.size() == 1) {
            return 0;
        }
		if (parentHandleStack.isEmpty()) {
			logDebug("return 0");
			fab.hide();
			return 0;
		} else {
			parentHandle = parentHandleStack.pop();
			setFabVisibility(megaApi.getNodeByHandle(parentHandle));
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			if (parentHandle == -1) {
				contactNodes = megaApi.getInShares(contact);
				((ContactFileListActivity)context).setTitleActionBar(null);
				((ContactFileListActivity)context).supportInvalidateOptionsMenu();
				adapter.setNodes(contactNodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				((ContactFileListActivity)context).setParentHandle(parentHandle);
				((ContactFileListActivity) context).supportInvalidateOptionsMenu();
				adapter.setParentHandle(parentHandle);
				logDebug("return 2");
				return 2;
			} else {
				contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
				((ContactFileListActivity)context).setTitleActionBar(megaApi.getNodeByHandle(parentHandle).getName());
				((ContactFileListActivity)context).supportInvalidateOptionsMenu();
				adapter.setNodes(contactNodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				((ContactFileListActivity)context).setParentHandle(parentHandle);
				adapter.setParentHandle(parentHandle);
				showFabButton(megaApi.getNodeByHandle(parentHandle));
				logDebug("return 3");
				return 3;
			}
		}
	}

	public void setNodes(){
		contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
		adapter.setNodes(contactNodes);
		listView.invalidate();
	}

	public void selectAll(){
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{
				adapter.setMultipleSelect(true);
				adapter.selectAll();

				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}

			new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
		}
	}

	protected void updateActionModeTitle() {
		if (actionMode == null) {
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
		Resources res = getResources();
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
		// actionMode.
	}

    public void clearSelections() {
        if(adapter != null && adapter.isMultipleSelect()){
            adapter.clearSelections();
        }
    }

	public void hideMultipleSelect() {
		logDebug("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public void showFabButton(MegaNode node){
		setFabVisibility(node);
		((ContactFileListActivity) context).invalidateOptionsMenu();
	}

	public int getFabVisibility(){
		return fab.getVisibility();
	}

    public void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
        if (adapter != null){
            adapter.setParentHandle(parentHandle);
        }
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public boolean isEmptyParentHandleStack() {
        return parentHandleStack.isEmpty();
    }

	private void setFabVisibility(MegaNode node) {
		if (megaApi.getAccess(node) == MegaShare.ACCESS_READ || node == null) {
			fab.hide();
		} else {
			fab.show();
		}
	}
}
