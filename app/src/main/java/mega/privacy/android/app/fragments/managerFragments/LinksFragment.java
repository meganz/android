package mega.privacy.android.app.fragments.managerFragments;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.MegaNodeBaseFragment;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import nz.mega.sdk.MegaNode;

import static android.view.MenuItem.*;
import static mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class LinksFragment extends MegaNodeBaseFragment {

    public static LinksFragment newInstance() {
        return new LinksFragment();
    }

    @Override
    public void activateActionMode() {
        super.activateActionMode();
        actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionBarCallBack());
    }

    private class ActionBarCallBack extends BaseActionBarCallBack {

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            checkSelectOptions(menu, false);

            if (managerActivity.getDeepBrowserTreeLinks() == 0) {
                menu.findItem(R.id.cab_menu_download).setVisible(false);
                menu.findItem(R.id.cab_menu_rename).setVisible(false);
                menu.findItem(R.id.cab_menu_copy).setVisible(false);
                menu.findItem(R.id.cab_menu_move).setVisible(false);
                showRemoveLink = true;
                for (MegaNode node : selected) {
                    if (node.isTakenDown()) {
                        showRemoveLink = false;
                    }
                }
                menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
                menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.cab_menu_trash).setVisible(false);
            } else {
                checkOptions();

                menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));
                menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
                menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
                int show;
                if (onlyOneFileSelected) {
                    show = SHOW_AS_ACTION_NEVER;
                } else {
                    show = SHOW_AS_ACTION_ALWAYS;
                }
                menu.findItem(R.id.cab_menu_copy).setShowAsAction(show);
                menu.findItem(R.id.cab_menu_move).setShowAsAction(show);

                menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
                menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
                menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);
                if(showLink){
                    menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(SHOW_AS_ACTION_NEVER);
                }else{
                    menu.findItem(R.id.cab_menu_share_link).setShowAsAction(SHOW_AS_ACTION_NEVER);
                    menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                menu.findItem(R.id.cab_menu_share).setVisible(showShare);
                menu.findItem(R.id.cab_menu_share).setTitle(context.getResources().getQuantityString(R.plurals.context_share_folders, selected.size()));
                if (onlyOneFileSelected) {
                    menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                } else {
                    menu.findItem(R.id.cab_menu_share).setShowAsAction(SHOW_AS_ACTION_NEVER);
                }

                menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
            }

            menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (megaApi.getRootNode() == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

        recyclerView = v.findViewById(R.id.file_list_view_browser);
        recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setClipToPadding(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
        fastScroller = v.findViewById(R.id.fastscroll);
        fastScroller.setRecyclerView(recyclerView);

        emptyImageView = v.findViewById(R.id.file_list_empty_image);
        emptyLinearLayout = v.findViewById(R.id.file_list_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);

        if (adapter == null) {
            adapter = new MegaNodeAdapter(context, this, nodes, managerActivity.getParentHandleLinks(), recyclerView, null, LINKS_ADAPTER, ITEM_VIEW_TYPE_LIST);
        } else {
            adapter.setAdapterType(ITEM_VIEW_TYPE_LIST);
        }

        adapter.setListFragment(recyclerView);

        if (managerActivity.getParentHandleLinks() == INVALID_HANDLE) {
            logWarning("ParentHandle -1");
            findNodes();
            adapter.setParentHandle(INVALID_HANDLE);
        } else {
            MegaNode parentNode = megaApi.getNodeByHandle(managerActivity.getParentHandleLinks());
            logDebug("ParentHandle to find children: " + managerActivity.getParentHandleLinks());

            nodes = megaApi.getChildren(parentNode, getLinksOrderCloud());
            addSectionTitle(nodes, adapter.getAdapterType());
            adapter.setNodes(nodes);
        }

        adapter.setMultipleSelect(false);
        recyclerView.setAdapter(adapter);

        return v;
    }

    private void findNodes() {
        setNodes(megaApi.getPublicLinks(getLinksOrderCloud()));
    }

    @Override
    public void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = nodes;
        addSectionTitle(nodes, ITEM_VIEW_TYPE_LIST);
        adapter.setNodes(nodes);
        setEmptyView();
        visibilityFastScroller();
    }

    @Override
    protected void setEmptyView() {
        String textToShow;

        if (megaApi.getRootNode().getHandle() == managerActivity.getParentHandleOutgoing()
                || managerActivity.getParentHandleOutgoing() == -1) {
            emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links);
            textToShow = String.format(context.getString(R.string.context_empty_links));
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
            logWarning("Exception formatting string", e);
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

    @Override
    public int onBackPressed() {
        if (adapter == null
                || managerActivity.getParentHandleLinks() == INVALID_HANDLE
                || managerActivity.getDeepBrowserTreeLinks() <= 0) {
            return 0;
        }

        managerActivity.decreaseDeepBrowserTreeLinks();

        if (managerActivity.getDeepBrowserTreeLinks() == 0) {
            managerActivity.setParentHandleLinks(INVALID_HANDLE);
            findNodes();
        } else if (managerActivity.getDeepBrowserTreeLinks() > 0) {
            MegaNode parentNodeLinks = megaApi.getNodeByHandle(managerActivity.getParentHandleLinks());
            if (parentNodeLinks != null) {
                parentNodeLinks = megaApi.getParentNode(parentNodeLinks);
                if (parentNodeLinks != null) {
                    managerActivity.setParentHandleLinks(parentNodeLinks.getHandle());
                    setNodes(megaApi.getChildren(parentNodeLinks, getLinksOrderCloud()));
                }
            }
        } else {
            managerActivity.setDeepBrowserTreeLinks(0);
        }

        int lastVisiblePosition = 0;
        if (!lastPositionStack.empty()) {
            lastVisiblePosition = lastPositionStack.pop();
        }
        if (lastVisiblePosition >= 0) {
            mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
        }

        managerActivity.showFabButton();
        managerActivity.setToolbarTitle();
        managerActivity.supportInvalidateOptionsMenu();

        return 1;
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
        } else {
            MegaNode n = nodes.get(position);

            if (n.isFolder()) {
                lastPositionStack.push(mLayoutManager.findFirstCompletelyVisibleItemPosition());
                managerActivity.increaseDeepBrowserTreeLinks();
                managerActivity.setParentHandleLinks(n.getHandle());
                managerActivity.supportInvalidateOptionsMenu();
                managerActivity.setToolbarTitle();

                setNodes(megaApi.getChildren(n, getLinksOrderCloud()));
                recyclerView.scrollToPosition(0);
                checkScroll();
                managerActivity.showFabButton();
            } else {
                openFile(n, LINKS_ADAPTER, position, screenPosition, imageView);
            }
        }
    }

    @Override
    public void refresh() {
        clearSelections();
        hideMultipleSelect();

        if (managerActivity.getParentHandleLinks() == INVALID_HANDLE
                || megaApi.getNodeByHandle(managerActivity.getParentHandleLinks()) == null) {
            findNodes();
        } else {
            MegaNode parentNodeLinks = megaApi.getNodeByHandle(managerActivity.getParentHandleLinks());
            setNodes(megaApi.getChildren(parentNodeLinks, getLinksOrderCloud()));
        }
    }
}
