package mega.privacy.android.app.fragments.managerFragments.cu;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.databinding.FragmentCameraUploadsBinding;
import mega.privacy.android.app.databinding.FragmentCameraUploadsFirstLoginBinding;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
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
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_CUMU;
import static mega.privacy.android.app.utils.FileUtil.findVideoLocalPath;
import static mega.privacy.android.app.utils.FileUtil.setLocalIntentParams;
import static mega.privacy.android.app.utils.FileUtil.setStreamingIntentParams;
import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText;
import static mega.privacy.android.app.utils.Util.checkFingerprint;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

@AndroidEntryPoint
public class CameraUploadsFragment extends BaseFragment implements CameraUploadsAdapter.Listener {
    public static final int TYPE_CAMERA = MegaNodeRepo.CU_TYPE_CAMERA;
    public static final int TYPE_MEDIA = MegaNodeRepo.CU_TYPE_MEDIA;

    private static final String ARG_TYPE = "type";

    @Inject
    SortOrderManagement sortOrderManagement;

    // in large grid view, we have 3 thumbnails each row, while in small grid view, we have 5.
    private static final int SPAN_LARGE_GRID = 3;
    private static final int SPAN_SMALL_GRID = 5;

    private ManagerActivityLollipop mManagerActivity;

    private FragmentCameraUploadsFirstLoginBinding mFirstLoginBinding;
    private FragmentCameraUploadsBinding mBinding;
    private CameraUploadsAdapter mAdapter;
    private ActionMode mActionMode;

    private CuViewModel mViewModel;

    private static final String AD_SLOT = "and3";

    public int getItemCount() {
        return mAdapter == null ? 0 : mAdapter.getItemCount();
    }

    public void reloadNodes() {
        mViewModel.loadCuNodes();
    }

    public void checkScroll() {
        if (mViewModel == null || mBinding == null) {
            return;
        }

        mManagerActivity.changeAppBarElevation(mViewModel.isSelecting()
                || mBinding.cuList.canScrollVertically(SCROLLING_UP_DIRECTION));
    }

    public void selectAll() {
        mViewModel.selectAll();
    }

    public int onBackPressed() {
        if (mManagerActivity.isFirstNavigationLevel()) {
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
        mViewModel.setEnableCUShown(false);
        mViewModel.setCamSyncEnabled(false);
        mManagerActivity.setFirstNavigationLevel(false);

        if (mManagerActivity.isFirstLogin()) {
            mManagerActivity.skipInitialCUSetup();
        } else {
            mManagerActivity.refreshCameraUpload();
        }
    }

    private void requestCameraUploadPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(mManagerActivity, permissions,
                requestCode);
    }

    public void enableCu() {
        if (mFirstLoginBinding == null) {
            return;
        }

        mViewModel.enableCu(mFirstLoginBinding.cellularConnectionSwitch.isChecked(),
                mFirstLoginBinding.uploadVideosSwitch.isChecked());

        mManagerActivity.setFirstLogin(false);
        mViewModel.setEnableCUShown(false);
        startCU();
    }

    private void startCU() {
        mManagerActivity.refreshCameraUpload();

        new Handler().postDelayed(() -> {
            logDebug("Starting CU");
            startCameraUploadService(context);
        }, 1000);
    }

    public void resetSwitchButtonLabel() {
        if (mBinding == null) {
            return;
        }

        mBinding.turnOnCuButton.setVisibility(View.VISIBLE);
        mBinding.turnOnCuButton.setText(
                getString(R.string.settings_camera_upload_turn_on).toUpperCase(
                        Locale.getDefault()));
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManagerActivity = (ManagerActivityLollipop) context;

        CuViewModelFactory viewModelFactory =
                new CuViewModelFactory(megaApi, DatabaseHandler.getDbHandler(context),
                        new MegaNodeRepo(megaApi, dbH), context, sortOrderManagement);
        mViewModel = new ViewModelProvider(this, viewModelFactory).get(CuViewModel.class);

        initAdsLoader(AD_SLOT, true);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        if (mManagerActivity.getFirstLogin() || mViewModel.isEnableCUShown()) {
            mViewModel.setEnableCUShown(true);
            return createCameraUploadsViewForFirstLogin(inflater, container);
        } else {
            mBinding = FragmentCameraUploadsBinding.inflate(inflater, container, false);
            setupGoogleAds();
            return mBinding.getRoot();
        }
    }

