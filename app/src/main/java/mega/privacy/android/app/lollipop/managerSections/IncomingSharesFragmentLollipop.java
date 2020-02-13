package mega.privacy.android.app.lollipop.managerSections;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class IncomingSharesFragmentLollipop extends MegaNodeBaseFragment {

    @Override
    public void activateActionMode() {
		super.activateActionMode();
		actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionBarCallBack());
    }

	private class ActionBarCallBack extends BaseActionBarCallBack {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		    checkSelectOptions(menu, false);

		    if (managerActivity.getDeepBrowserTreeIncoming() == 0) {
                menu.findItem(R.id.cab_menu_rename).setVisible(false);
                menu.findItem(R.id.cab_menu_leave_multiple_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.cab_menu_move).setVisible(false);
                menu.findItem(R.id.cab_menu_trash).setVisible(false);
            } else {
                checkOptions();

                menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

                menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));
                menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
                menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                if (selected.size() >= 1 && megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() != MegaError.API_OK) {
                    showMove = false;
                }
                menu.findItem(R.id.cab_menu_move).setVisible(showMove);
                menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

                menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
            }

            menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			return false;
		}
	}

	public static IncomingSharesFragmentLollipop newInstance() {
		return new IncomingSharesFragmentLollipop();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("Parent Handle: " + ((ManagerActivityLollipop) context).getParentHandleIncoming());

		if (megaApi.getRootNode() == null) {
			return null;
		}

		((ManagerActivityLollipop) context).showFabButton();

		View v;

		if (((ManagerActivityLollipop) context).isList) {
			v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
		} else {
			v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);

			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(80, outMetrics));
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);
			addSectionTitle(nodes, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			} else {
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}

		adapter.setParentHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
		adapter.setListFragment(recyclerView);

		if (((ManagerActivityLollipop) context).getParentHandleIncoming() == INVALID_HANDLE) {
			logWarning("ParentHandle -1");
			findNodes();
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
			logDebug("ParentHandle to find children: " + ((ManagerActivityLollipop) context).getParentHandleIncoming());

			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		recyclerView.setClipToPadding(false);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				checkScroll();
			}
		});

		((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();

		adapter.setMultipleSelect(false);
		recyclerView.setAdapter(adapter);
		fastScroller.setRecyclerView(recyclerView);
		visibilityFastScroller();
		setEmptyView();

		logDebug("Deep browser tree: " + ((ManagerActivityLollipop) context).deepBrowserTreeIncoming);

		return v;
	}

	@Override
	public void refresh (){
		logDebug("refresh");
		MegaNode parentNode = null;

		if (((ManagerActivityLollipop) context).getParentHandleIncoming() == -1
				|| megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming()) == null) {
			findNodes();
		} else {
			parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		visibilityFastScroller();
		clearSelections();
		hideMultipleSelect();
		setEmptyView();
	}

    @Override
	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		logDebug("itemClick");

		if (adapter.isMultipleSelect()) {
			logDebug("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else {
			if (nodes.get(position).isFolder()) {
				((ManagerActivityLollipop) context).increaseDeepBrowserTreeIncoming();
				logDebug("Is folder deep: " + ((ManagerActivityLollipop) context).deepBrowserTreeIncoming);

				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if (((ManagerActivityLollipop) context).isList) {
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				} else {
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if (lastFirstVisiblePosition == -1) {
						logDebug("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");

				lastPositionStack.push(lastFirstVisiblePosition);

				((ManagerActivityLollipop) context).setParentHandleIncoming(n.getHandle());
				((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();
				((ManagerActivityLollipop) context).setToolbarTitle();

				MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleIncoming());
				nodes = megaApi.getChildren(nodes.get(position), ((ManagerActivityLollipop) context).orderCloud);
				addSectionTitle(nodes, adapter.getAdapterType());

				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				visibilityFastScroller();
				setEmptyView();
				checkScroll();
				((ManagerActivityLollipop) context).showFabButton();
			} else {
				//Is file
				openFile(nodes.get(position), INCOMING_SHARES_ADAPTER, position, screenPosition, imageView);
			}
		}
	}

	public void findNodes(){
		logDebug("findNodes");
		nodes=megaApi.getInShares();

		if(((ManagerActivityLollipop)context).orderOthers == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByMailDescending(nodes);
		}
		addSectionTitle(nodes,adapter.getAdapterType() );
		adapter.setNodes(nodes);

		setEmptyView();
	}

	@Override
	public int onBackPressed(){
		logDebug("deepBrowserTree:" + ((ManagerActivityLollipop)context).deepBrowserTreeIncoming);

		if (adapter == null){
			return 0;
		}


        if (((ManagerActivityLollipop) context).comesFromNotifications && ((ManagerActivityLollipop) context).comesFromNotificationsLevel == (((ManagerActivityLollipop) context).deepBrowserTreeIncoming)) {
			((ManagerActivityLollipop) context).comesFromNotifications = false;
			((ManagerActivityLollipop) context).comesFromNotificationsLevel = 0;
			((ManagerActivityLollipop) context).comesFromNotificationHandle = -1;
            ((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
			((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(((ManagerActivityLollipop) context).comesFromNotificationDeepBrowserTreeIncoming);
			((ManagerActivityLollipop) context).comesFromNotificationDeepBrowserTreeIncoming = -1;
			((ManagerActivityLollipop) context).setParentHandleIncoming(((ManagerActivityLollipop) context).comesFromNotificationHandleSaved);
			((ManagerActivityLollipop) context).comesFromNotificationHandleSaved = -1;
			((ManagerActivityLollipop) context).refreshIncomingShares();

			return 4;
        }
		else {
			((ManagerActivityLollipop)context).decreaseDeepBrowserTreeIncoming();
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			if(((ManagerActivityLollipop)context).deepBrowserTreeIncoming==0){
				//In the beginning of the navigation

				logDebug("deepBrowserTree==0");
				((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
				((ManagerActivityLollipop) context).setToolbarTitle();
				findNodes();
				visibilityFastScroller();
				recyclerView.setVisibility(View.VISIBLE);
				int lastVisiblePosition = 0;
				if (!lastPositionStack.empty()) {
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");


				if (lastVisiblePosition >= 0) {

					if (((ManagerActivityLollipop) context).isList) {
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else {
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				((ManagerActivityLollipop) context).showFabButton();

				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);

				return 3;
			}
			else if (((ManagerActivityLollipop)context).deepBrowserTreeIncoming>0){
				logDebug("deepTree>0");

				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleIncoming()));

				if (parentNode != null){
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);

					((ManagerActivityLollipop)context).setParentHandleIncoming(parentNode.getHandle());

					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setToolbarTitle();

					nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
					addSectionTitle(nodes,adapter.getAdapterType() );

					adapter.setNodes(nodes);
					visibilityFastScroller();
					int lastVisiblePosition = 0;
					if(!lastPositionStack.empty()){
						lastVisiblePosition = lastPositionStack.pop();
						logDebug("Pop of the stack "+lastVisiblePosition+" position");
					}
					logDebug("Scroll to "+lastVisiblePosition+" position");

					if(lastVisiblePosition>=0){

						if(((ManagerActivityLollipop)context).isList){
							mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						}
						else{
							gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						}
					}
				}

				((ManagerActivityLollipop) context).showFabButton();
				return 2;
			}
			else{
				logDebug("ELSE deepTree");
				((ManagerActivityLollipop)context).deepBrowserTreeIncoming=0;
				return 0;
			}

		}
	}

	@Override
	public void setNodes(ArrayList<MegaNode> nodes) {
		logDebug("setNodes");
		this.nodes = nodes;
		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
		} else {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
		}
		adapter.setNodes(nodes);
	}

	@Override
	protected void setEmptyView() {
		String textToShow;

		if (megaApi.getRootNode().getHandle() == ((ManagerActivityLollipop) context).getParentHandleIncoming()
				|| ((ManagerActivityLollipop) context).getParentHandleIncoming() == -1) {
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
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		emptyTextViewFirst.setText(result);

		checkEmptyView();
	}

	public int getDeepBrowserTree(){
		return ((ManagerActivityLollipop)context).deepBrowserTreeIncoming;
	}
}
