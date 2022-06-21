package mega.privacy.android.app.main.managerSections;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.main.ManagerActivity.INCOMING_TAB;
import static mega.privacy.android.app.utils.MegaNodeUtil.allHaveFullAccess;
import static mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaError.API_OK;
import static nz.mega.sdk.MegaShare.ACCESS_FULL;

public class IncomingSharesFragment extends MegaNodeBaseFragment {

	@Override
	public void activateActionMode() {
		if (!adapter.isMultipleSelect()) {
			super.activateActionMode();

			if (getActivity() != null) {
				actionMode = ((AppCompatActivity) getActivity())
						.startSupportActionMode(new ActionBarCallBack(INCOMING_TAB));
			}
		}
	}

	@Override
	protected int viewerFrom() {
		return VIEWER_FROM_INCOMING_SHARES;
	}

	private class ActionBarCallBack extends BaseActionBarCallBack {

		public ActionBarCallBack(int currentTab) {
			super(currentTab);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			super.onPrepareActionMode(mode, menu);

			CloudStorageOptionControlUtil.Control control =
					new CloudStorageOptionControlUtil.Control();

			if (managerActivity.getDeepBrowserTreeIncoming() == 0) {
				control.leaveShare().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			} else if (areAllFileNodesAndNotTakenDown(selected)) {
				control.sendToChat().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (selected.size() == 1
					&& megaApi.checkAccessErrorExtended(selected.get(0), ACCESS_FULL).getErrorCode() == API_OK) {
				control.rename().setVisible(true);
				if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
					control.rename().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				} else {
					control.rename().setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}
			}

			if (managerActivity.getDeepBrowserTreeIncoming() > 0 && selected.size() > 0
					&& allHaveFullAccess(selected)) {
				control.move().setVisible(true);

				if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
					control.move().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				} else {
					control.move().setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}
			}

			if (areAllNotTakenDown(selected)) {
				control.copy().setVisible(true);
				if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
					control.copy().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				} else {
					control.copy().setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}
			} else {
				control.saveToDevice().setVisible(false);
			}

			control.selectAll().setVisible(notAllNodesSelected());
			control.trash().setVisible(managerActivity.getDeepBrowserTreeIncoming() > 0
					&& allHaveFullAccess(selected));

			CloudStorageOptionControlUtil.applyControl(menu, control);

			return true;
		}
	}

	public static IncomingSharesFragment newInstance() {
		return new IncomingSharesFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		logDebug("Parent Handle: " + managerActivity.getParentHandleIncoming());

		if (megaApi.getRootNode() == null) {
			return null;
		}

		managerActivity.showFabButton();

		View v;

		if (managerActivity.isList) {
			v = getListView(inflater, container);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes,
						managerActivity.getParentHandleIncoming(), recyclerView,
						INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST, sortByHeaderViewModel);
			}
		} else {
			v = getGridView(inflater, container);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes,
						managerActivity.getParentHandleIncoming(), recyclerView,
						INCOMING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID, sortByHeaderViewModel);
			}

			gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(gridLayoutManager.getSpanCount()));
		}

		adapter.setParentHandle(managerActivity.getParentHandleIncoming());
		adapter.setListFragment(recyclerView);

		if (managerActivity.getParentHandleIncoming() == INVALID_HANDLE) {
			logWarning("ParentHandle -1");
			findNodes();
		} else {
			managerActivity.hideTabs(true, INCOMING_TAB);
			MegaNode parentNode = megaApi.getNodeByHandle(managerActivity.getParentHandleIncoming());
			logDebug("ParentHandle to find children: " + managerActivity.getParentHandleIncoming());

			nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
			adapter.setNodes(nodes);
		}

		managerActivity.supportInvalidateOptionsMenu();

		adapter.setMultipleSelect(false);
		recyclerView.setAdapter(adapter);
		visibilityFastScroller();
		setEmptyView();

		selectNewlyAddedNodes();

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
			nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
			adapter.setNodes(nodes);
		}

		managerActivity.supportInvalidateOptionsMenu();

		visibilityFastScroller();
		hideActionMode();
		setEmptyView();
	}

	@Override
	public void itemClick(int position) {
		if (adapter.isMultipleSelect()) {
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else if (nodes.get(position).isFolder()) {
			navigateToFolder(nodes.get(position));
		} else {
			openFile(nodes.get(position), INCOMING_SHARES_ADAPTER, position);
		}
	}

	@Override
	public void navigateToFolder(MegaNode node) {
		managerActivity.hideTabs(true, INCOMING_TAB);
		managerActivity.increaseDeepBrowserTreeIncoming();
		logDebug("Is folder deep: " + managerActivity.deepBrowserTreeIncoming);

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

		managerActivity.setParentHandleIncoming(node.getHandle());
		managerActivity.supportInvalidateOptionsMenu();
		managerActivity.setToolbarTitle();

		nodes = megaApi.getChildren(node, sortOrderManagement.getOrderCloud());

		adapter.setNodes(nodes);
		recyclerView.scrollToPosition(0);
		visibilityFastScroller();
		setEmptyView();
		checkScroll();
		managerActivity.showFabButton();
	}

	public void findNodes() {
		nodes = megaApi.getInShares(sortOrderManagement.getOrderOthers());
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
			managerActivity.restoreSharesAfterComingFromNotifications();
			return 4;
		} else {
			managerActivity.decreaseDeepBrowserTreeIncoming();
			managerActivity.supportInvalidateOptionsMenu();

			if (managerActivity.deepBrowserTreeIncoming == 0) {
				//In the beginning of the navigation

				logDebug("deepBrowserTree==0");
				managerActivity.setParentHandleIncoming(INVALID_HANDLE);
				managerActivity.hideTabs(false, INCOMING_TAB);
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

					nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());

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
		adapter.setNodes(nodes);
	}

	@Override
	protected void setEmptyView() {
		String textToShow = null;

		if (megaApi.getRootNode().getHandle() == managerActivity.getParentHandleIncoming()
				|| managerActivity.getParentHandleIncoming() == -1) {
			ColorUtils.setImageViewAlphaIfDark(context, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA);

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
	public void updateContact(long contactHandle) {
		adapter.updateItem(contactHandle);
	}

	/**
	 * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
	 */
	private void selectNewlyAddedNodes() {
		ArrayList<Integer> positions = managerActivity.getPositionsList(nodes);
		if (!positions.isEmpty()) {
			int firstPosition = Collections.min(positions);
			activateActionMode();
			for (int position : positions) {
				if (adapter.isMultipleSelect()) {
					adapter.toggleSelection(position);
				}
			}
			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
			recyclerView.scrollToPosition(firstPosition);
		}
	}
}
