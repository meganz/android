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
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class OutgoingSharesFragmentLollipop extends MegaNodeBaseFragment {

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
			super.onPrepareActionMode(mode, menu);

			CloudStorageOptionControlUtil.Control control =
					new CloudStorageOptionControlUtil.Control();

			if (selected.size() == 1) {
				if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode()
						== MegaError.API_OK) {
					if (selected.get(0).isExported()) {
						control.manageLink().setVisible(true)
								.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

						control.removeLink().setVisible(true);
					} else {
						control.getLink().setVisible(true)
								.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					}
				}
				if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode()
						== MegaError.API_OK) {
					control.rename().setVisible(true);
				}

				control.shareFolder().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				if (MegaNodeUtil.isOutShare(selected.get(0))) {
					control.removeShare().setVisible(true);
				}
			} else {
				menu.findItem(R.id.cab_menu_remove_share)
						.setIcon(mutateIconSecondary(context, R.drawable.ic_remove_share,
								R.color.white));
				control.removeShare().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				control.copy().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			control.shareOut().setVisible(true)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			control.copy().setVisible(true);

			control.trash().setVisible(MegaNodeUtil.canMoveToRubbish(selected));
			control.selectAll().setVisible(notAllNodesSelected());

			CloudStorageOptionControlUtil.applyControl(menu, control);

			return true;
		}
	}

	public static OutgoingSharesFragmentLollipop newInstance() {
		return new OutgoingSharesFragmentLollipop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		if (megaApi.getRootNode() == null) {
			return null;
		}

		managerActivity.showFabButton();

		View v;

		if (managerActivity.isList) {
			v = getListView(inflater, container);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, managerActivity.getParentHandleOutgoing(), recyclerView, null, OUTGOING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
		} else {
			v = getGridView(inflater, container);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, managerActivity.getParentHandleOutgoing(), recyclerView, null, OUTGOING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}

		adapter.setParentHandle(managerActivity.getParentHandleOutgoing());
		adapter.setListFragment(recyclerView);

		if (managerActivity.getParentHandleOutgoing() == INVALID_HANDLE) {
			logWarning("Parent Handle == -1");
			findNodes();
			adapter.setParentHandle(INVALID_HANDLE);
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(managerActivity.getParentHandleOutgoing());
			logDebug("Parent Handle: " + managerActivity.getParentHandleOutgoing());

			nodes = megaApi.getChildren(parentNode, managerActivity.orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		managerActivity.setToolbarTitle();
		managerActivity.supportInvalidateOptionsMenu();

		adapter.setMultipleSelect(false);
		recyclerView.setAdapter(adapter);
		visibilityFastScroller();
		setEmptyView();

		return v;
	}

	@Override
	public void refresh() {
		logDebug("Parent Handle: " + managerActivity.getParentHandleOutgoing());

		if (managerActivity.getParentHandleOutgoing() == -1) {
			findNodes();
		} else {
			MegaNode n = megaApi.getNodeByHandle(managerActivity.getParentHandleOutgoing());
			managerActivity.setToolbarTitle();

			nodes = megaApi.getChildren(n, managerActivity.orderCloud);
			addSectionTitle(nodes, adapter.getAdapterType());
			adapter.setNodes(nodes);
		}

		managerActivity.supportInvalidateOptionsMenu();

		visibilityFastScroller();
		hideActionMode();
		setEmptyView();
	}

	public void findNodes() {
		ArrayList<MegaShare> outNodeList = megaApi.getOutShares();

		nodes.clear();
		long lastFolder = -1;

		for (int k = 0; k < outNodeList.size(); k++) {
			if (outNodeList.get(k).getUser() != null) {
				MegaShare mS = outNodeList.get(k);
				MegaNode node = megaApi.getNodeByHandle(mS.getNodeHandle());

				if (lastFolder != node.getHandle()) {
					lastFolder = node.getHandle();
					nodes.add(node);
				}
			}
		}

		orderNodes();
	}

	@Override
	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		if (adapter.isMultipleSelect()) {
			logDebug("multiselect ON");

			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else if (nodes.get(position).isFolder()) {
			managerActivity.increaseDeepBrowserTreeOutgoing();
			logDebug("deepBrowserTree after clicking folder" + managerActivity.deepBrowserTreeOutgoing);

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

			managerActivity.setParentHandleOutgoing(n.getHandle());

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
			//Is file
			openFile(nodes.get(position), OUTGOING_SHARES_ADAPTER, position, screenPosition, imageView);
		}
	}

	@Override
	public void setNodes(ArrayList<MegaNode> nodes) {
		this.nodes = nodes;
		orderNodes();
	}

	protected void orderNodes() {
		if (managerActivity.orderOthers == MegaApiJava.ORDER_DEFAULT_DESC) {
			sortByNameDescending(this.nodes);
		} else {
			sortByNameAscending(this.nodes);
		}

		addSectionTitle(nodes, adapter.getAdapterType());
		adapter.setNodes(nodes);
	}

	@Override
	public int onBackPressed() {
		logDebug("deepBrowserTree: " + managerActivity.deepBrowserTreeOutgoing);

		if (adapter == null) {
			return 0;
		}

		managerActivity.decreaseDeepBrowserTreeOutgoing();
		managerActivity.supportInvalidateOptionsMenu();
		if (managerActivity.deepBrowserTreeOutgoing == 0) {
			logDebug("deepBrowserTree==0");
			//In the beginning of the navigation
			managerActivity.setParentHandleOutgoing(-1);

			managerActivity.setToolbarTitle();
			findNodes();
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

			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyLinearLayout.setVisibility(View.GONE);
			managerActivity.showFabButton();
			return 3;
		} else if (managerActivity.deepBrowserTreeOutgoing > 0) {
			logDebug("Keep navigation");

			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(managerActivity.getParentHandleOutgoing()));

			if (parentNode != null) {
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);

				managerActivity.setParentHandleOutgoing(parentNode.getHandle());

				managerActivity.setToolbarTitle();
				managerActivity.supportInvalidateOptionsMenu();

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
			logDebug("Back to Cloud");
			managerActivity.deepBrowserTreeOutgoing = 0;
			return 0;
		}
	}

	@Override
	protected void setEmptyView() {
		String textToShow = null;

		if (megaApi.getRootNode().getHandle() == managerActivity.getParentHandleOutgoing()
				|| managerActivity.getParentHandleOutgoing() == -1) {
			if (isScreenInPortrait(context)) {
				emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
			} else {
				emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
			}
			textToShow = context.getString(R.string.context_empty_outgoing);
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
}
