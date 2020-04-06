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
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static android.view.MenuItem.*;
import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class OutgoingSharesFragmentLollipop extends MegaNodeBaseFragment {

	@Override
	public void activateActionMode() {
		super.activateActionMode();
		actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionBarCallBack());
	}

	private class ActionBarCallBack extends BaseActionBarCallBack {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			checkSelectOptions(menu, false);

			if (managerActivity.getDeepBrowserTreeOutgoing() == 0) {
				menu.findItem(R.id.cab_menu_download).setVisible(false);
				menu.findItem(R.id.cab_menu_rename).setVisible(false);
				menu.findItem(R.id.cab_menu_share).setVisible(true);
				menu.findItem(R.id.cab_menu_remove_share).setIcon(mutateIconSecondary(context, R.drawable.ic_remove_share, R.color.white));
				menu.findItem(R.id.cab_menu_remove_share).setVisible(true);
				menu.findItem(R.id.cab_menu_remove_share).setShowAsAction(SHOW_AS_ACTION_ALWAYS);
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);
				menu.findItem(R.id.cab_menu_trash).setVisible(false);
			} else {
				checkOptions();

				menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));
				menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
				menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
				int show = onlyOneFileSelected ? SHOW_AS_ACTION_NEVER : SHOW_AS_ACTION_ALWAYS;
				menu.findItem(R.id.cab_menu_copy).setShowAsAction(show);
				menu.findItem(R.id.cab_menu_move).setShowAsAction(show);

				menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
				menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
				menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);
				if (showLink) {
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(SHOW_AS_ACTION_NEVER);
				} else {
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(SHOW_AS_ACTION_NEVER);
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}

				menu.findItem(R.id.cab_menu_share).setVisible(showShare);

				menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			}

			menu.findItem(R.id.cab_menu_share).setTitle(context.getResources().getQuantityString(R.plurals.context_share_folders, selected.size()));
			menu.findItem(R.id.cab_menu_share).setShowAsAction(SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			return false;
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
}
