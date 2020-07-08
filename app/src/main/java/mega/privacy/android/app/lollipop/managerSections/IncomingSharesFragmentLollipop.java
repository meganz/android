package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class IncomingSharesFragmentLollipop extends MegaNodeBaseFragment {

	@Override
	public void activateActionMode() {
		if (!adapter.isMultipleSelect()) {
			super.activateActionMode();
			actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(
					new ActionBarCallBack());
		}
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("Parent Handle: " + managerActivity.getParentHandleIncoming());

		if (megaApi.getRootNode() == null) {
			return null;
		}

		managerActivity.showFabButton();

		View v;

		if (managerActivity.isList) {
			v = getListView(inflater, container);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, managerActivity.getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
		} else {
			v = getGridView(inflater, container);

			addSectionTitle(nodes, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, managerActivity.getParentHandleIncoming(), recyclerView, null, INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}

		adapter.setParentHandle(managerActivity.getParentHandleIncoming());
		adapter.setListFragment(recyclerView);

		if (managerActivity.getParentHandleIncoming() == INVALID_HANDLE) {
			logWarning("ParentHandle -1");
			findNodes();
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(managerActivity.getParentHandleIncoming());
			logDebug("ParentHandle to find children: " + managerActivity.getParentHandleIncoming());

			nodes = megaApi.getChildren(parentNode, managerActivity.orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		managerActivity.supportInvalidateOptionsMenu();

		adapter.setMultipleSelect(false);
		recyclerView.setAdapter(adapter);
		visibilityFastScroller();
		setEmptyView();

		logDebug("Deep browser tree: " + managerActivity.deepBrowserTreeIncoming);

		return v;
	}

	@Override
	public void refresh() {
		MegaNode parentNode;

		if (managerActivity.getParentHandleIncoming() == -1
				|| megaApi.getNodeByHandle(managerActivity.getParentHandleIncoming()) == null) {
			findNodes();
		} else {
			parentNode = megaApi.getNodeByHandle(managerActivity.getParentHandleIncoming());
			nodes = megaApi.getChildren(parentNode, managerActivity.orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		managerActivity.supportInvalidateOptionsMenu();

		visibilityFastScroller();
		hideActionMode();
		setEmptyView();
	}

	@Override
	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		if (adapter.isMultipleSelect()) {
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else if (nodes.get(position).isFolder()) {
			managerActivity.increaseDeepBrowserTreeIncoming();
			logDebug("Is folder deep: " + managerActivity.deepBrowserTreeIncoming);

			MegaNode n = nodes.get(position);

			int lastFirstVisiblePosition = 0;
			if (managerActivity.isList) {
				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			} else {
				lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
				if (lastFirstVisiblePosition == -1) {
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
				}
			}

			lastPositionStack.push(lastFirstVisiblePosition);

			managerActivity.setParentHandleIncoming(n.getHandle());
			managerActivity.supportInvalidateOptionsMenu();
			managerActivity.setToolbarTitle();

			nodes = megaApi.getChildren(nodes.get(position), managerActivity.orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());

			adapter.setNodes(nodes);
			recyclerView.scrollToPosition(0);
			visibilityFastScroller();
			setEmptyView();
			checkScroll();
			managerActivity.showFabButton();
		} else {
			openFile(nodes.get(position), INCOMING_SHARES_ADAPTER, position, screenPosition, imageView);
		}
	}

	public void findNodes() {
		nodes = megaApi.getInShares();

		if (managerActivity.orderOthers == MegaApiJava.ORDER_DEFAULT_DESC) {
			sortByMailDescending(nodes);
		}
		addSectionTitle(nodes, adapter.getAdapterType());
		adapter.setNodes(nodes);

		setEmptyView();
	}

	@Override
	public int onBackPressed() {
		logDebug("deepBrowserTree:" + managerActivity.deepBrowserTreeIncoming);

		if (adapter == null) {
			return 0;
		}

		if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationsLevel == (managerActivity.deepBrowserTreeIncoming)) {
			managerActivity.comesFromNotifications = false;
			managerActivity.comesFromNotificationsLevel = 0;
			managerActivity.comesFromNotificationHandle = -1;
			managerActivity.selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
			managerActivity.setDeepBrowserTreeIncoming(managerActivity.comesFromNotificationDeepBrowserTreeIncoming);
			managerActivity.comesFromNotificationDeepBrowserTreeIncoming = -1;
			managerActivity.setParentHandleIncoming(managerActivity.comesFromNotificationHandleSaved);
			managerActivity.comesFromNotificationHandleSaved = -1;
			managerActivity.refreshIncomingShares();

			return 4;
		} else {
			managerActivity.decreaseDeepBrowserTreeIncoming();
			managerActivity.supportInvalidateOptionsMenu();

			if (managerActivity.deepBrowserTreeIncoming == 0) {
				//In the beginning of the navigation

				logDebug("deepBrowserTree==0");
				managerActivity.setParentHandleIncoming(-1);
				managerActivity.setToolbarTitle();
				findNodes();
				visibilityFastScroller();
				recyclerView.setVisibility(View.VISIBLE);
				int lastVisiblePosition = 0;
				if (!lastPositionStack.empty()) {
					lastVisiblePosition = lastPositionStack.pop();
				}

				if (lastVisiblePosition >= 0) {
					if (managerActivity.isList) {
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					} else {
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				managerActivity.showFabButton();

				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);

				return 3;
			} else if (managerActivity.deepBrowserTreeIncoming > 0) {
				logDebug("deepTree>0");

				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(managerActivity.getParentHandleIncoming()));

				if (parentNode != null) {
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);

					managerActivity.setParentHandleIncoming(parentNode.getHandle());

					managerActivity.supportInvalidateOptionsMenu();
					managerActivity.setToolbarTitle();

					nodes = megaApi.getChildren(parentNode, managerActivity.orderCloud);
					addSectionTitle(nodes, adapter.getAdapterType());

					adapter.setNodes(nodes);
					visibilityFastScroller();
					int lastVisiblePosition = 0;
					if (!lastPositionStack.empty()) {
						lastVisiblePosition = lastPositionStack.pop();
					}

					if (lastVisiblePosition >= 0) {
						if (managerActivity.isList) {
							mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						} else {
							gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
						}
					}
				}

				managerActivity.showFabButton();
				return 2;
			} else {
				logDebug("ELSE deepTree");
				managerActivity.deepBrowserTreeIncoming = 0;
				return 0;
			}

		}
	}

	@Override
	public void setNodes(ArrayList<MegaNode> nodes) {
		this.nodes = nodes;
		if (managerActivity.isList) {
			addSectionTitle(nodes, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
		} else {
			addSectionTitle(nodes, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
		}
		adapter.setNodes(nodes);
	}

	@Override
	protected void setEmptyView() {
		String textToShow = null;

		if (megaApi.getRootNode().getHandle() == managerActivity.getParentHandleIncoming()
				|| managerActivity.getParentHandleIncoming() == -1) {
			if (isScreenInPortrait(context)) {
				emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
			} else {
				emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
			}
			textToShow = context.getString(R.string.context_empty_incoming);
		}

		setFinalEmptyView(textToShow);
	}

	/**
	 * Method to update an item when a nickname is added, updated or removed from a contact.
	 *
	 * @param contactHandle Contact ID.
	 */
	public void updateNicknames(long contactHandle) {
		adapter.updateItem(contactHandle);
	}
}
