package mega.privacy.android.app.main.providers;

import static mega.privacy.android.app.providers.FileProviderActivity.INCOMING_TAB;
import static mega.privacy.android.app.utils.Constants.INCOMING_SHARES_PROVIDER_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.main.CheckScrollInterface;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;


public class IncomingSharesProviderFragment extends Fragment implements CheckScrollInterface {

    Context context;
    MegaApiAndroid megaApi;
    ArrayList<MegaNode> nodes;
    long parentHandle = -1;

    long[] hashes;

    MegaProviderAdapter adapter;
    public String name;

    RecyclerView listView;
    LinearLayoutManager mLayoutManager;

    ImageView emptyImageView;
    TextView emptyTextViewFirst;

    int deepBrowserTree = -1;

    Stack<Integer> lastPositionStack;

    public ActionMode actionMode;

    Handler handler;

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
        }
    }

    @Override
    public void checkScroll() {
        ((FileProviderActivity) context)
                .changeActionBarElevation(listView.canScrollVertically(SCROLLING_UP_DIRECTION)
                        || (adapter != null && adapter.isMultipleSelect()), INCOMING_TAB);
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_browser_action, menu);
            ((FileProviderActivity) context).hideTabs(true, INCOMING_TAB);
            checkScroll();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            List<MegaNode> selected = adapter.getSelectedNodes();

            menu.findItem(R.id.cab_menu_share_link)
                    .setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size()));

            boolean showDownload = false;
            boolean showRename = false;
            boolean showCopy = false;
            boolean showMove = false;
            boolean showLink = false;
            boolean showEditLink = false;
            boolean showRemoveLink = false;
            boolean showTrash = false;
            boolean showShare = false;

            if (selected.size() != 0) {

                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if (selected.size() == adapter.getItemCount()) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }


            menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
            menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

            menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);

            menu.findItem(R.id.cab_menu_move).setVisible(showMove);

            menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

            menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);

            menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

            menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);

            menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
            menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

            menu.findItem(R.id.cab_menu_share).setVisible(showShare);
            menu.findItem(R.id.cab_menu_share).setTitle(context.getResources().getQuantityString(R.plurals.context_share_folders, selected.size()));

            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            List<MegaNode> documents = adapter.getSelectedNodes();

            switch (item.getItemId()) {
                case R.id.action_mode_close_button: {
                    Timber.d("Close button");
                }
                case R.id.cab_menu_select_all: {
                    selectAll();
                    break;
                }
                case R.id.cab_menu_unselect_all: {
                    clearSelections();
                    hideMultipleSelect();
                    break;
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Timber.d("onDestroyActionMode");
            clearSelections();
            adapter.setMultipleSelect(false);
            ((FileProviderActivity) context).hideTabs(false, INCOMING_TAB);
            checkScroll();
        }
    }


    public static IncomingSharesProviderFragment newInstance() {
        Timber.d("newInstance");
        IncomingSharesProviderFragment fragment = new IncomingSharesProviderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaApi.getRootNode() == null) {
            return;
        }

        lastPositionStack = new Stack<>();

        nodes = new ArrayList<MegaNode>();

        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);

        Display display = getActivity().getWindowManager().getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        listView = (RecyclerView) v.findViewById(R.id.provider_list_view_browser);

        listView.addItemDecoration(new SimpleDividerItemDecoration(context));
        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        emptyImageView = (ImageView) v.findViewById(R.id.provider_list_empty_image);
        emptyTextViewFirst = (TextView) v.findViewById(R.id.provider_list_empty_text_first);

        if (context instanceof FileProviderActivity) {
            parentHandle = ((FileProviderActivity) context).getIncParentHandle();
            deepBrowserTree = ((FileProviderActivity) context).getIncomingDeepBrowserTree();
            Timber.d("The parent handle is: %s", parentHandle);
            Timber.d("The browser tree deep is: %s", deepBrowserTree);
        }

        if (adapter == null) {
            adapter = new MegaProviderAdapter(context, this, nodes, parentHandle, listView, emptyImageView, INCOMING_SHARES_PROVIDER_ADAPTER);
        }
        listView.setAdapter(adapter);

        if (parentHandle == -1) {
            findNodes();
            setNodes(nodes);
            adapter.setParentHandle(-1);
        } else {
            adapter.setParentHandle(parentHandle);
            MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
            if (parentNode != null) {
                nodes = megaApi.getChildren(parentNode);
                setNodes(nodes);
                Timber.d("INCOMING is in: %s", parentNode.getName());
                changeActionBarTitle(parentNode.getName());
            } else {
                Timber.w("ERROR parentNode is NULL");
                findNodes();
                setNodes(nodes);
                parentHandle = -1;
                adapter.setParentHandle(-1);
                changeActionBarTitle(getString(R.string.file_provider_title));
                if (context instanceof FileProviderActivity) {
                    ((FileProviderActivity) context).setParentHandle(parentHandle);
                    Timber.d("ParentHandle change to: %s", parentHandle);
                }
            }
        }

        adapter.setPositionClicked(-1);

        return v;
    }

    public void findNodes() {
        Timber.d("findNodes");

        deepBrowserTree = 0;
        if (context instanceof FileProviderActivity) {
            ((FileProviderActivity) context).setIncomingDeepBrowserTree(deepBrowserTree);
            Timber.d("The browser tree change to: %s", deepBrowserTree);
        }
        ArrayList<MegaUser> contacts = megaApi.getContacts();
        nodes.clear();
        for (int i = 0; i < contacts.size(); i++) {
            ArrayList<MegaNode> nodeContact = megaApi.getInShares(contacts.get(i));
            if (nodeContact != null) {
                if (nodeContact.size() > 0) {
                    nodes.addAll(nodeContact);
                }
            }
        }

        changeActionBarTitle(getString(R.string.file_provider_title));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    public void changeActionBarTitle(String folder) {
        if (context instanceof FileProviderActivity) {
            int tabShown = ((FileProviderActivity) context).getTabShown();

            if (tabShown == FileProviderActivity.INCOMING_TAB) {
                ((FileProviderActivity) context).changeTitle(folder);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
        Timber.d("deepBrowserTree: %s", deepBrowserTree);
        if (adapter.isMultipleSelect()) {
            Timber.d("Multiselect ON");
            adapter.toggleSelection(position);

            List<MegaNode> selectedNodes = adapter.getSelectedNodes();
            if (selectedNodes.size() > 0) {
                updateActionModeTitle();
                ((FileProviderActivity) context).activateButton(true);
                ((FileProviderActivity) context).attachFiles(selectedNodes);
            } else {

                ((FileProviderActivity) context).activateButton(false);
            }
        } else {
            ((FileProviderActivity) context).activateButton(false);
            if (nodes.get(position).isFolder()) {

                deepBrowserTree = deepBrowserTree + 1;
                if (context instanceof FileProviderActivity) {
                    ((FileProviderActivity) context).hideTabs(true, INCOMING_TAB);
                    ((FileProviderActivity) context).setIncomingDeepBrowserTree(deepBrowserTree);
                    Timber.d("The browser tree change to: %s", deepBrowserTree);
                }

                MegaNode n = nodes.get(position);

                int lastFirstVisiblePosition = 0;

                lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

                Timber.d("Push to stack %d position", lastFirstVisiblePosition);
                lastPositionStack.push(lastFirstVisiblePosition);

                String path = n.getName();
                String[] temp;
                temp = path.split("/");
                name = temp[temp.length - 1];

                changeActionBarTitle(name);

                parentHandle = nodes.get(position).getHandle();
                if (context instanceof FileProviderActivity) {
                    ((FileProviderActivity) context).setIncParentHandle(parentHandle);
                    Timber.d("The parent handle change to: %s", parentHandle);
                }
                adapter.setParentHandle(parentHandle);
                nodes = megaApi.getChildren(nodes.get(position));
                setNodes(nodes);
                listView.scrollToPosition(0);
            } else {
                //File selected to download
                MegaNode n = nodes.get(position);
                ((FileProviderActivity) context).downloadAndAttachAfterClick(n.getHandle());
            }
        }
    }


    public int onBackPressed() {
        Timber.d("deepBrowserTree: %s", deepBrowserTree);
        deepBrowserTree = deepBrowserTree - 1;
        if (context instanceof FileProviderActivity) {
            ((FileProviderActivity) context).setIncomingDeepBrowserTree(deepBrowserTree);
            Timber.d("The browser tree change to: %s", deepBrowserTree);
        }

        if (deepBrowserTree == 0) {
            parentHandle = INVALID_HANDLE;

            if (context instanceof FileProviderActivity) {
                ((FileProviderActivity) context).setIncParentHandle(parentHandle);
                ((FileProviderActivity) context).hideTabs(false, INCOMING_TAB);
                Timber.d("The parent handle change to: %s", parentHandle);
            }

            changeActionBarTitle(getString(R.string.file_provider_title));
            findNodes();

            setNodes(nodes);
            int lastVisiblePosition = 0;
            if (!lastPositionStack.empty()) {
                lastVisiblePosition = lastPositionStack.pop();
                Timber.d("Pop of the stack %d position", lastVisiblePosition);
            }
            Timber.d("Scroll to %d position", lastVisiblePosition);

            if (lastVisiblePosition >= 0) {
                mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
            }
            adapter.setParentHandle(parentHandle);

            return 3;
        } else if (deepBrowserTree > 0) {
            parentHandle = adapter.getParentHandle();
            if (context instanceof FileProviderActivity) {
                ((FileProviderActivity) context).setIncParentHandle(parentHandle);
                Timber.d("The parent handle change to: %s", parentHandle);
            }
            MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));

            if (parentNode != null) {
                listView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextViewFirst.setVisibility(View.GONE);

                changeActionBarTitle(parentNode.getName());

                parentHandle = parentNode.getHandle();
                if (context instanceof FileProviderActivity) {
                    ((FileProviderActivity) context).setIncParentHandle(parentHandle);
                    Timber.d("The parent handle change to: %s", parentHandle);
                }
                nodes = megaApi.getChildren(parentNode);

                setNodes(nodes);
                int lastVisiblePosition = 0;
                if (!lastPositionStack.empty()) {
                    lastVisiblePosition = lastPositionStack.pop();
                    Timber.d("Pop of the stack %d position", lastVisiblePosition);
                }
                Timber.d("Scroll to %d position", lastVisiblePosition);

                if (lastVisiblePosition >= 0) {
                    mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
                }
                adapter.setParentHandle(parentHandle);
                return 2;
            }
            return 2;
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextViewFirst.setVisibility(View.GONE);
            deepBrowserTree = 0;
            if (context instanceof FileProviderActivity) {
                ((FileProviderActivity) context).setIncomingDeepBrowserTree(deepBrowserTree);
                Timber.d("The browser tree change to: %s", deepBrowserTree);
            }
            return 0;
        }
    }

    public long getParentHandle() {
        return adapter.getParentHandle();
    }

    public void setParentHandle(long parentHandle) {
        Timber.d("setParentHandle");
        this.parentHandle = parentHandle;
        if (context instanceof FileProviderActivity) {
            ((FileProviderActivity) context).setIncParentHandle(parentHandle);
            Timber.d("The parent handle change to: %s", parentHandle);
        }
        if (adapter != null) {
            adapter.setParentHandle(parentHandle);
        }
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        Timber.d("setNodes");
        this.nodes = nodes;
        if (adapter != null) {
            adapter.setNodes(nodes);
            if (adapter.getItemCount() == 0) {
                listView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextViewFirst.setVisibility(View.VISIBLE);

                if (parentHandle == -1) {
                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
                    } else {
                        emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
                    }
                    String textToShow = String.format(context.getString(R.string.context_empty_incoming));
                    try {
                        textToShow = textToShow.replace(
                                "[A]", "<font color=\'"
                                        + ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                        + "\'>"
                        ).replace("[/A]", "</font>").replace(
                                "[B]", "<font color=\'"
                                        + ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                        + "\'>"
                        ).replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    emptyTextViewFirst.setText(result);
                } else {
                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
                    } else {
                        emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
                    }
                    String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
                    try {
                        textToShow = textToShow.replace(
                                "[A]", "<font color=\'"
                                        + ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
                                        + "\'>"
                        ).replace("[/A]", "</font>").replace(
                                "[B]", "<font color=\'"
                                        + ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
                                        + "\'>"
                        ).replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    emptyTextViewFirst.setText(result);
                }
            } else {
                listView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextViewFirst.setVisibility(View.GONE);
            }
        }
    }

    public RecyclerView getListView() {
        return listView;
    }

    public int getDeepBrowserTree() {
        return deepBrowserTree;
    }

    public void setDeepBrowserTree(int deepBrowserTree) {
        this.deepBrowserTree = deepBrowserTree;
    }

    public void hideMultipleSelect() {
        Timber.d("hideMultipleSelect");
        adapter.setMultipleSelect(false);

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void selectAll() {
        Timber.d("selectAll");
        if (adapter != null) {
            adapter.selectAll();
        } else {
            adapter.setMultipleSelect(true);
            adapter.selectAll();

            actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
        }

        new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null || getActivity() == null) {
            Timber.d("RETURN: actionMode == null || getActivity() == null");
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
        int sum = files + folders;

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
            Timber.e(e, "Invalidate error");
        }

    }

    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
            ((FileProviderActivity) context).activateButton(false);
        }
    }
}
