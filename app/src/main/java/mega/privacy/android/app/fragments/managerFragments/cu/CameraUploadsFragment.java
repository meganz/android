package mega.privacy.android.app.fragments.managerFragments.cu;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.databinding.FragmentCameraUploadsBinding;
import mega.privacy.android.app.databinding.FragmentCameraUploadsFirstLoginBinding;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.BUSINESS_CU_FRAGMENT_CU;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetCUTimestampsAndCache;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_GRID;
import static mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME;
import static mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_CUMU;
import static mega.privacy.android.app.utils.FileUtil.findVideoLocalPath;
import static mega.privacy.android.app.utils.FileUtil.setLocalIntentParams;
import static mega.privacy.android.app.utils.FileUtil.setStreamingIntentParams;
import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;
import static mega.privacy.android.app.utils.JobUtil.stopRunningCameraUploadService;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.PermissionUtils.hasPermissions;
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

    // in large grid view, we have 3 thumbnails each row, while in small grid view, we have 7.
    private static final int SPAN_LARGE_GRID = 3;
    private static final int SPAN_SMALL_GRID = 7;

    private int mCamera = TYPE_CAMERA;

    private ManagerActivityLollipop mManagerActivity;

    private FragmentCameraUploadsFirstLoginBinding mFirstLoginBinding;
    private FragmentCameraUploadsBinding mBinding;
    private CameraUploadsAdapter mAdapter;
    private ActionMode mActionMode;

    private CuViewModel mViewModel;

    private static final String AD_SLOT = "and3";
    private static long[] cuSearchDate = null;

    public static CameraUploadsFragment newInstance(int type) {
        CameraUploadsFragment fragment = new CameraUploadsFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);

        return fragment;
    }

    public int getItemCount() {
        return mAdapter == null ? 0 : mAdapter.getItemCount();
    }

    public void setOrderBy(int orderBy) {
        reloadNodes(orderBy);
    }

    /**
     * Search the media of camera
     * @param searchDate the date or date range for searching
     * @param orderBy The order of sort
     */
    public void setSearchDate(long[] searchDate, int orderBy) {
        cuSearchDate = searchDate;
        if (mViewModel != null) {
            mViewModel.setSearchDate(searchDate, orderBy);
        }
    }

    public void reloadNodes(int orderBy) {
        mViewModel.loadCuNodes(orderBy);
    }

    public void checkScroll() {
        if (mViewModel == null || mBinding == null) {
            return;
        }

        if (mViewModel.isSelecting() || mBinding.cuList.canScrollVertically(-1)) {
            mManagerActivity.changeAppBarElevation(true);
        } else {
            mManagerActivity.changeAppBarElevation(false);
        }
    }

    public void selectAll() {
        mViewModel.selectAll();
    }

    public int onBackPressed() {
        if (mManagerActivity.getFirstLogin()) {
            mViewModel.setCamSyncEnabled(false);
            mManagerActivity.setFirstLogin(false);
            mManagerActivity.refreshMenu();
        }

        if (mManagerActivity.isFirstNavigationLevel()) {
            return 0;
        } else {
            reloadNodes(sortOrderManagement.getOrderCamera());

            // When press back, reload all files.
            setSearchDate(null, sortOrderManagement.getOrderCamera());
            mManagerActivity.invalidateOptionsMenu();
            mManagerActivity.setIsSearchEnabled(false);
            mManagerActivity.setToolbarTitle();
            return 1;
        }
    }

    public void onStoragePermissionRefused() {
        showSnackbar(context, getString(R.string.on_refuse_storage_permission));
        skipInitialCUSetup();
    }

    private void skipInitialCUSetup() {
        mViewModel.setCamSyncEnabled(false);
        mManagerActivity.setFirstLogin(false);
        mManagerActivity.skipInitialCUSetup();
    }

    private void requestCameraUploadPermission(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(mManagerActivity, permissions,
                requestCode);
    }

    public void enableCuForBusinessFirstTime() {
        if (mFirstLoginBinding == null) {
            return;
        }

        boolean enableCellularSync = mFirstLoginBinding.cellularConnectionSwitch.isChecked();
        boolean syncVideo = mFirstLoginBinding.uploadVideosSwitch.isChecked();

        mViewModel.enableCuForBusinessFirstTime(enableCellularSync, syncVideo);

        mManagerActivity.setFirstLogin(false);
        startCU();
    }

    /**
     * This function is kept almost the same as it was in CameraUploadFragmentLollipop#cameraOnOff.
     */
    public void enableCuForBusiness() {
        MegaPreferences prefs = dbH.getPreferences();
        boolean isEnabled = false;
        if (prefs != null) {
            if (prefs.getCamSyncEnabled() != null) {
                if (Boolean.parseBoolean(prefs.getCamSyncEnabled())) {
                    isEnabled = true;
                }
            }
        }

        if (isEnabled) {
            resetCUTimestampsAndCache();
            dbH.setCamSyncEnabled(false);
            dbH.deleteAllSyncRecords(SyncRecord.TYPE_ANY);
            stopRunningCameraUploadService(context);
            mManagerActivity.refreshCameraUpload();
        } else {
            prefs = dbH.getPreferences();
            if (prefs != null &&
                    !TextUtils.isEmpty(prefs.getCamSyncLocalPath()) &&
                    !TextUtils.isEmpty(prefs.getCamSyncFileUpload()) &&
                    !TextUtils.isEmpty(prefs.getCamSyncWifi())
            ) {
                resetCUTimestampsAndCache();
                dbH.setCamSyncEnabled(true);
                dbH.deleteAllSyncRecords(SyncRecord.TYPE_ANY);

                //video quality
                saveCompressionSettings();
                startCU();

                return;
            }

            final ListAdapter adapter =
                    new ArrayAdapter<>(context, R.layout.select_dialog_singlechoice,
                            android.R.id.text1,
                            new String[] {
                                    getResources().getString(R.string.cam_sync_wifi),
                                    getResources().getString(R.string.cam_sync_data)
                            });
            new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                    .setTitle(getString(R.string.section_photo_sync))
                    .setSingleChoiceItems(adapter, -1, (dialog, which) -> {
                        resetCUTimestampsAndCache();
                        dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
                        File localFile =
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DCIM);
                        String localPath = localFile.getAbsolutePath();
                        dbH.setCamSyncLocalPath(localPath);
                        dbH.setCameraFolderExternalSDCard(false);
                        // After target and local folder setup, then enable CU.
                        dbH.setCamSyncEnabled(true);

                        switch (which) {
                            case 0: {
                                dbH.setCamSyncWifi(true);
                                break;
                            }
                            case 1: {
                                dbH.setCamSyncWifi(false);
                                break;
                            }
                        }

                        startCU();
                        dialog.dismiss();
                    })
                    .setPositiveButton(context.getString(R.string.general_cancel),
                            (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
    }

    private void saveCompressionSettings() {
        dbH.setCameraUploadVideoQuality(MEDIUM);
        dbH.setConversionOnCharging(true);

        dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
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

        Bundle args = getArguments();
        if (args != null) {
            mCamera = getArguments().getInt(ARG_TYPE, TYPE_CAMERA);
        }

        mManagerActivity = (ManagerActivityLollipop) context;

        CuViewModelFactory viewModelFactory =
                new CuViewModelFactory(megaApi, DatabaseHandler.getDbHandler(context),
                        new MegaNodeRepo(context, megaApi, dbH), context, mCamera, cuSearchDate);
        mViewModel = new ViewModelProvider(this, viewModelFactory).get(CuViewModel.class);

        initAdsLoader(AD_SLOT, true);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        if (mCamera == TYPE_CAMERA && mManagerActivity.getFirstLogin()) {
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
                        .changeAppBarElevation(mFirstLoginBinding.camSyncScrollView.canScrollVertically(-1)));

        mFirstLoginBinding.camSyncButtonOk.setOnClickListener(v -> {
            ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
            String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
            if (hasPermissions(context, permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert(
                        BUSINESS_CU_FRAGMENT_CU, true);
            } else {
                requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
            }
        });
        mFirstLoginBinding.camSyncButtonSkip.setOnClickListener(v -> {
            ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
            skipInitialCUSetup();
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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBinding.emptyHintImage.setImageResource(R.drawable.empty_cu_landscape);
        } else {
            mBinding.emptyHintImage.setImageResource(R.drawable.empty_cu_portrait);
        }

        if (mCamera == TYPE_CAMERA) {
            mBinding.turnOnCuButton.setText(
                    getString(R.string.settings_camera_upload_turn_on).toUpperCase(
                            Locale.getDefault()));
        } else {
            mBinding.turnOnCuButton.setText(
                    getString(R.string.settings_set_up_automatic_uploads).toUpperCase(
                            Locale.getDefault()));
        }

        mBinding.turnOnCuButton.setOnClickListener(v -> {
            ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
            String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };

            if (mCamera == TYPE_CAMERA) {
                if (hasPermissions(context, permissions)) {
                    mManagerActivity.checkIfShouldShowBusinessCUAlert(
                            BUSINESS_CU_FRAGMENT_CU, false);
                } else {
                    requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF);
                }
            } else {
                mManagerActivity.moveToSettingsSection();
            }
        });
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
                mBinding.emptyHintImage.setVisibility(
                        mViewModel.isSearchMode() ? View.GONE : View.VISIBLE);
                if (mViewModel.isSearchMode()) {
                    mBinding.emptyHintText.setText(R.string.no_results_found);
                } else {
                    String textToShow = getString(R.string.context_empty_camera_uploads);
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
                    } catch (Exception ignored) {
                    }
                    Spanned result;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    mBinding.emptyHintText.setText(result);
                }
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
            if (actionBar != null && mViewModel.isSearchMode()) {
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
                .observe(getViewLifecycleOwner(), enabled -> mBinding.turnOnCuButton.setVisibility(
                        enabled ? View.GONE : View.VISIBLE));

        observeDragSupportEvents(getViewLifecycleOwner(), mBinding.cuList, VIEWER_FROM_CUMU);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_ON_OFF: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mManagerActivity.checkIfShouldShowBusinessCUAlert(
                            BUSINESS_CU_FRAGMENT_CU, false);
                }
                break;
            }
            case REQUEST_CAMERA_ON_OFF_FIRST_TIME: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mManagerActivity.checkIfShouldShowBusinessCUAlert(
                            BUSINESS_CU_FRAGMENT_CU, true);
                }
                break;
            }
        }
    }

    @Override public void onResume() {
        super.onResume();

        reloadNodes(sortOrderManagement.getOrderCamera());
    }

    private void openNode(int position, CuNode cuNode) {
        if (position < 0 || position >= mAdapter.getItemCount()) {
            return;
        }

        MegaNode node = cuNode.getNode();
        if (node == null) {
            return;
        }

        MimeTypeThumbnail mime = MimeTypeThumbnail.typeForName(node.getName());
        if (mime.isImage()) {
            Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
            putExtras(intent, cuNode.getIndexForViewer(), position, node);
            launchNodeViewer(intent);
        } else if (mime.isVideoReproducible()) {
            Intent mediaIntent;
            if (mime.isVideoNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
            } else {
                mediaIntent = getMediaIntent(context, node.getName());
            }

            putExtras(mediaIntent, cuNode.getIndexForViewer(), position, node);

            mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());

            boolean paramsSetSuccessfully;
            String localPath = null;
            try {
                localPath = findVideoLocalPath(context, node);
            } catch (Exception e) {
                logWarning(e.getMessage());
            }
            if (localPath != null && checkFingerprint(megaApi, node, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(context, node, mediaIntent, localPath,
                        false, mManagerActivity);
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(context, node, megaApi,
                        mediaIntent, mManagerActivity);
            }
            if (!isIntentAvailable(context, mediaIntent)) {
                mManagerActivity.showSnackbar(SNACKBAR_TYPE,
                        getString(R.string.intent_not_available), MEGACHAT_INVALID_HANDLE);
                paramsSetSuccessfully = false;
            }
            if (paramsSetSuccessfully) {
                launchNodeViewer(mediaIntent);
            }
        }
    }

    private void putExtras(Intent intent, int indexForViewer, int position, MegaNode node) {
        intent.putExtra(INTENT_EXTRA_KEY_POSITION, indexForViewer);
        intent.putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, sortOrderManagement.getOrderCamera());

        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle());

        MegaNode parentNode = megaApi.getParentNode(node);
        if (parentNode == null || parentNode.getType() == MegaNode.TYPE_ROOT) {
            intent.putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE);
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentNode.getHandle());
        }

        if (mViewModel.isSearchMode()) {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_BY_ADAPTER);
            intent.putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                    mViewModel.getSearchResultNodeHandles());
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTO_SYNC_ADAPTER);
        }

        putThumbnailLocation(intent, mBinding.cuList, position, VIEWER_FROM_CUMU, mAdapter);
    }

    private void launchNodeViewer(Intent intent) {
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
}
