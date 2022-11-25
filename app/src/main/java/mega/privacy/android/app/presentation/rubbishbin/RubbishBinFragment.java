package mega.privacy.android.app.presentation.rubbishbin;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.utils.Constants.BUFFER_COMP;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB;
import static mega.privacy.android.app.utils.Constants.ORDER_CLOUD;
import static mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_RUBBISH_BIN;
import static mega.privacy.android.app.utils.FileUtil.getDownloadLocation;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.PdfViewerActivity;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.presentation.manager.ManagerViewModel;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.mapper.SortOrderIntMapperKt;
import mega.privacy.android.data.model.MegaPreferences;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

@AndroidEntryPoint
public class RubbishBinFragment extends Fragment {

    @Inject
    DatabaseHandler dbH;

    private ManagerViewModel managerViewModel;

    Context context;
    RecyclerView recyclerView;
    LinearLayoutManager mLayoutManager;
    CustomizedGridLayoutManager gridLayoutManager;
    MegaNodeAdapter adapter;

    List<MegaNode> nodes;

    ImageView emptyImageView;
    LinearLayout emptyTextView;
    TextView emptyTextViewFirst;

    MegaApiAndroid megaApi;

    public ActionMode actionMode;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    Stack<Integer> lastPositionStack;


    MegaPreferences prefs;
    String downloadLocationDefaultPath;

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
        }
    }

    public void checkScroll() {
        if (recyclerView != null) {
            if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
                ((ManagerActivity) context).changeAppBarElevation(true);
            } else {
                ((ManagerActivity) context).changeAppBarElevation(false);
            }
        }
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<MegaNode> documents = adapter.getSelectedNodes();

            switch (item.getItemId()) {
                case R.id.cab_menu_restore_from_rubbish:

                    ((ManagerActivity) context).restoreFromRubbish(documents);
                    clearSelections();
                    hideMultipleSelect();
                    break;
                case R.id.cab_menu_delete:
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i = 0; i < documents.size(); i++) {
                        handleList.add(documents.get(i).getHandle());
                    }

                    ((ManagerActivity) context).askConfirmationMoveToRubbish(handleList);
                    break;
                case R.id.cab_menu_select_all:
                    selectAll();
                    break;
                case R.id.cab_menu_clear_selection:
                    clearSelections();
                    hideMultipleSelect();
                    break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.rubbish_bin_action, menu);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            clearSelections();
            adapter.setMultipleSelect(false);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.cab_menu_select_all)
                    .setVisible(adapter.getSelectedItemCount()
                            < adapter.getItemCount() - adapter.getPlaceholderCount());

            boolean isRestoreVisible = true;
            List<MegaNode> documents = adapter.getSelectedNodes();
            for (MegaNode node : documents) {
                long restoreHandle = node.getRestoreHandle();
                if (restoreHandle == INVALID_HANDLE) {
                    isRestoreVisible = false;
                    break;
                }

                MegaNode restoreNode = megaApi.getNodeByHandle(restoreHandle);
                if (restoreNode == null || megaApi.isInRubbish(restoreNode) || megaApi.isInInbox(restoreNode)) {
                    isRestoreVisible = false;
                    break;
                }
            }

            menu.findItem(R.id.cab_menu_restore_from_rubbish).setVisible(isRestoreVisible);

            return true;
        }
    }

    public boolean showSelectMenuItem() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void selectAll() {
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
            }

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    /**
     * Shows the Sort by panel.
     *
     * @param unit Unit event.
     * @return Null.
     */
    private Unit showSortByPanel(Unit unit) {
        ((ManagerActivity) context).showNewSortByPanel(ORDER_CLOUD);
        return null;
    }

    public static RubbishBinFragment newInstance() {
        Timber.d("newInstance");
        RubbishBinFragment fragment = new RubbishBinFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");

        prefs = dbH.getPreferences();

        downloadLocationDefaultPath = getDownloadLocation();

        lastPositionStack = new Stack<>();

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        if (megaApi.getRootNode() == null) {
            return null;
        }

        SortByHeaderViewModel sortByHeaderViewModel = new ViewModelProvider(this)
                .get(SortByHeaderViewModel.class);

        sortByHeaderViewModel.getShowDialogEvent().observe(getViewLifecycleOwner(),
                new EventObserver<>(this::showSortByPanel));

        managerViewModel = new ViewModelProvider(requireActivity()).get(ManagerViewModel.class);
        managerViewModel.getUpdateRubbishBinNodes().observe(getViewLifecycleOwner(),
                new EventObserver<>(nodes -> {
                    hideMultipleSelect();
                    setNodes(new ArrayList(nodes));
                    getRecyclerView().invalidate();
                    return null;
                })
        );

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        if (managerViewModel.getState().getValue().getRubbishBinParentHandle() == -1L || managerViewModel.getState().getValue().getRubbishBinParentHandle() == megaApi.getRubbishNode().getHandle()) {
            Timber.d("Parent is the Rubbish: %s", managerViewModel.getState().getValue().getRubbishBinParentHandle());

            nodes = megaApi.getChildren(megaApi.getRubbishNode(), SortOrderIntMapperKt.sortOrderToInt(managerViewModel.getOrder()));

        } else {
            MegaNode parentNode = megaApi.getNodeByHandle(managerViewModel.getState().getValue().getRubbishBinParentHandle());

            if (parentNode != null) {
                Timber.d("The parent node is: %s", parentNode.getHandle());
                nodes = megaApi.getChildren(parentNode, SortOrderIntMapperKt.sortOrderToInt(managerViewModel.getOrder()));

                ((ManagerActivity) context).supportInvalidateOptionsMenu();
            }
            nodes = megaApi.getChildren(parentNode, SortOrderIntMapperKt.sortOrderToInt(managerViewModel.getOrder()));
        }

        ((ManagerActivity) context).setToolbarTitle();
        ((ManagerActivity) context).supportInvalidateOptionsMenu();

        if (((ManagerActivity) context).isList) {
            Timber.d("List View");
            View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);

            recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_list_view);

            mLayoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mLayoutManager);
            //Add bottom padding for recyclerView like in other fragments.
            recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
            recyclerView.setClipToPadding(false);
            recyclerView.setItemAnimator(noChangeRecyclerViewItemAnimator());
            recyclerView.addItemDecoration(new PositionDividerItemDecoration(requireContext(), getResources().getDisplayMetrics()));
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });

            emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_list_empty_image);
            emptyTextView = (LinearLayout) v.findViewById(R.id.rubbishbin_list_empty_text);
            emptyTextViewFirst = (TextView) v.findViewById(R.id.rubbishbin_list_empty_text_first);

            if (adapter == null) {
                adapter = new MegaNodeAdapter(context, this, nodes,
                        managerViewModel.getState().getValue().getRubbishBinParentHandle(), recyclerView,
                        RUBBISH_BIN_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST, sortByHeaderViewModel);
            } else {
                adapter.setParentHandle(managerViewModel.getState().getValue().getRubbishBinParentHandle());
                adapter.setListFragment(recyclerView);
                adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
            }

            adapter.setMultipleSelect(false);

            recyclerView.setAdapter(adapter);

            setNodes(nodes);

            if (adapter.getItemCount() == 0) {

                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                if (megaApi.getRubbishNode().getHandle() == managerViewModel.getState().getValue().getRubbishBinParentHandle() || managerViewModel.getState().getValue().getRubbishBinParentHandle() == -1) {
                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_landscape);
                    } else {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_portrait);
                    }
                    String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
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
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    emptyTextViewFirst.setText(result);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }

            return v;
        } else {
            Timber.d("Grid View");
            View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);

            recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_grid_view);
            recyclerView.setHasFixedSize(true);
            gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });

            emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_grid_empty_image);
            emptyTextView = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_empty_text);
            emptyTextViewFirst = (TextView) v.findViewById(R.id.rubbishbin_grid_empty_text_first);

            if (adapter == null) {
                adapter = new MegaNodeAdapter(context, this, nodes,
                        managerViewModel.getState().getValue().getRubbishBinParentHandle(), recyclerView,
                        RUBBISH_BIN_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID, sortByHeaderViewModel);
            } else {
                adapter.setParentHandle(managerViewModel.getState().getValue().getRubbishBinParentHandle());
                adapter.setListFragment(recyclerView);
                adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
            }

            gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(gridLayoutManager.getSpanCount()));

            adapter.setMultipleSelect(false);

            recyclerView.setAdapter(adapter);

            setNodes(nodes);

            if (adapter.getItemCount() == 0) {

                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                if (megaApi.getRubbishNode().getHandle() == managerViewModel.getState().getValue().getRubbishBinParentHandle() || managerViewModel.getState().getValue().getRubbishBinParentHandle() == -1) {
                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_landscape);
                    } else {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_portrait);
                    }
                    String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
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
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    emptyTextViewFirst.setText(result);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
            return v;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        observeDragSupportEvents(getViewLifecycleOwner(), recyclerView, VIEWER_FROM_RUBBISH_BIN);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        if (adapter.isMultipleSelect()) {
            Timber.d("Multiselect ON");
            adapter.toggleSelection(position);

            List<MegaNode> selectedNodes = adapter.getSelectedNodes();
            if (selectedNodes.size() > 0) {
                updateActionModeTitle();

            }
        } else {
            if (nodes.get(position).isFolder()) {
                MegaNode n = nodes.get(position);

                int lastFirstVisiblePosition = 0;
                if (((ManagerActivity) context).isList) {
                    lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                } else {
                    lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
                    if (lastFirstVisiblePosition == -1) {
                        Timber.w("Completely -1 then find just visible position");
                        lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
                    }
                }
                Timber.d("Push to stack %d position", lastFirstVisiblePosition);
                lastPositionStack.push(lastFirstVisiblePosition);

                managerViewModel.setRubbishBinParentHandle(n.getHandle());

                ((ManagerActivity) context).setToolbarTitle();
                ((ManagerActivity) context).supportInvalidateOptionsMenu();

                adapter.setParentHandle(managerViewModel.getState().getValue().getRubbishBinParentHandle());
                nodes = megaApi.getChildren(nodes.get(position), SortOrderIntMapperKt.sortOrderToInt(managerViewModel.getOrder()));
                adapter.setNodes(nodes);
                recyclerView.scrollToPosition(0);

                //If folder has no files
                if (adapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);

                    if (megaApi.getRubbishNode().getHandle() == managerViewModel.getState().getValue().getRubbishBinParentHandle() || managerViewModel.getState().getValue().getRubbishBinParentHandle() == -1) {
                        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_landscape);
                        } else {
                            emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_portrait);
                        }

                        String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'"
                                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                    + "\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'"
                                    + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                    + "\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
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
                            textToShow = textToShow.replace("[A]", "<font color=\'"
                                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                    + "\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'"
                                    + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                    + "\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        } catch (Exception e) {
                        }
                        Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                        emptyTextViewFirst.setText(result);
                    }
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
                checkScroll();
            } else {
                //Is FILE
                if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()) {
                    Intent intent = ImageViewerActivity.getIntentForParentNode(
                            requireContext(),
                            megaApi.getParentNode(nodes.get(position)).getHandle(),
                            managerViewModel.getOrder(),
                            nodes.get(position).getHandle()
                    );
                    putThumbnailLocation(intent, recyclerView, position, VIEWER_FROM_RUBBISH_BIN, adapter);
                    startActivity(intent);
                    ((ManagerActivity) context).overridePendingTransition(0, 0);
                } else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio()) {
                    MegaNode file = nodes.get(position);

                    String mimeType = MimeTypeList.typeForName(file.getName()).getType();
                    Timber.d("FILE HANDLE: %d, TYPE: %s", file.getHandle(), mimeType);

                    Intent mediaIntent;
                    boolean internalIntent;
                    boolean opusFile = false;
                    if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()) {
                        mediaIntent = new Intent(Intent.ACTION_VIEW);
                        internalIntent = false;
                        String[] s = file.getName().split("\\.");
                        if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
                            opusFile = true;
                        }
                    } else {
                        internalIntent = true;
                        mediaIntent = getMediaIntent(context, nodes.get(position).getName());
                    }
                    mediaIntent.putExtra("placeholder", adapter.getPlaceholderCount());
                    putThumbnailLocation(mediaIntent, recyclerView, position, VIEWER_FROM_RUBBISH_BIN, adapter);
                    mediaIntent.putExtra("FILENAME", file.getName());
                    mediaIntent.putExtra("adapterType", RUBBISH_BIN_ADAPTER);

                    if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH) {
                        mediaIntent.putExtra("parentNodeHandle", -1L);
                    } else {
                        mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
                    }

                    String localPath = getLocalFile(file);

                    if (localPath != null) {
                        File mediaFile = new File(localPath);
                        if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                        } else {
                            mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                        }
                        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart();
                            mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if (mi.totalMem > BUFFER_COMP) {
                            Timber.d("Total mem: %d allocate 32 MB", mi.totalMem);
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        } else {
                            Timber.d("Total mem: %d allocate 16 MB", mi.totalMem);
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                        }

                        String url = megaApi.httpServerGetLocalLink(file);
                        mediaIntent.setDataAndType(Uri.parse(url), mimeType);
                    }
                    mediaIntent.putExtra("HANDLE", file.getHandle());
                    if (opusFile) {
                        mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                    }
                    if (internalIntent) {
                        context.startActivity(mediaIntent);
                    } else {
                        if (isIntentAvailable(context, mediaIntent)) {
                            context.startActivity(mediaIntent);
                        } else {
                            ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getString(R.string.intent_not_available), -1);
                            adapter.notifyDataSetChanged();
                            ((ManagerActivity) context).saveNodesToDevice(
                                    Collections.singletonList(nodes.get(position)),
                                    true, false, false, false);
                        }
                    }
                    ((ManagerActivity) context).overridePendingTransition(0, 0);
                } else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()) {
                    MegaNode file = nodes.get(position);

                    String mimeType = MimeTypeList.typeForName(file.getName()).getType();
                    Timber.d("FILE HANDLE: %d, TYPE: %s", file.getHandle(), mimeType);

                    Intent pdfIntent = new Intent(context, PdfViewerActivity.class);
                    pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    pdfIntent.putExtra("adapterType", RUBBISH_BIN_ADAPTER);
                    pdfIntent.putExtra("inside", true);
                    pdfIntent.putExtra("APP", true);

                    String localPath = getLocalFile(file);

                    if (localPath != null) {
                        File mediaFile = new File(localPath);
                        if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                        } else {
                            pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart();
                            pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if (mi.totalMem > BUFFER_COMP) {
                            Timber.d("Total mem: %d allocate 32 MB", mi.totalMem);
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        } else {
                            Timber.d("Total mem: %d allocate 16 MB", mi.totalMem);
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                        }

                        String url = megaApi.httpServerGetLocalLink(file);
                        pdfIntent.setDataAndType(Uri.parse(url), mimeType);
                    }
                    pdfIntent.putExtra("HANDLE", file.getHandle());
                    putThumbnailLocation(pdfIntent, recyclerView, position, VIEWER_FROM_RUBBISH_BIN, adapter);
                    if (isIntentAvailable(context, pdfIntent)) {
                        startActivity(pdfIntent);
                    } else {
                        Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

                        ((ManagerActivity) context).saveNodesToDevice(
                                Collections.singletonList(nodes.get(position)),
                                true, false, false, false);
                    }
                    ((ManagerActivity) context).overridePendingTransition(0, 0);
                } else if (MimeTypeList.typeForName(nodes.get(position).getName()).isURL()) {
                    manageURLNode(context, megaApi, nodes.get(position));
                } else if (MimeTypeList.typeForName(nodes.get(position).getName()).isOpenableTextFile(nodes.get(position).getSize())) {
                    manageTextFileIntent(requireContext(), nodes.get(position), RUBBISH_BIN_ADAPTER);
                } else {
                    adapter.notifyDataSetChanged();
                    onNodeTapped(context, nodes.get(position), ((ManagerActivity) context)::saveNodeByTap, (ManagerActivity) context, (ManagerActivity) context);
                }
            }
        }
    }

    private void updateActionModeTitle() {
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
            e.printStackTrace();
            Timber.e(e, "Invalidate error");
        }
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        adapter.setMultipleSelect(false);

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public int onBackPressed() {

        if (adapter == null) {
            return 0;
        }

        if (((ManagerActivity) context).comesFromNotifications && ((ManagerActivity) context).comesFromNotificationHandle == managerViewModel.getState().getValue().getRubbishBinParentHandle()) {
            ((ManagerActivity) context).comesFromNotifications = false;
            ((ManagerActivity) context).comesFromNotificationHandle = -1;
            ((ManagerActivity) context).selectDrawerItem(DrawerItem.NOTIFICATIONS);
            managerViewModel.setRubbishBinParentHandle(((ManagerActivity) context).comesFromNotificationHandleSaved);
            ((ManagerActivity) context).comesFromNotificationHandleSaved = -1;

            return 2;
        } else {
            MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(managerViewModel.getState().getValue().getRubbishBinParentHandle()));
            if (parentNode != null) {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);

                ((ManagerActivity) context).supportInvalidateOptionsMenu();
                managerViewModel.setRubbishBinParentHandle(parentNode.getHandle());

                ((ManagerActivity) context).setToolbarTitle();
                nodes = megaApi.getChildren(parentNode, SortOrderIntMapperKt.sortOrderToInt(managerViewModel.getOrder()));
                adapter.setNodes(nodes);

                int lastVisiblePosition = 0;
                if (!lastPositionStack.empty()) {
                    lastVisiblePosition = lastPositionStack.pop();
                    Timber.d("Pop of the stack %d position", lastVisiblePosition);
                }
                Timber.d("Scroll to %d position", lastVisiblePosition);

                if (lastVisiblePosition >= 0) {
                    if (((ManagerActivity) context).isList) {
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
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setNodes(List<MegaNode> nodes) {
        Timber.d("setNodes");
        this.nodes = nodes;

        if (megaApi != null) {
            if (megaApi.getRubbishNode() == null) {
                Timber.e("megaApi.getRubbishNode() is NULL");
                return;
            }
        }

        this.nodes = nodes;

        if (adapter != null) {
            adapter.setNodes(this.nodes);
            if (adapter.getItemCount() == 0) {
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                if (megaApi.getRubbishNode().getHandle() == managerViewModel.getState().getValue().getRubbishBinParentHandle() || managerViewModel.getState().getValue().getRubbishBinParentHandle() == -1) {
                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_landscape);
                    } else {
                        emptyImageView.setImageResource(R.drawable.empty_rubbish_bin_portrait);
                    }
                    String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
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
                        textToShow = textToShow.replace("[A]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                                + "\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'"
                                + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                                + "\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    emptyTextViewFirst.setText(result);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public boolean isMultipleselect() {
        return adapter.isMultipleSelect();
    }

    public int getItemCount() {
        return adapter.getItemCount();
    }
}