    private View createCameraUploadsViewForFirstLogin(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container) {
        mViewModel.setInitialPreferences();

        mFirstLoginBinding =
                FragmentCameraUploadsFirstLoginBinding.inflate(inflater, container, false);

        new ListenScrollChangesHelper().addViewToListen(mFirstLoginBinding.camSyncScrollView,
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> mManagerActivity
                        .changeAppBarElevation(mFirstLoginBinding.camSyncScrollView.canScrollVertically(SCROLLING_UP_DIRECTION)));

        mFirstLoginBinding.enableButton.setOnClickListener(v -> {
            MegaApplication.getInstance().sendSignalPresenceActivity();
            String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
            if (hasPermissions(context, permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert();
            } else {
                requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
            }
        });

        return mFirstLoginBinding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mBinding == null) {
            return;
        }

        setupRecyclerView();
        setupOtherViews();
        observeLiveData();
    }

    /**
     * Set the Ads view container to the Ads Loader
     */
    private void setupGoogleAds() {
        mAdsLoader.setAdViewContainer(mBinding.adViewContainer,
                mManagerActivity.getOutMetrics());
    }

    private void setupRecyclerView() {
        boolean smallGrid = mManagerActivity.isSmallGridCameraUploads;
        int spanCount = smallGrid ? SPAN_SMALL_GRID : SPAN_LARGE_GRID;

        mBinding.cuList.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
        mBinding.cuList.setLayoutManager(layoutManager);
        mBinding.cuList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        int imageMargin = getResources().getDimensionPixelSize(
                smallGrid ? R.dimen.cu_fragment_image_margin_small
                        : R.dimen.cu_fragment_image_margin_large);
        int gridWidth = (outMetrics.widthPixels - imageMargin * spanCount * 2) / spanCount;
        int icSelectedWidth = getResources().getDimensionPixelSize(
                smallGrid ? R.dimen.cu_fragment_ic_selected_size_small
                        : R.dimen.cu_fragment_ic_selected_size_large);
        int icSelectedMargin = getResources().getDimensionPixelSize(
                smallGrid ? R.dimen.cu_fragment_ic_selected_margin_small
                        : R.dimen.cu_fragment_ic_selected_margin_large);
        CuItemSizeConfig itemSizeConfig = new CuItemSizeConfig(smallGrid, gridWidth,
                icSelectedWidth, imageMargin,
                getResources().getDimensionPixelSize(R.dimen.cu_fragment_selected_padding),
                icSelectedMargin,
                getResources().getDimensionPixelSize(
                        R.dimen.cu_fragment_selected_round_corner_radius));

        mAdapter = new CameraUploadsAdapter(this, spanCount, itemSizeConfig);
        mAdapter.setHasStableIds(true);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return mAdapter.getSpanSize(position);
            }
        });

        mBinding.cuList.setAdapter(mAdapter);
        mBinding.scroller.setRecyclerView(mBinding.cuList);
    }

    private void setupOtherViews() {
        mBinding.turnOnCuButton.setOnClickListener(v -> enableCUClick());
        mBinding.emptyEnableCuButton.setOnClickListener(v -> enableCUClick());
    }

    private void enableCUClick() {
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
        String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };

        if (hasPermissions(context, permissions)) {
            mViewModel.setEnableCUShown(true);
            mManagerActivity.refreshCameraUpload();
        } else {
            requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF);
        }
    }

    private void observeLiveData() {
        mViewModel.cuNodes().observe(getViewLifecycleOwner(), nodes -> {
            if (!isResumed()) {
                return;
            }

            boolean showScroller = nodes.size() >= (mManagerActivity.isSmallGridCameraUploads
                    ? MIN_ITEMS_SCROLLBAR_GRID : MIN_ITEMS_SCROLLBAR);
            mBinding.scroller.setVisibility(showScroller ? View.VISIBLE : View.GONE);
            mAdapter.setNodes(nodes);
            mManagerActivity.updateCuFragmentOptionsMenu();

            mBinding.emptyHint.setVisibility(nodes.isEmpty() ? View.VISIBLE : View.GONE);
            mBinding.cuList.setVisibility(nodes.isEmpty() ? View.GONE : View.VISIBLE);
            mBinding.scroller.setVisibility(nodes.isEmpty() ? View.GONE : View.VISIBLE);
            if (nodes.isEmpty()) {
                String textToShow = StringResourcesUtils.getString(R.string.photos_empty);
                mBinding.emptyHintText.setText(HtmlCompat.fromHtml(
                        formatEmptyScreenText(context, textToShow), HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        });

        mViewModel.nodeToOpen()
                .observe(getViewLifecycleOwner(), pair -> openNode(pair.first, pair.second));

        mViewModel.nodeToAnimate().observe(getViewLifecycleOwner(), pair -> {
            if (pair.first < 0 || pair.first >= mAdapter.getItemCount()) {
                return;
            }

            mAdapter.showSelectionAnimation(pair.first, pair.second,
                    mBinding.cuList.findViewHolderForLayoutPosition(pair.first));
        });

        mViewModel.actionBarTitle().observe(getViewLifecycleOwner(), title -> {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        });

        mViewModel.actionMode().observe(getViewLifecycleOwner(), visible -> {
            if (visible) {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) context).startSupportActionMode(
                            new CuActionModeCallback(context, this, mViewModel, megaApi));
                }

                mActionMode.setTitle(String.valueOf(mViewModel.getSelectedNodesCount()));
                mActionMode.invalidate();
            } else {
                if (mActionMode != null) {
                    mActionMode.finish();
                    mActionMode = null;
                }
            }
        });

        mViewModel.camSyncEnabled()
                .observe(getViewLifecycleOwner(), enabled -> {
                            boolean empty = mAdapter.getItemCount() <= 0;
                            mBinding.turnOnCuButton.setVisibility(!enabled && !empty ? View.VISIBLE : View.GONE);
                            mBinding.emptyEnableCuButton.setVisibility(!enabled && empty ? View.VISIBLE : View.GONE);
                        }
                );

        observeDragSupportEvents(getViewLifecycleOwner(), mBinding.cuList, VIEWER_FROM_CUMU);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_ON_OFF:
            case REQUEST_CAMERA_ON_OFF_FIRST_TIME: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mManagerActivity.checkIfShouldShowBusinessCUAlert();
                }
                break;
            }
        }
    }

    @Override public void onResume() {
        super.onResume();

        reloadNodes();
    }

    private void openNode(int position, CuNode cuNode) {
        if (position < 0 || position >= mAdapter.getItemCount()) {
            return;
        }

        MegaNode node = cuNode.getNode();
        if (node == null) {
            return;
        }

        MegaNode parentNode = megaApi.getParentNode(node);
        Intent intent = new Intent(context, FullScreenImageViewerLollipop.class)
                .putExtra(INTENT_EXTRA_KEY_POSITION, cuNode.getIndexForViewer())
                .putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, sortOrderManagement.getOrderCamera())
                .putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle())
                .putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                        parentNode == null || parentNode.getType() == MegaNode.TYPE_ROOT
                                ? INVALID_HANDLE
                                : parentNode.getHandle())
                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTO_SYNC_ADAPTER);

        putThumbnailLocation(intent, mBinding.cuList, position, VIEWER_FROM_CUMU, mAdapter);
        startActivity(intent);
        requireActivity().overridePendingTransition(0, 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override public void onNodeClicked(int position, CuNode node) {
        mViewModel.onNodeClicked(position, node);
    }

    @Override public void onNodeLongClicked(int position, CuNode node) {
        mViewModel.onNodeLongClicked(position, node);
    }

    public boolean isEnableCUFragmentShown() {
        return mViewModel.isEnableCUShown();
    }
}
