package mega.privacy.android.app.main;

import static mega.privacy.android.app.main.FileExplorerActivity.CLOUD_FRAGMENT;
import static mega.privacy.android.app.search.usecase.SearchNodesUseCase.TYPE_CLOUD_EXPLORER;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText;
import static mega.privacy.android.app.utils.Util.getPreferences;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.managerSections.RotatableFragment;
import mega.privacy.android.app.search.callback.SearchCallback;
import mega.privacy.android.app.search.usecase.SearchNodesUseCase;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaCancelToken;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

@AndroidEntryPoint
public class CloudDriveExplorerFragment extends RotatableFragment implements
        OnClickListener, CheckScrollInterface, SearchCallback.View, SearchCallback.Data {

    @Inject
    SortOrderManagement sortOrderManagement;
    @Inject
    SearchNodesUseCase searchNodesUseCase;
    @Inject
    DatabaseHandler dbH;

    private Context context;
    private MegaApiAndroid megaApi;
    private ArrayList<MegaNode> nodes;
    private ArrayList<MegaNode> searchNodes;

    private long parentHandle = -1;

    private MegaExplorerAdapter adapter;
    private FastScroller fastScroller;

    private int modeCloud;
    private boolean selectFile = false;

    private ActionMode actionMode;

    private RelativeLayout contentLayout;
    private LinearLayout optionsBar;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private CustomizedGridLayoutManager gridLayoutManager;

    private ImageView emptyImageView;
    private LinearLayout emptyTextView;
    private TextView emptyTextViewFirst;

    private Button optionButton;
    private FloatingActionButton fabSelect;

    private Stack<Integer> lastPositionStack;

    private Handler handler;

    private int order;

    private MegaCancelToken searchCancelToken;
    private ProgressBar searchProgressBar;
    private boolean shouldResetNodes = true;

    private Spanned emptyRootText;
    private Spanned emptyGeneralText;

    @Override
    protected RotatableAdapter getAdapter() {
        return adapter;
    }

    public void activateActionMode() {
        Timber.d("activateActionMode");

        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());

            if (isMultiselect()) {
                activateButton(true);
            }
        }
    }

    @Override
    public void multipleItemClick(int position) {
        adapter.toggleSelection(position);
    }

    @Override
    public void reselectUnHandledSingleItem(int position) {
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            List<MegaNode> documents = adapter.getSelectedNodes();

            switch (item.getItemId()) {

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
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_explorer_multiaction, menu);
            ((FileExplorerActivity) context).hideTabs(true, CLOUD_FRAGMENT);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            if (!((FileExplorerActivity) context).shouldReopenSearch()) {
                ((FileExplorerActivity) context).hideTabs(false, CLOUD_FRAGMENT);
                ((FileExplorerActivity) context).clearQuerySearch();
                getNodes();
                setNodes(nodes);
            }
            clearSelections();
            adapter.setMultipleSelect(false);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            List<MegaNode> selected = adapter.getSelectedNodes();

            if (selected.size() != 0) {
                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                MegaNode nodeS = megaApi.getNodeByHandle(parentHandle);

                if (selected.size() == megaApi.getNumChildFiles(nodeS)) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);

                } else {
                    if (modeCloud == FileExplorerActivity.SELECT) {
                        if (selectFile) {
                            if (((FileExplorerActivity) context).isMultiselect()) {
                                MegaNode node = megaApi.getNodeByHandle(parentHandle);
                                menu.findItem(R.id.cab_menu_select_all).setVisible(selected.size() != megaApi.getNumChildFiles(node));
                            }
                        }
                    } else {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    }

                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            return false;
        }
    }


    public static CloudDriveExplorerFragment newInstance() {
        Timber.d("newInstance");
        return new CloudDriveExplorerFragment();
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

        parentHandle = -1;
        lastPositionStack = new Stack<>();
        handler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void checkScroll() {
        if (recyclerView == null) {
            return;
        }

        ((FileExplorerActivity) context).changeActionBarElevation(
                recyclerView.canScrollVertically(SCROLLING_UP_DIRECTION)
                        || (adapter != null && adapter.isMultipleSelect()),
                CLOUD_FRAGMENT);
    }

    /**
     * Shows the Sort by panel.
     *
     * @param unit Unit event.
     * @return Null.
     */
    private Unit showSortByPanel(Unit unit) {
        ((FileExplorerActivity) context).showSortByPanel();
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        SortByHeaderViewModel sortByHeaderViewModel = new ViewModelProvider(this)
                .get(SortByHeaderViewModel.class);

        sortByHeaderViewModel.getShowDialogEvent().observe(getViewLifecycleOwner(),
                new EventObserver<>(this::showSortByPanel));

        View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
        Display display = requireActivity().getWindowManager().getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        contentLayout = v.findViewById(R.id.content_layout);
        searchProgressBar = v.findViewById(R.id.progressbar);

        optionsBar = v.findViewById(R.id.options_explorer_layout);
        optionButton = v.findViewById(R.id.action_text);
        optionButton.setOnClickListener(this);

        Button cancelButton = v.findViewById(R.id.cancel_text);
        cancelButton.setOnClickListener(this);
        cancelButton.setText(StringResourcesUtils.getString(R.string.general_cancel));
        fabSelect = v.findViewById(R.id.fab_select);
        fabSelect.setOnClickListener(this);

        fastScroller = v.findViewById(R.id.fastscroll);
        if (((FileExplorerActivity) context).isList()) {
            recyclerView = v.findViewById(R.id.file_list_view_browser);
            v.findViewById(R.id.file_grid_view_browser).setVisibility(View.GONE);
            mLayoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.addItemDecoration(new PositionDividerItemDecoration(requireContext(), getResources().getDisplayMetrics()));
        } else {
            recyclerView = v.findViewById(R.id.file_grid_view_browser);
            v.findViewById(R.id.file_list_view_browser).setVisibility(View.GONE);
            gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        emptyImageView = v.findViewById(R.id.file_list_empty_image);
        emptyTextView = v.findViewById(R.id.file_list_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);

        modeCloud = ((FileExplorerActivity) context).getMode();
        selectFile = ((FileExplorerActivity) context).isSelectFile();

        parentHandle = ((FileExplorerActivity) context).getParentHandleCloud();

        if (parentHandle != INVALID_HANDLE && megaApi.getRootNode() != null
                && parentHandle != megaApi.getRootNode().getHandle()) {
            ((FileExplorerActivity) context).hideTabs(true, CLOUD_FRAGMENT);
        }

        if (modeCloud == FileExplorerActivity.SELECT_CAMERA_FOLDER) {
            setParentHandle(-1);
        } else if (parentHandle == -1) {
            setParentHandle(megaApi.getRootNode().getHandle());
        }

        MegaPreferences prefs = getPreferences(context);
        order = prefs != null && prefs.getPreferredSortCloud() != null
                ? Integer.parseInt(prefs.getPreferredSortCloud())
                : MegaApiJava.ORDER_DEFAULT_ASC;

        getNodes();
        setParentHandle(parentHandle);

        switch (modeCloud) {
            case FileExplorerActivity.MOVE:
                optionButton.setText(StringResourcesUtils.getString(R.string.context_move));

                MegaNode parentMove = ((FileExplorerActivity) context).parentMoveCopy();
                activateButton(parentMove == null || parentMove.getHandle() != parentHandle);
                break;

            case FileExplorerActivity.COPY:
                optionButton.setText(StringResourcesUtils.getString(R.string.context_copy));

                MegaNode parentCopy = ((FileExplorerActivity) context).parentMoveCopy();
                activateButton(parentCopy == null || parentCopy.getHandle() != parentHandle);
                break;

            case FileExplorerActivity.UPLOAD:
                optionButton.setText(StringResourcesUtils.getString(R.string.context_upload));
                break;

            case FileExplorerActivity.IMPORT:
                optionButton.setText(StringResourcesUtils.getString(R.string.add_to_cloud));
                break;

            case FileExplorerActivity.SAVE:
                optionButton.setText(StringResourcesUtils.getString(R.string.save_action));
                break;

            case FileExplorerActivity.SELECT:
                optionsBar.setVisibility(View.GONE);
                activateButton(shouldShowOptionsBar(megaApi.getNodeByHandle(parentHandle)));
                //No break; needed: the text should be set with SELECT mode

            default:
                optionButton.setText(StringResourcesUtils.getString(R.string.general_select));
                break;
        }

        if (adapter == null) {
            adapter = new MegaExplorerAdapter(context, this, nodes, parentHandle,
                    recyclerView, selectFile, sortByHeaderViewModel);
        } else {
            adapter.setListFragment(recyclerView);
            adapter.setParentHandle(parentHandle);
            adapter.setSelectFile(selectFile);
        }

        if (gridLayoutManager != null) {
            gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(gridLayoutManager.getSpanCount()));
        }

        recyclerView.setAdapter(adapter);
        fastScroller.setRecyclerView(recyclerView);
        setNodes(nodes);

        if (((FileExplorerActivity) context).shouldRestartSearch()) {
            setWaitingForSearchedNodes(true);
            search(((FileExplorerActivity) context).getQuerySearch());
        }
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emptyRootText = formatEmptyScreenText(requireContext(),
                        StringResourcesUtils.getString(R.string.context_empty_cloud_drive));

        emptyGeneralText = formatEmptyScreenText(requireContext(),
                        StringResourcesUtils.getString(R.string.file_browser_empty_folder_new));

        updateEmptyScreen();
        super.onViewCreated(view, savedInstanceState);
    }

    private void getNodes() {
        MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);

        if (chosenNode != null && chosenNode.getType() != MegaNode.TYPE_ROOT) {
            nodes = megaApi.getChildren(chosenNode, order);
            Timber.d("chosenNode is: %s", chosenNode.getName());
            return;
        }

        MegaNode rootNode = megaApi.getRootNode();
        if (rootNode != null) {
            setParentHandle(rootNode.getHandle());
            nodes = megaApi.getChildren(rootNode, order);
        }
    }

    private void showEmptyScreen() {
        if (adapter == null) {
            return;
        }

        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            updateEmptyScreen();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void updateEmptyScreen() {
        if (megaApi.getRootNode().getHandle() == parentHandle) {
            emptyImageView.setImageResource(isScreenInPortrait(context)
                    ? R.drawable.ic_empty_cloud_drive : R.drawable.cloud_empty_landscape);

            emptyTextViewFirst.setText(emptyRootText);
        } else {
            emptyImageView.setImageResource(isScreenInPortrait(context)
                    ? R.drawable.ic_zero_portrait_empty_folder : R.drawable.ic_zero_landscape_empty_folder);

            emptyTextViewFirst.setText(emptyGeneralText);
        }

        ColorUtils.setImageViewAlphaIfDark(context, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA);
    }

    @Override
    public void onAttach(Context mContext) {
        super.onAttach(mContext);
        context = mContext;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        switch (v.getId()) {
            case R.id.fab_select:
            case R.id.action_text: {
                dbH.setLastCloudFolder(Long.toString(parentHandle));

                if (((FileExplorerActivity) context).isMultiselect()) {
                    Timber.d("Send several files to chat");
                    if (adapter.getSelectedItemCount() > 0) {
                        long[] handles = adapter.getSelectedHandles();
                        ((FileExplorerActivity) context).buttonClick(handles);
                    } else {
                        ((FileExplorerActivity) context).showSnackbar(getString(R.string.no_files_selected_warning));
                    }

                } else {
                    ((FileExplorerActivity) context).buttonClick(parentHandle);
                }
                break;

            }
            case R.id.cancel_text: {
                ((FileExplorerActivity) context).finishActivity();
            }
            break;
        }
    }

    public void navigateToFolder(long handle) {
        Timber.d("Handle: %s", handle);

        int lastFirstVisiblePosition;
        if (((FileExplorerActivity) context).isList()) {
            if (mLayoutManager == null) {
                Timber.e("mLayoutManager is null");
                mLayoutManager = new LinearLayoutManager(context);
                recyclerView.setLayoutManager(mLayoutManager);
            }
            lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        } else {
            // For grid view, just add null check
            lastFirstVisiblePosition = gridLayoutManager == null ? 0 : gridLayoutManager.findFirstCompletelyVisibleItemPosition();
        }

        Timber.d("Push to stack %d position", lastFirstVisiblePosition);
        lastPositionStack.push(lastFirstVisiblePosition);

        setParentHandle(handle);
        nodes.clear();
        setNodes(nodes);
        recyclerView.scrollToPosition(0);

        if ((modeCloud == FileExplorerActivity.MOVE) || (modeCloud == FileExplorerActivity.COPY)) {
            activateButton(true);
        }
    }

    public void itemClick(View view, int position) {
        Timber.d("Position: %s", position);

        ArrayList<MegaNode> clickNodes;

        if (searchNodes != null) {
            clickNodes = searchNodes;
            shouldResetNodes = false;
            ((FileExplorerActivity) context).setQueryAfterSearch();
            ((FileExplorerActivity) context).collapseSearchView();
        } else {
            clickNodes = nodes;
        }

        if (position < 0 || position >= clickNodes.size()) return;

        MegaNode n = clickNodes.get(position);

        if (n.isFolder()) {
            searchNodes = null;
            ((FileExplorerActivity) context).setShouldRestartSearch(false);

            if (selectFile && ((FileExplorerActivity) context).isMultiselect() && adapter.isMultipleSelect()) {
                hideMultipleSelect();
            }

            ((FileExplorerActivity) context).hideTabs(true, CLOUD_FRAGMENT);

            int lastFirstVisiblePosition;
            if (((FileExplorerActivity) context).isList()) {
                if (mLayoutManager == null) {
                    Timber.e("mLayoutManager is null");
                    mLayoutManager = new LinearLayoutManager(context);
                    recyclerView.setLayoutManager(mLayoutManager);
                }
                lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
            } else {
                // For grid view, just add null check
                lastFirstVisiblePosition = gridLayoutManager == null ? 0 : gridLayoutManager.findFirstCompletelyVisibleItemPosition();
            }

            Timber.d("Push to stack %d position", lastFirstVisiblePosition);
            lastPositionStack.push(lastFirstVisiblePosition);

            if (modeCloud == FileExplorerActivity.SELECT) {
                activateButton(!selectFile);
            }

            setParentHandle(n.getHandle());
            setNodes(megaApi.getChildren(n, order));

            recyclerView.scrollToPosition(0);

            if (adapter.getItemCount() == 0 && (modeCloud == FileExplorerActivity.MOVE || modeCloud == FileExplorerActivity.COPY)) {
                activateButton(true);
            } else if (modeCloud == FileExplorerActivity.MOVE || modeCloud == FileExplorerActivity.COPY) {
                MegaNode parent = ((FileExplorerActivity) context).parentMoveCopy();
                activateButton(parent == null || parent.getHandle() != parentHandle);
            }

        } else if (selectFile) {
            if (((FileExplorerActivity) context).isMultiselect()) {
                if (adapter.getSelectedItemCount() == 0) {
                    activateActionMode();
                    adapter.toggleSelection(position);
                    updateActionModeTitle();
                } else {
                    adapter.toggleSelection(position);

                    List<MegaNode> selectedNodes = adapter.getSelectedNodes();
                    if (selectedNodes.size() > 0) {
                        updateActionModeTitle();
                    }
                }
            } else {
                //Send file
                ((FileExplorerActivity) context).buttonClick(n.getHandle());
            }
        }

        shouldResetNodes = true;
    }

    private boolean shouldShowOptionsBar(MegaNode parentNode) {
        if (selectFile) {
            return false;
        }

        MegaNode rootNode = megaApi.getRootNode();
        return rootNode != null && parentNode != null && parentNode.getHandle() != rootNode.getHandle();
    }

    public int onBackPressed() {
        Timber.d("onBackPressed");
        if (selectFile) {
            if (((FileExplorerActivity) context).isMultiselect()) {
                if (adapter.isMultipleSelect()) {
                    hideMultipleSelect();
                }
            }
        }

        MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));

        if (parentNode != null) {
            if (modeCloud == FileExplorerActivity.SELECT) {
                activateButton(shouldShowOptionsBar(parentNode));
            }

            setParentHandle(parentNode.getHandle());

            if (parentNode.getType() == MegaNode.TYPE_ROOT) {
                ((FileExplorerActivity) context).hideTabs(false, CLOUD_FRAGMENT);
            }

            ((FileExplorerActivity) context).changeTitle();

            if ((modeCloud == FileExplorerActivity.MOVE) || (modeCloud == FileExplorerActivity.COPY)) {
                MegaNode parent = ((FileExplorerActivity) context).parentMoveCopy();
                if (parent != null) {
                    activateButton(parent.getHandle() != parentNode.getHandle());
                } else {
                    activateButton(true);

                }
            }

            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);

            setNodes(megaApi.getChildren(parentNode, order));
            int lastVisiblePosition = 0;
            if (!lastPositionStack.empty()) {
                lastVisiblePosition = lastPositionStack.pop();
                Timber.d("Pop of the stack %d position", lastVisiblePosition);
            }
            Timber.d("Scroll to %d position", lastVisiblePosition);

            if (lastVisiblePosition >= 0) {
                if (((FileExplorerActivity) context).isList()) {
                    mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
                } else {
                    gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
                }
            }

            return 2;
        } else {
            return 0;
        }
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public void setParentHandle(long parentHandle) {
        Timber.d("Parent handle: %s", parentHandle);
        this.parentHandle = parentHandle;
        if (adapter != null) {
            adapter.setParentHandle(parentHandle);
        }
        ((FileExplorerActivity) context).setParentHandleCloud(parentHandle);
        ((FileExplorerActivity) context).changeTitle();
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = nodes;
        if (adapter != null) {
            adapter.setNodes(nodes);
            showEmptyScreen();
        }
    }

    private void selectAll() {
        Timber.d("selectAll");

        if (adapter != null) {
            adapter.selectAll();

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
        if (modeCloud == FileExplorerActivity.SELECT) {
            activateButton(false);
        }
    }

    @Override
    protected void updateActionModeTitle() {
        if (actionMode == null || getActivity() == null) {
            return;
        }

        List<MegaNode> documents = adapter.getSelectedNodes();

        if (documents == null) return;

        int files = 0;
        int folders = 0;
        for (MegaNode document : documents) {
            if (document.isFile()) {
                files++;
            } else if (document.isFolder()) {
                folders++;
            }
        }

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
            e.printStackTrace();
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        Timber.d("hideMultipleSelect");
        adapter.setMultipleSelect(false);
        adapter.clearSelections();
        if (actionMode != null) {
            actionMode.finish();
        }

        if (isMultiselect()) {
            activateButton(false);
        }

    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    private void activateButton(boolean show) {
        if (modeCloud == FileExplorerActivity.SELECT) {
            int visibility = show ? View.VISIBLE : View.GONE;

            if (selectFile) {
                fabSelect.setVisibility(visibility);
            } else {
                optionsBar.setVisibility(visibility);
            }
        } else {
            optionButton.setEnabled(show);
        }
    }

    public void orderNodes(int order) {
        this.order = order;
        if (parentHandle == -1) {
            nodes = megaApi.getChildren(megaApi.getRootNode(), order);
        } else {
            nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), order);
        }

        setNodes(nodes);
    }

    private boolean isMultiselect() {
        return modeCloud == FileExplorerActivity.SELECT && selectFile && ((FileExplorerActivity) context).isMultiselect();
    }

    public void search(String s) {
        if (megaApi == null || s == null || !shouldResetNodes) {
            return;
        }
        if (getParentHandle() == -1) {
            setParentHandle(megaApi.getRootNode().getHandle());
        }
        MegaNode parent = megaApi.getNodeByHandle(getParentHandle());

        if (parent == null) {
            Timber.w("Parent null when search");
            return;
        }

        searchCancelToken = initNewSearch();
        searchNodesUseCase.get(s, INVALID_HANDLE, getParentHandle(), TYPE_CLOUD_EXPLORER, searchCancelToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((searchedNodes, throwable) -> {
                    if (throwable == null) {
                        finishSearch(searchedNodes);
                    }
                });
    }

    @NonNull
    @Override
    public MegaCancelToken initNewSearch() {
        updateSearchProgressView(true);
        cancelSearch();
        return Objects.requireNonNull(MegaCancelToken.createInstance());
    }

    @Override
    public void updateSearchProgressView(boolean inProgress) {
        if (contentLayout == null || searchProgressBar == null || recyclerView == null) {
            Timber.w("Cannot set search progress view, one or more parameters are NULL.");
            return;
        }

        contentLayout.setEnabled(!inProgress);
        contentLayout.setAlpha(inProgress ? 0.4f : 1f);
        searchProgressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(inProgress ? View.GONE : View.VISIBLE);
    }

    @Override
    public void cancelSearch() {
        if (searchCancelToken != null) {
            searchCancelToken.cancel();
        }
    }

    @Override
    public void finishSearch(@NonNull ArrayList<MegaNode> searchedNodes) {
        updateSearchProgressView(false);
        setSearchNodes(searchedNodes);
    }

    public void setSearchNodes(ArrayList<MegaNode> nodes) {
        if (adapter == null) return;
        searchNodes = nodes;
        ((FileExplorerActivity) context).setShouldRestartSearch(true);
        adapter.setNodes(searchNodes);
        showEmptyScreen();

        if (isWaitingForSearchedNodes()) {
            reDoTheSelectionAfterRotation();
        }
    }

    public void closeSearch(boolean collapsedByClick) {
        updateSearchProgressView(false);
        cancelSearch();
        if (!collapsedByClick) {
            searchNodes = null;
        }
        if (shouldResetNodes) {
            getNodes();
            setNodes(nodes);
        }
    }

    public FastScroller getFastScroller() {
        return fastScroller;
    }

    public boolean isFolderEmpty() {
        return adapter == null || adapter.getItemCount() <= 0;
    }
}
