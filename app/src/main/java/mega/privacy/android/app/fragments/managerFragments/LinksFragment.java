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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_LIST;
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
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
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
            adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).getParentHandleLinks(), recyclerView, null, LINKS_ADAPTER, ITEM_VIEW_TYPE_LIST);
        } else {
            adapter.setAdapterType(ITEM_VIEW_TYPE_LIST);
        }

        adapter.setListFragment(recyclerView);

        if (((ManagerActivityLollipop) context).getParentHandleLinks() == INVALID_HANDLE) {
            logWarning("ParentHandle -1");
            findNodes();
            adapter.setParentHandle(INVALID_HANDLE);
        } else {
            MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleLinks());
            logDebug("ParentHandle to find children: " + ((ManagerActivityLollipop) context).getParentHandleLinks());

            nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
            addSectionTitle(nodes, adapter.getAdapterType());
            adapter.setNodes(nodes);
        }

        adapter.setMultipleSelect(false);
        recyclerView.setAdapter(adapter);

        setEmptyView();
        visibilityFastScroller();

        return v;
    }

    private void findNodes() {
        nodes = megaApi.getPublicLinks(ORDER_DEFAULT_ASC);

        addSectionTitle(nodes, adapter.getAdapterType());
        adapter.setNodes(nodes);

        setEmptyView();
    }

    @Override
    public void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = nodes;
        addSectionTitle(nodes, ITEM_VIEW_TYPE_LIST);
        adapter.setNodes(nodes);
    }

    @Override
    protected void setEmptyView() {
        String textToShow;

        if (megaApi.getRootNode().getHandle() == ((ManagerActivityLollipop) context).getParentHandleOutgoing()
                || ((ManagerActivityLollipop) context).getParentHandleOutgoing() == -1) {
            if (isScreenInPortrait(context)) {
                emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links);
            } else {
                emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links);
            }
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
        if (adapter == null || ((ManagerActivityLollipop) context).getParentHandleLinks() == INVALID_HANDLE) {
            return 0;
        }
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
            if (nodes.get(position).isFolder()) {
                MegaNode n = nodes.get(position);

                lastPositionStack.push(mLayoutManager.findFirstCompletelyVisibleItemPosition());

                ((ManagerActivityLollipop) context).setParentHandleLinks(n.getHandle());
                ((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();
                ((ManagerActivityLollipop) context).setToolbarTitle();

                nodes = megaApi.getChildren(n, ((ManagerActivityLollipop) context).orderCloud);
                addSectionTitle(nodes, adapter.getAdapterType());

                adapter.setNodes(nodes);
                recyclerView.scrollToPosition(0);
                visibilityFastScroller();
                setEmptyView();
                checkScroll();
                ((ManagerActivityLollipop) context).showFabButton();
            } else {
                //Is file
            }
        }
    }

    @Override
    public void refresh() {

    }
}
