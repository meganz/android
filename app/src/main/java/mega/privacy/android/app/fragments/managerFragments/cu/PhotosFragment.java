package mega.privacy.android.app.fragments.managerFragments.cu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.databinding.FragmentPhotosBinding;
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.ZoomUtil;
import nz.mega.sdk.MegaNode;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.utils.ColorUtils.DARK_IMAGE_ALPHA;
import static mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark;
import static mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_GRID;
import static mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_CUMU;
import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.permission.PermissionUtils.*;
import static mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static mega.privacy.android.app.utils.ZoomUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

@AndroidEntryPoint
public class PhotosFragment extends BaseZoomFragment implements CUGridViewAdapter.Listener,
        CUCardViewAdapter.Listener {

    private static final String SELECTED_VIEW = "SELECTED_VIEW";
    public static final int ALL_VIEW = 0;
    public static final int DAYS_VIEW = 1;
    public static final int MONTHS_VIEW = 2;
    public static final int YEARS_VIEW = 3;

    // Cards per row
    public static final int SPAN_CARD_PORTRAIT = 1;
    public static final int SPAN_CARD_LANDSCAPE = 2;

    @Inject
    SortOrderManagement sortOrderManagement;

    private ManagerActivityLollipop mManagerActivity;
    private FragmentPhotosBinding binding;
    private CUGridViewAdapter gridAdapter;
    private CUCardViewAdapter cardAdapter;
    private ActionMode mActionMode;

    private LinearLayout viewTypesLayout;
    private TextView yearsButton;
    private TextView monthsButton;
    private TextView daysButton;
    private TextView allButton;

    private CuViewModel viewModel;

    private GridLayoutManager layoutManager;

    private ScaleGestureHandler scaleGestureHandler;

    private int selectedView = ALL_VIEW;

    public int getItemCount() {
        return gridAdapter == null ? 0 : gridAdapter.getItemCount();
    }

    public void reloadNodes() {
        viewModel.loadNodes();
        viewModel.getCards();
    }

    public void checkScroll() {
        if (viewModel == null || binding == null) {
            return;
        }

        boolean isScrolled = binding.cuList.canScrollVertically(SCROLLING_UP_DIRECTION);
        mManagerActivity.changeAppBarElevation(binding.uploadProgress.getVisibility() == View.VISIBLE
                || viewModel.isSelecting() || isScrolled);
    }

    public void selectAll() {
        viewModel.selectAll();
    }

    public int onBackPressed() {
        if (mManagerActivity.isFirstNavigationLevel()) {
            if (selectedView != ALL_VIEW) {
                mManagerActivity.enableHideBottomViewOnScroll(false);
                mManagerActivity.showBottomView();
            }

            return 0;
        } else if (isEnableCUFragmentShown()) {
            skipCUSetup();
            return 1;
        } else {
            reloadNodes();
            mManagerActivity.invalidateOptionsMenu();
            mManagerActivity.setToolbarTitle();
            return 1;
        }
    }

    public void onStoragePermissionRefused() {
        showSnackbar(context, getString(R.string.on_refuse_storage_permission));
        skipCUSetup();
    }

    private void skipCUSetup() {
        viewModel.setEnableCUShown(false);
        viewModel.setCamSyncEnabled(false);
        mManagerActivity.setFirstNavigationLevel(false);

        if (mManagerActivity.isFirstLogin()) {
            mManagerActivity.skipInitialCUSetup();
        } else {
            mManagerActivity.refreshPhotosFragment();
        }
    }

    private void requestCameraUploadPermission(String[] permissions, int requestCode) {
        requestPermission(mManagerActivity, requestCode, permissions);
    }

    public void enableCu() {
        viewModel.enableCu(binding.fragmentPhotosFirstLogin.cellularConnectionSwitch.isChecked(),
                binding.fragmentPhotosFirstLogin.uploadVideosSwitch.isChecked());

        mManagerActivity.setFirstLogin(false);
        viewModel.setEnableCUShown(false);
        startCU();
    }

    private void startCU() {
        mManagerActivity.refreshPhotosFragment();

        new Handler().postDelayed(() -> {
            logDebug("Starting CU");
            startCameraUploadService(context);
        }, 1000);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedView = savedInstanceState.getInt(SELECTED_VIEW, ALL_VIEW);
        }

        mManagerActivity = (ManagerActivityLollipop) context;

        CuViewModelFactory viewModelFactory =
                new CuViewModelFactory(megaApi, DatabaseHandler.getDbHandler(context),
                        new MegaNodeRepo(megaApi, dbH), context, sortOrderManagement);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(CuViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPhotosBinding.inflate(inflater, container, false);

        if (mManagerActivity.getFirstLogin() || viewModel.isEnableCUShown()) {
            viewModel.setEnableCUShown(true);
            createCameraUploadsViewForFirstLogin();
        } else {
            showPhotosGrid();
        }

        return binding.getRoot();
    }

    /**
     * Refresh view and layout after CU enabled or disabled.
     */
    public void refreshViewLayout() {
        if(isEnableCUFragmentShown()) {
            showEnablePage();
            createCameraUploadsViewForFirstLogin();
        } else {
            showPhotosGrid();

        }

        initAfterViewCreated();
    }

    /**
     * Show photos view.
     */
    private void showPhotosGrid() {
        binding.fragmentPhotosFirstLogin.getRoot().setVisibility(View.GONE);
        binding.fragmentPhotosGrid.setVisibility(View.VISIBLE);
    }

    /**
     * Show enable CU page.
     */
    private void showEnablePage() {
        binding.fragmentPhotosFirstLogin.getRoot().setVisibility(View.VISIBLE);
        binding.fragmentPhotosGrid.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        outState.putInt(SELECTED_VIEW, selectedView);
        super.onSaveInstanceState(outState);
    }

    private void createCameraUploadsViewForFirstLogin() {
        viewModel.setInitialPreferences();

        new ListenScrollChangesHelper().addViewToListen(binding.fragmentPhotosFirstLogin.camSyncScrollView,
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> mManagerActivity
                        .changeAppBarElevation(binding.fragmentPhotosFirstLogin.camSyncScrollView.canScrollVertically(SCROLLING_UP_DIRECTION)));

        binding.fragmentPhotosFirstLogin.enableButton.setOnClickListener(v -> {
            MegaApplication.getInstance().sendSignalPresenceActivity();
            String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE};
            if (hasPermissions(context, permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert();
            } else {
                requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initAfterViewCreated();
    }

    /**
     * Init UI and view model when view is created or refreshed.
     */
    private void initAfterViewCreated() {
        if (viewModel.isEnableCUShown()) {
            mManagerActivity.updateCULayout(View.GONE);
            mManagerActivity.updateCUViewTypes(View.GONE);

            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    mManagerActivity.showSnackbar(DISMISS_ACTION_SNACKBAR,
                            StringResourcesUtils.getString(R.string.video_quality_info),
                            MEGACHAT_INVALID_HANDLE);
                }

                binding.fragmentPhotosFirstLogin.qualityText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });

            handlePhotosMenuUpdate(false);
            return;
        }

        viewModel.resetOpenedNode();
        mManagerActivity.updateCUViewTypes(View.VISIBLE);
        setupRecyclerView();
        setupViewTypes();
        setupOtherViews();
        observeLiveData();

        int currentZoom = ZoomUtil.getPHOTO_ZOOM_LEVEL();
        getZoomViewModel().setCurrentZoom(currentZoom);
        getZoomViewModel().setZoom(currentZoom);
        viewModel.setZoom(currentZoom);

        viewModel.getCards();
        viewModel.getCUNodes();
    }

    public void setViewTypes(LinearLayout cuViewTypes, TextView cuYearsButton,
                             TextView cuMonthsButton, TextView cuDaysButton, TextView cuAllButton) {
        this.viewTypesLayout = cuViewTypes;
        this.yearsButton = cuYearsButton;
        this.monthsButton = cuMonthsButton;
        this.daysButton = cuDaysButton;
        this.allButton = cuAllButton;

        setupViewTypes();
    }

    private void setupViewTypes() {
        if (allButton != null) {
            allButton.setOnClickListener(v -> newViewClicked(ALL_VIEW));
        }

        if (daysButton != null) {
            daysButton.setOnClickListener(v -> newViewClicked(DAYS_VIEW));
        }

        if (monthsButton != null) {
            monthsButton.setOnClickListener(v -> newViewClicked(MONTHS_VIEW));
        }

        if (yearsButton != null) {
            yearsButton.setOnClickListener(v -> newViewClicked(YEARS_VIEW));
        }

        if (context != null
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && viewTypesLayout != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewTypesLayout.getLayoutParams();
            params.width = outMetrics.heightPixels;
            viewTypesLayout.setLayoutParams(params);
        }

        if (getView() != null) {
            updateViewSelected();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRecyclerView() {
        binding.cuList.setHasFixedSize(true);
        binding.cuList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        scaleGestureHandler = new ScaleGestureHandler(context, this);
        binding.cuList.setOnTouchListener(scaleGestureHandler);

        setGridView();
    }

    private void setGridView() {
        viewModel.clearSelection();

        boolean isPortrait = getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
        int spanCount = getSpanCount(isPortrait);

        layoutManager = new GridLayoutManager(context, spanCount);
        binding.cuList.setLayoutManager(layoutManager);
        binding.cuList.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.cu_margin_bottom));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.cuList.getLayoutParams();

        if (selectedView == ALL_VIEW) {
            int imageMargin = ZoomUtil.INSTANCE.getMargin(context, getCurrentZoom());
            ZoomUtil.INSTANCE.setMargin(context, params, getCurrentZoom());
            int gridWidth = ZoomUtil.INSTANCE.getItemWidth(context, outMetrics, getCurrentZoom(), spanCount);

            int icSelectedWidth = ZoomUtil.INSTANCE.getSelectedFrameWidth(context, getCurrentZoom());

            int icSelectedMargin = ZoomUtil.INSTANCE.getSelectedFrameMargin(context, getCurrentZoom());

            CuItemSizeConfig itemSizeConfig = new CuItemSizeConfig(getCurrentZoom(), gridWidth,
                    icSelectedWidth, imageMargin,
                    getResources().getDimensionPixelSize(R.dimen.cu_fragment_selected_padding),
                    icSelectedMargin,
                    getResources().getDimensionPixelSize(
                            R.dimen.cu_fragment_selected_round_corner_radius));
            if (gridAdapter == null) {
                gridAdapter = new CUGridViewAdapter(this, spanCount, itemSizeConfig);
            } else {
                gridAdapter.setSpanCount(spanCount);
                gridAdapter.setCuItemSizeConfig(itemSizeConfig);
            }
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return gridAdapter.getSpanSize(position);
                }
            });
            binding.cuList.setAdapter(gridAdapter);
        } else {
            int cardMargin = getResources().getDimensionPixelSize(isPortrait
                    ? R.dimen.card_margin_portrait
                    : R.dimen.card_margin_landscape);

            int cardWidth = ((outMetrics.widthPixels - cardMargin * spanCount * 2) - cardMargin * 2) / spanCount;

            cardAdapter = new CUCardViewAdapter(selectedView, cardWidth, cardMargin, this);
            cardAdapter.setHasStableIds(true);
            binding.cuList.setAdapter(cardAdapter);
            params.leftMargin = params.rightMargin = cardMargin;
        }

        binding.cuList.setLayoutParams(params);
        binding.scroller.setRecyclerView(binding.cuList);
    }

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private int getSpanCount(boolean isPortrait) {
        return super.getSpanCount(selectedView,isPortrait);
    }

    private void setupOtherViews() {
        binding.emptyEnableCuButton.setOnClickListener(v -> enableCUClick());
        setImageViewAlphaIfDark(context, binding.emptyHintImage, DARK_IMAGE_ALPHA);
        binding.emptyHintText.setText(HtmlCompat.fromHtml(
                formatEmptyScreenText(context, StringResourcesUtils.getString(R.string.photos_empty)),
                HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    /**
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void newViewClicked(int selectedView) {
        if (this.selectedView == selectedView) {
            return;
        }
        this.selectedView = selectedView;
        setGridView();

        switch (selectedView) {
            case DAYS_VIEW:
                showDayCards(viewModel.getDayCards());
                binding.cuList.setOnTouchListener(null);
                break;

            case MONTHS_VIEW:
                showMonthCards(viewModel.getMonthCards());
                binding.cuList.setOnTouchListener(null);
                break;

            case YEARS_VIEW:
                showYearCards(viewModel.getYearCards());
                binding.cuList.setOnTouchListener(null);
                break;

            default:
                gridAdapter.setNodes(viewModel.getCUNodes());
                binding.cuList.setOnTouchListener(scaleGestureHandler);
        }
        handleOptionsMenuUpdate(shouldShowFullInfoAndOptions());
        updateViewSelected();
    }

    public void enableCUClick() {
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
        String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE};

        if (hasPermissions(context, permissions)) {
            viewModel.setEnableCUShown(true);
            mManagerActivity.refreshPhotosFragment();
        } else {
            requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF);
        }
    }

    private void observeLiveData() {
        viewModel.cuNodes().observe(getViewLifecycleOwner(), nodes -> {
            // On enable CU page, don't update layout and view.
            if(isEnableCUFragmentShown()) return;

            boolean showScroller = nodes.size() >= (getCurrentZoom() < ZOOM_DEFAULT ? MIN_ITEMS_SCROLLBAR_GRID : MIN_ITEMS_SCROLLBAR);
            binding.scroller.setVisibility(showScroller ? View.VISIBLE : View.GONE);

            if (gridAdapter != null) {
                gridAdapter.setNodes(nodes);
            }

            updateEnableCUButtons(viewModel.isCUEnabled());
            handlePhotosMenuUpdate(isShowMenu());

            binding.emptyHint.setVisibility(nodes.isEmpty() ? View.VISIBLE : View.GONE);
            binding.cuList.setVisibility(nodes.isEmpty() ? View.GONE : View.VISIBLE);
            binding.scroller.setVisibility(nodes.isEmpty() ? View.GONE : View.VISIBLE);
            mManagerActivity.updateCUViewTypes(nodes.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.nodeToOpen()
                .observe(getViewLifecycleOwner(), pair -> openNode(pair.first, pair.second));

        viewModel.nodeToAnimate().observe(getViewLifecycleOwner(), pair -> {
            if (gridAdapter == null || pair.first < 0 || pair.first >= gridAdapter.getItemCount()) {
                return;
            }

            gridAdapter.showSelectionAnimation(pair.first, pair.second,
                    binding.cuList.findViewHolderForLayoutPosition(pair.first));
        });

        viewModel.actionBarTitle().observe(getViewLifecycleOwner(), title -> {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        });

        viewModel.actionMode().observe(getViewLifecycleOwner(), visible -> {
            if (visible) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) context).startSupportActionMode(
                            new CuActionModeCallback(context, this, viewModel, megaApi));
                }

                mActionMode.setTitle(String.valueOf(viewModel.getSelectedNodesCount()));
                mActionMode.invalidate();
            } else if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }

            animateUI(visible);
        });

        viewModel.camSyncEnabled().observe(getViewLifecycleOwner(), this::updateEnableCUButtons);
        observeDragSupportEvents(getViewLifecycleOwner(), binding.cuList, VIEWER_FROM_CUMU);

        viewModel.getDayCardsData().observe(getViewLifecycleOwner(), this::showDayCards);
        viewModel.getMonthCardsData().observe(getViewLifecycleOwner(), this::showMonthCards);
        viewModel.getYearCardsData().observe(getViewLifecycleOwner(), this::showYearCards);
    }

    /**
     * Animates the UI by showing or hiding some views.
     * Enables or disables the translucent navigation bar only if portrait mode.
     *
     * @param hide True if should hide the UI, false otherwise.
     */
    private void animateUI(boolean hide) {
        mManagerActivity.animateCULayout(hide || viewModel.isCUEnabled());
        mManagerActivity.animateBottomView(hide);
        mManagerActivity.setDrawerLockMode(hide);
        checkScroll();
    }

    /**
     * Updates CU enable buttons visibility depending on if CU is enabled/disabled
     * and if the view contains some node.
     *
     * @param cuEnabled True if CU is enabled, false otherwise.
     */
    private void updateEnableCUButtons(boolean cuEnabled) {
        boolean emptyAdapter = gridAdapter == null || gridAdapter.getItemCount() <= 0;
        binding.emptyEnableCuButton.setVisibility(!cuEnabled && emptyAdapter ? View.VISIBLE : View.GONE);
        mManagerActivity.updateEnableCUButton(selectedView == ALL_VIEW && !cuEnabled
                && !emptyAdapter && mActionMode == null
                ? View.VISIBLE
                : View.GONE);

        if (!cuEnabled) {
            hideCUProgress();
        }
    }

    /**
     * this method handle is show menu.
     *
     * @return false, when no photo here or in the action mode or not in all view, then will hide the menu.
     * Otherwise, true, show menu.
     */
    private boolean isShowMenu() {
        boolean emptyAdapter = gridAdapter == null || gridAdapter.getItemCount() <= 0;
        return !emptyAdapter && mActionMode == null && selectedView == ALL_VIEW;
    }

    private void showDayCards(List<CUCard> dayCards) {
        if (selectedView == DAYS_VIEW) {
            cardAdapter.submitList(dayCards);
        }
    }

    private void showMonthCards(List<CUCard> monthCards) {
        if (selectedView == MONTHS_VIEW) {
            cardAdapter.submitList(monthCards);
        }
    }

    private void showYearCards(List<CUCard> yearCards) {
        if (selectedView == YEARS_VIEW) {
            cardAdapter.submitList(yearCards);
        }
    }

    private void openNode(int position, CuNode cuNode) {
        if (position < 0 || gridAdapter == null || position >= gridAdapter.getItemCount()) {
            return;
        }

        MegaNode node = cuNode.getNode();
        if (node == null) {
            return;
        }

        MegaNode parentNode = megaApi.getParentNode(node);
        Intent intent = ImageViewerActivity.getIntentForParentNode(
                requireContext(),
                parentNode.getHandle(),
                sortOrderManagement.getOrderCamera(),
                node.getHandle()
        );
        putThumbnailLocation(intent, binding.cuList, position, VIEWER_FROM_CUMU, gridAdapter);
        startActivity(intent);
        requireActivity().overridePendingTransition(0, 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onNodeClicked(int position, CuNode node) {
        viewModel.onNodeClicked(position, node);
    }

    @Override
    public void onNodeLongClicked(int position, CuNode node) {
        // Multiple selection only available for zoom default (3 items per row) or zoom out 1x (5 items per row).
        if (getCurrentZoom() == ZOOM_DEFAULT || getCurrentZoom() == ZOOM_OUT_1X) {
            viewModel.onNodeLongClicked(position, node);
        }
    }

    public boolean isEnableCUFragmentShown() {
        return viewModel.isEnableCUShown();
    }

    public boolean shouldShowFullInfoAndOptions() {
        return !isEnableCUFragmentShown() && selectedView == ALL_VIEW;
    }

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    private void updateViewSelected() {
        super.updateViewSelected(allButton,daysButton,monthsButton,yearsButton,selectedView);

        updateFastScrollerVisibility();
        mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW);
        mManagerActivity.updateEnableCUButton(selectedView == ALL_VIEW
                && (gridAdapter != null && gridAdapter.getItemCount() > 0)
                && (viewModel != null && !viewModel.isCUEnabled())
                ? View.VISIBLE
                : View.GONE);

        if (selectedView != ALL_VIEW) {
            hideCUProgress();
            binding.uploadProgress.setVisibility(View.GONE);
        }
    }

    /**
     * Hides CU progress bar and checks the scroll
     * in order to hide elevation if the list is not scrolled.
     */
    private void hideCUProgress() {
        mManagerActivity.hideCUProgress();
        checkScroll();
    }

    private void updateFastScrollerVisibility() {
        if (binding == null || cardAdapter == null) {
            return;
        }
        super.updateFastScrollerVisibility(selectedView, binding.scroller, cardAdapter.getItemCount());
    }

    @Override
    public void onCardClicked(int position, @NonNull CUCard card) {
        switch (selectedView) {
            case DAYS_VIEW:
                getZoomViewModel().restoreDefaultZoom();
                handleZoomMenuItemStatus();
                card = viewModel.dayClicked(position, card);
                newViewClicked(ALL_VIEW);
                int cuNodePosition = gridAdapter.getNodePosition(card.getNode().getHandle());
                openNode(cuNodePosition, gridAdapter.getNodeAtPosition(cuNodePosition));
                layoutManager.scrollToPosition(cuNodePosition);
                break;

            case MONTHS_VIEW:
                newViewClicked(DAYS_VIEW);
                layoutManager.scrollToPosition(viewModel.monthClicked(position, card));
                break;

            case YEARS_VIEW:
                newViewClicked(MONTHS_VIEW);
                layoutManager.scrollToPosition(viewModel.yearClicked(position, card));
                break;
        }
        mManagerActivity.showBottomView();
    }

    public void updateProgress(int visibility, int pending) {
        if (binding.uploadProgress.getVisibility() != visibility) {
            binding.uploadProgress.setVisibility(visibility);
            checkScroll();
        }

        binding.uploadProgress.setText(StringResourcesUtils
                .getQuantityString(R.plurals.cu_upload_progress, pending, pending));
    }

    public void setDefaultView() {
        newViewClicked(ALL_VIEW);
    }

    @Override
    public void handleZoomChange(int zoom, boolean needReload) {
        handleZoomAdapterLayoutChange(zoom);
        if (needReload) {
            reloadNodes();
        }
    }

    private void handleZoomAdapterLayoutChange(int zoom) {
        if (!viewModel.isEnableCUShown()) {
            viewModel.setZoom(zoom);
            ZoomUtil.setPHOTO_ZOOM_LEVEL(zoom);
            Parcelable state = layoutManager.onSaveInstanceState();
            setGridView();
            layoutManager.onRestoreInstanceState(state);
        }
    }

    @Override
    public void handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(isShowMenu());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!isInPhotosPage()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!isInPhotosPage()) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isInPhotosPage(){
        return (ManagerActivityLollipop) getActivity() != null && ((ManagerActivityLollipop) getActivity()).getDrawerItem() == ManagerActivityLollipop.DrawerItem.PHOTOS;
    }

    public void handlePhotosMenuUpdate(boolean isShowMenu){
        if (!isInPhotosPage()) {
            return;
        }
        handleOptionsMenuUpdate(isShowMenu);
    }
}
