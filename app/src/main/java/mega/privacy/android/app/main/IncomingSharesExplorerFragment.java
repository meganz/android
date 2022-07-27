package mega.privacy.android.app.main;

import static mega.privacy.android.app.main.FileExplorerActivity.COPY;
import static mega.privacy.android.app.main.FileExplorerActivity.INCOMING_FRAGMENT;
import static mega.privacy.android.app.main.FileExplorerActivity.MOVE;
import static mega.privacy.android.app.search.usecase.SearchNodesUseCase.TYPE_INCOMING_EXPLORER;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText;
import static mega.privacy.android.app.utils.Util.getPreferences;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
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
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

@AndroidEntryPoint
public class IncomingSharesExplorerFragment extends RotatableFragment
        implements OnClickListener, CheckScrollInterface, SearchCallback.View, SearchCallback.Data {

    @Inject
    SortOrderManagement sortOrderManagement;
    @Inject
    SearchNodesUseCase searchNodesUseCase;

    private DisplayMetrics outMetrics;
    private Context context;
    private MegaApiAndroid megaApi;
    private ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
    private ArrayList<MegaNode> searchNodes = null;

    private long parentHandle = -1;

    private MegaExplorerAdapter adapter;
    private FastScroller fastScroller;

    private int modeCloud;
    private boolean selectFile;

    private RelativeLayout contentLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private CustomizedGridLayoutManager gridLayoutManager;

    private ImageView emptyImageView;
    private LinearLayout emptyTextView;
    private TextView emptyTextViewFirst;

    private Button optionButton;
    private Button cancelButton;
    private LinearLayout optionsBar;
    private FloatingActionButton fabSelect;

    private Stack<Integer> lastPositionStack;

    private Handler handler;
    private ActionMode actionMode;

    private int orderParent = megaApi.ORDER_DEFAULT_ASC;
    private int order = megaApi.ORDER_DEFAULT_ASC;

    private MegaCancelToken searchCancelToken;
    private ProgressBar searchProgressBar;
    private boolean shouldResetNodes = true;
    private boolean hasWritePermissions = true;

    private Spanned emptyRootText;
    private Spanned emptyGeneralText;

    @Override
    protected RotatableAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void activateActionMode() {
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
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_explorer_multiaction, menu);
            ((FileExplorerActivity) context).hideTabs(true, INCOMING_FRAGMENT);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            if (!((FileExplorerActivity) context).shouldReopenSearch()) {
                ((FileExplorerActivity) context).hideTabs(false, INCOMING_FRAGMENT);
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
            List<MegaNode> selected = adapter.getSelectedNodes();

            if (selected.size() != 0) {
                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                MegaNode node = megaApi.getNodeByHandle(parentHandle);

                if (selected.size() == megaApi.getNumChildFiles(node)) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);

                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                }

                unselect.setTitle(getString(R.string.action_unselect_all));
                unselect.setVisible(true);
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            return false;
        }
    }

    public static IncomingSharesExplorerFragment newInstance() {
        Timber.d("newInstance");
        IncomingSharesExplorerFragment fragment = new IncomingSharesExplorerFragment();
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
        if (recyclerView == null) return;

        ((FileExplorerActivity) context).changeActionBarElevation(
                recyclerView.canScrollVertically(SCROLLING_UP_DIRECTION)
                        || (adapter != null && adapter.isMultipleSelect()),
                FileExplorerActivity.INCOMING_FRAGMENT);
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

        Display display = getActivity().getWindowManager().getDefaultDisplay();

        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);

        contentLayout = v.findViewById(R.id.content_layout);
        searchProgressBar = v.findViewById(R.id.progressbar);

        optionsBar = v.findViewById(R.id.options_explorer_layout);

        optionButton = v.findViewById(R.id.action_text);
        optionButton.setOnClickListener(this);

        cancelButton = v.findViewById(R.id.cancel_text);
        cancelButton.setOnClickListener(this);
        cancelButton.setText(getString(R.string.general_cancel));

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
            recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
            v.findViewById(R.id.file_list_view_browser).setVisibility(View.GONE);
            gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        emptyImageView = v.findViewById(R.id.file_list_empty_image);
        emptyTextView = v.findViewById(R.id.file_list_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);
        parentHandle = ((FileExplorerActivity) context).getParentHandleIncoming();

        if (parentHandle != INVALID_HANDLE) {
            ((FileExplorerActivity) context).hideTabs(true, INCOMING_FRAGMENT);
        }

        modeCloud = ((FileExplorerActivity) context).getMode();
        selectFile = ((FileExplorerActivity) context).isSelectFile();

        MegaPreferences prefs = getPreferences(context);

        if (prefs != null) {
            if (prefs.getPreferredSortOthers() != null) {
                orderParent = Integer.parseInt(prefs.getPreferredSortOthers());
            }
            if (prefs.getPreferredSortCloud() != null) {
                order = Integer.parseInt(prefs.getPreferredSortCloud());
            }
        }

        getNodes();

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


        if (modeCloud == MOVE || modeCloud == COPY) {
            optionButton.setText(StringResourcesUtils.getString(modeCloud == MOVE
                    ? R.string.context_move
                    : R.string.context_copy));

            if (((FileExplorerActivity) context).getDeepBrowserTree() > 0) {
                checkCopyMoveButton();
            }
        } else if (modeCloud == FileExplorerActivity.UPLOAD) {
            optionButton.setText(getString(R.string.context_upload));
        } else if (modeCloud == FileExplorerActivity.IMPORT) {
            optionButton.setText(getString(R.string.add_to_cloud));
        } else if (isMultiselect()) {
            optionButton.setText(getString(R.string.context_send));
            if (adapter != null && adapter.getSelectedItemCount() > 0) {
                activateButton(true);
            } else {
                activateButton(false);
            }
        } else if (modeCloud == FileExplorerActivity.SELECT) {
            optionsBar.setVisibility(View.GONE);
        } else if (modeCloud == FileExplorerActivity.SELECT_CAMERA_FOLDER) {
            optionButton.setText(getString(R.string.general_select));
        } else {
            optionButton.setText(getString(R.string.general_select));
        }

        Timber.d("deepBrowserTree value: %s", ((FileExplorerActivity) context).getDeepBrowserTree());
        setOptionsBarVisibility();

        if (((FileExplorerActivity) context).shouldRestartSearch()) {
            setWaitingForSearchedNodes(true);
            search(((FileExplorerActivity) context).getQuerySearch());
        }
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emptyRootText = formatEmptyScreenText(requireContext(),
                        StringResourcesUtils.getString(R.string.context_empty_incoming));

        emptyGeneralText = formatEmptyScreenText(requireContext(),
                        StringResourcesUtils.getString(R.string.file_browser_empty_folder_new));

        updateEmptyScreen();
        super.onViewCreated(view, savedInstanceState);
    }

    private void setOptionsBarVisibility() {
        if (optionsBar == null) {
            return;
        }

        if (modeCloud == FileExplorerActivity.SELECT ||
                (!isMultiselect() && (((FileExplorerActivity) context).getDeepBrowserTree() <= 0 || selectFile))) {
            optionsBar.setVisibility(View.GONE);
        } else {
            optionsBar.setVisibility(View.VISIBLE);
        }
    }

    private void getNodes() {
        if (parentHandle == -1) {
            findNodes();
        } else {
            MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
            nodes = megaApi.getChildren(parentNode, order);
        }
    }

    private void showEmptyScreen() {
        if (adapter == null) {
            return;
        }

        if (adapter.getItemCount() != 0) {
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            updateEmptyScreen();
        }
    }

    private void updateEmptyScreen() {
        if (parentHandle == INVALID_HANDLE) {
            emptyImageView.setImageResource(isScreenInPortrait(context)
                    ? R.drawable.incoming_shares_empty : R.drawable.incoming_empty_landscape);

            emptyTextViewFirst.setText(emptyRootText);
        } else {
            emptyImageView.setImageResource(isScreenInPortrait(context)
                    ? R.drawable.ic_zero_portrait_empty_folder : R.drawable.ic_zero_landscape_empty_folder);

            emptyTextViewFirst.setText(emptyGeneralText);
        }

        ColorUtils.setImageViewAlphaIfDark(context, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA);
    }

    private void findNodes() {
        Timber.d("findNodes");
        ((FileExplorerActivity) context).setDeepBrowserTree(0);

        setOptionsBarVisibility();
        nodes = megaApi.getInShares();

        if (orderParent == MegaApiJava.ORDER_DEFAULT_DESC) {
            sortByMailDescending(nodes);
        }
    }


    private void sortByMailDescending(ArrayList<MegaNode> nodes) {
        Timber.d("sortByNameDescending");
        ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
        ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) == null) {
                continue;
            }
            if (nodes.get(i).isFolder()) {
                folderNodes.add(nodes.get(i));
            } else {
                fileNodes.add(nodes.get(i));
            }
        }

        Collections.reverse(folderNodes);
        Collections.reverse(fileNodes);

        nodes.clear();
        nodes.addAll(folderNodes);
        nodes.addAll(fileNodes);
    }

    private void checkWritePermissions() {
        MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

        if (parentNode == null) {
            hasWritePermissions = false;
        } else {
            int accessLevel = megaApi.getAccess(parentNode);
            hasWritePermissions = accessLevel >= MegaShare.ACCESS_READWRITE;
        }

        activateButton(hasWritePermissions);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.fab_select:
            case R.id.action_text: {
                if (((FileExplorerActivity) context).isMultiselect()) {
                    if (adapter.getSelectedItemCount() > 0) {
                        long handles[] = adapter.getSelectedHandles();
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
                break;
            }
        }
    }

    public void navigateToFolder(long handle) {
        Timber.d("navigateToFolder");

        ((FileExplorerActivity) context).increaseDeepBrowserTree();
        Timber.d("((FileExplorerActivity)context).deepBrowserTree value: %s", ((FileExplorerActivity) context).getDeepBrowserTree());
        setOptionsBarVisibility();

        int lastFirstVisiblePosition = 0;
        if (((FileExplorerActivity) context).isList()) {
            lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        } else {
            lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
        }

        Timber.d("Push to stack %d position", lastFirstVisiblePosition);
        lastPositionStack.push(lastFirstVisiblePosition);

        setParentHandle(handle);
        nodes.clear();
        setNodes(nodes);

        recyclerView.scrollToPosition(0);

        if (modeCloud == COPY || modeCloud == MOVE) {
            activateButton(true);
        }
    }

    public void itemClick(View view, int position) {
        ArrayList<MegaNode> clickNodes;

        if (searchNodes != null) {
            clickNodes = searchNodes;
            shouldResetNodes = false;
            ((FileExplorerActivity) context).setQueryAfterSearch();
            ((FileExplorerActivity) context).collapseSearchView();
        } else {
            clickNodes = nodes;
        }

        MegaNode n = clickNodes.get(position);

        if (n.isFolder()) {
            searchNodes = null;
            ((FileExplorerActivity) context).hideTabs(true, INCOMING_FRAGMENT);
            ((FileExplorerActivity) context).setShouldRestartSearch(false);

            if (selectFile && ((FileExplorerActivity) context).isMultiselect() && adapter.isMultipleSelect()) {
                hideMultipleSelect();
            }
            ((FileExplorerActivity) context).increaseDeepBrowserTree();
            Timber.d("deepBrowserTree value: %s", ((FileExplorerActivity) context).getDeepBrowserTree());
            setOptionsBarVisibility();

            int lastFirstVisiblePosition = 0;
            if (((FileExplorerActivity) context).isList()) {
                lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
            } else {
                lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
            }

            Timber.d("Push to stack %d position", lastFirstVisiblePosition);
            lastPositionStack.push(lastFirstVisiblePosition);

            setParentHandle(n.getHandle());
            ((FileExplorerActivity) context).supportInvalidateOptionsMenu();

            setNodes(megaApi.getChildren(nodes.get(position), order));
            recyclerView.scrollToPosition(0);

            if (modeCloud == COPY || modeCloud == MOVE) {
                if (adapter.getItemCount() == 0) {
                    activateButton(true);
                } else if (((FileExplorerActivity) context).getDeepBrowserTree() > 0) {
                    checkCopyMoveButton();
                }
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
                ((FileExplorerActivity) context).buttonClick(n.getHandle());

            }
        }
        ((FileExplorerActivity) context).supportInvalidateOptionsMenu();
        shouldResetNodes = true;
    }

    public int onBackPressed() {
        Timber.d("deepBrowserTree %s", ((FileExplorerActivity) context).getDeepBrowserTree());
        ((FileExplorerActivity) context).decreaseDeepBrowserTree();

        if (((FileExplorerActivity) context).getDeepBrowserTree() == 0) {
            setParentHandle(INVALID_HANDLE);
            ((FileExplorerActivity) context).hideTabs(false, INCOMING_FRAGMENT);
            findNodes();

            setNodes(nodes);

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
            setOptionsBarVisibility();
            ((FileExplorerActivity) context).supportInvalidateOptionsMenu();
            return 3;
        } else if (((FileExplorerActivity) context).getDeepBrowserTree() > 0) {
            parentHandle = adapter.getParentHandle();

            MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));

            if (parentNode != null) {

                setParentHandle(parentNode.getHandle());
                nodes = megaApi.getChildren(parentNode, order);
                setNodes(nodes);

                if (modeCloud == COPY || modeCloud == MOVE) {
                    checkCopyMoveButton();
                }

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

                ((FileExplorerActivity) context).supportInvalidateOptionsMenu();
                return 2;
            }

            setOptionsBarVisibility();

            return 2;
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
            optionsBar.setVisibility(View.GONE);
            activateButton(false);
            ((FileExplorerActivity) context).setDeepBrowserTree(0);
            ((FileExplorerActivity) context).supportInvalidateOptionsMenu();
            return 0;
        }
    }

    public long getParentHandle() {
        return parentHandle;
    }

    private void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
        if (adapter != null) {
            adapter.setParentHandle(parentHandle);
        }
        ((FileExplorerActivity) context).setParentHandleIncoming(parentHandle);
        ((FileExplorerActivity) context).changeTitle();
    }

    private void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = nodes;
        if (adapter != null) {
            adapter.setNodes(nodes);
            showEmptyScreen();
        }

        checkWritePermissions();
    }

    private RecyclerView getRecyclerView() {
        return recyclerView;
    }

    private void activateButton(boolean show) {
        if (modeCloud == FileExplorerActivity.SELECT) {
            fabSelect.setVisibility(selectFile && show ? View.VISIBLE : View.GONE);
        } else {
            boolean shouldShowButton = hasWritePermissions && show;
            optionButton.setEnabled(shouldShowButton);
        }
    }

    private void selectAll() {
        if (adapter != null) {
            adapter.selectAll();

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

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
        actionMode.invalidate();
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        adapter.setMultipleSelect(false);
        adapter.clearSelections();
        if (actionMode != null) {
            actionMode.finish();
        }

        if (isMultiselect()) {
            activateButton(false);
        }

    }

    public void orderNodes(int order) {
        if (parentHandle == -1) {
            this.orderParent = order;
            findNodes();
        } else {
            this.order = order;
            nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), order);
        }

        setNodes(nodes);
    }

    boolean isMultiselect() {
        return modeCloud == FileExplorerActivity.SELECT && selectFile && ((FileExplorerActivity) context).isMultiselect();
    }

    public void search(String s) {
        if (megaApi == null || s == null || !shouldResetNodes) {
            return;
        }

        searchCancelToken = initNewSearch();
        searchNodesUseCase.get(s, INVALID_HANDLE, getParentHandle(), TYPE_INCOMING_EXPLORER, searchCancelToken)
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

    /**
     * Checks if copy or move button should be shown or hidden depending on the current navigation level.
     * Shows it if the current navigation level is not the parent of moving/copying nodes.
     * Hides it otherwise.
     */
    private void checkCopyMoveButton() {
        MegaNode parentMove = ((FileExplorerActivity) context).parentMoveCopy();
        activateButton(parentMove == null || parentMove.getHandle() != parentHandle);
    }
}
