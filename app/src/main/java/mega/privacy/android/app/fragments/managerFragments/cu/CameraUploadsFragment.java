package mega.privacy.android.app.fragments.managerFragments.cu;

import android.app.Activity;
import android.app.AlertDialog;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Locale;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.databinding.FragmentCameraUploadsBinding;
import mega.privacy.android.app.databinding.FragmentCameraUploadsFirstLoginBinding;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.DraggingThumbnailCallback;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.BUSINESS_CU_FRAGMENT_CU;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetCUTimestampsAndCache;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SCREEN_POSITION;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_GRID;
import static mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME;
import static mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
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
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CameraUploadsFragment extends BaseFragment implements CameraUploadsAdapter.Listener {
    public static final int TYPE_CAMERA = MegaNodeRepo.CU_TYPE_CAMERA;
    public static final int TYPE_MEDIA = MegaNodeRepo.CU_TYPE_MEDIA;

    private static final String ARG_TYPE = "type";

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
    private long mDraggingNodeHandle = INVALID_HANDLE;

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

    public void setSearchDate(long[] searchDate, int orderBy) {
        mViewModel.setSearchDate(searchDate, orderBy);
    }

    public void reloadNodes(int orderBy) {
        mViewModel.loadCuNodes(orderBy);
    }

    public void checkScroll() {
        if (mViewModel == null || mBinding == null) {
            return;
        }

        if (mViewModel.isSelecting() || mBinding.cuList.canScrollVertically(-1)) {
            mManagerActivity.changeActionBarElevation(true);
        } else {
            mManagerActivity.changeActionBarElevation(false);
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
            reloadNodes(mManagerActivity.orderCamera);
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

    public void scrollToNode(long handle) {
        logDebug("scrollToNode, handle " + handle);
        if (mBinding != null) {
            int position = mAdapter.getNodePosition(handle);
            logDebug("scrollToNode, handle " + handle + ", position " + position);
            if (position != INVALID_POSITION) {
                mBinding.cuList.scrollToPosition(position);
                notifyThumbnailLocationOnScreen();
            }
        }
    }

    public void hideDraggingThumbnail(long handle) {
        logDebug("hideDraggingThumbnail: " + handle);

        if (mViewModel != null) {
            setDraggingThumbnailVisibility(mDraggingNodeHandle, View.VISIBLE);
            setDraggingThumbnailVisibility(handle, View.GONE);
            mDraggingNodeHandle = handle;
            notifyThumbnailLocationOnScreen();
        }
    }

    private void setDraggingThumbnailVisibility(long handle, int visibility) {
        int position = mAdapter.getNodePosition(handle);
        RecyclerView.ViewHolder viewHolder =
                mBinding.cuList.findViewHolderForLayoutPosition(position);
        if (viewHolder == null) {
            return;
        }
        mAdapter.setThumbnailVisibility(viewHolder, visibility);
    }

    private void notifyThumbnailLocationOnScreen() {
        int position = mAdapter.getNodePosition(mDraggingNodeHandle);
        RecyclerView.ViewHolder viewHolder =
                mBinding.cuList.findViewHolderForLayoutPosition(position);
        if (viewHolder == null) {
            return;
        }
        int[] res = mAdapter.getThumbnailLocationOnScreen(viewHolder);
        res[0] += res[2] / 2;
        res[1] += res[3] / 2;
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG);
        intent.putExtra("screenPosition", res);
        context.sendBroadcast(intent);
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
            new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
                    .setTitle(getString(R.string.section_photo_sync))
                    .setSingleChoiceItems(adapter, -1, (dialog, which) -> {
                        resetCUTimestampsAndCache();
                        dbH.setCamSyncEnabled(true);
                        dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
                        File localFile =
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DCIM);
                        String localPath = localFile.getAbsolutePath();
                        dbH.setCamSyncLocalPath(localPath);
                        dbH.setCameraFolderExternalSDCard(false);

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
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        CuViewModelFactory viewModelFactory =
                new CuViewModelFactory(megaApi, DatabaseHandler.getDbHandler(context),
                        new MegaNodeRepo(context, megaApi, dbH), context, mCamera);
        mViewModel = new ViewModelProvider(this, viewModelFactory).get(CuViewModel.class);

        if (mCamera == TYPE_CAMERA && mManagerActivity.getFirstLogin()) {
            return createCameraUploadsViewForFirstLogin(inflater, container);
        } else {
            mBinding = FragmentCameraUploadsBinding.inflate(inflater, container, false);
            return mBinding.getRoot();
        }
    }

    private View createCameraUploadsViewForFirstLogin(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container) {
        mManagerActivity.showHideBottomNavigationView(true);
        mViewModel.setInitialPreferences();

        mFirstLoginBinding =
                FragmentCameraUploadsFirstLoginBinding.inflate(inflater, container, false);

        new ListenScrollChangesHelper().addViewToListen(mFirstLoginBinding.camSyncScrollView,
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (mFirstLoginBinding.camSyncScrollView.canScrollVertically(-1)) {
                        mManagerActivity.changeActionBarElevation(true);
                    } else {
                        mManagerActivity.changeActionBarElevation(false);
                    }
                });

        mFirstLoginBinding.camSyncButtonOk.setOnClickListener(v -> {
            ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
            String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
            if (hasPermissions(context, permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert(
                        BUSINESS_CU_FRAGMENT_CU, true);
            } else {
                requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
            }
            mManagerActivity.showHideBottomNavigationView(false);
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
        setDraggingThumbnailCallback();
    }

    private void setDraggingThumbnailCallback() {
        FullScreenImageViewerLollipop.addDraggingThumbnailCallback(CameraUploadsFragment.class,
                new CuDraggingThumbnailCallback(this));
        AudioVideoPlayerLollipop.addDraggingThumbnailCallback(CameraUploadsFragment.class,
                new CuDraggingThumbnailCallback(this));
    }

    @Override public void onDestroy() {
        super.onDestroy();

        FullScreenImageViewerLollipop.removeDraggingThumbnailCallback(CameraUploadsFragment.class);
        AudioVideoPlayerLollipop.removeDraggingThumbnailCallback(CameraUploadsFragment.class);
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
            mBinding.emptyHintImage.setImageResource(R.drawable.uploads_empty_landscape);
        } else {
            mBinding.emptyHintImage.setImageResource(R.drawable.ic_empty_camera_uploads);
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
            if (mDraggingNodeHandle != INVALID_HANDLE && !isResumed()) {
                // don't update UI while dragging FullscreenImageViewer/AudioVideoPlayer,
                // to not cause the hidden thumbnail be shown.
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
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
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

        mDraggingNodeHandle = INVALID_HANDLE;
        reloadNodes(mManagerActivity.orderCamera);
    }

    private void openNode(int position, CuNode cuNode) {
        if (position < 0 || position >= mAdapter.getItemCount()) {
            return;
        }

        int[] thumbnailLocation = mAdapter.getThumbnailLocationOnScreen(
                mBinding.cuList.findViewHolderForLayoutPosition(position));
        if (thumbnailLocation == null) {
            return;
        }

        MegaNode node = cuNode.getNode();
        if (node == null) {
            return;
        }

        MimeTypeThumbnail mime = MimeTypeThumbnail.typeForName(node.getName());
        if (mime.isImage()) {
            Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
            putExtras(intent, cuNode.getIndexForViewer(), node, thumbnailLocation);
            setDraggingThumbnailCallback();
            launchNodeViewer(intent, node.getHandle());
        } else if (mime.isVideoReproducible()) {
            Intent mediaIntent;
            boolean internalIntent;
            if (mime.isVideoNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
            } else {
                internalIntent = true;
                mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
            }

            putExtras(mediaIntent, cuNode.getIndexForViewer(), node, thumbnailLocation);

            mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle());
            mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());

            boolean paramsSetSuccessfully = false;
            String localPath = null;
            try {
                localPath = findVideoLocalPath(context, node);
            } catch (Exception e) {
                logWarning(e.getMessage());
            }
            if (localPath != null && checkFingerprint(megaApi, node, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(context, node, mediaIntent, localPath,
                        false);
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(context, node, megaApi,
                        mediaIntent);
            }
            if (!isIntentAvailable(context, mediaIntent)) {
                mManagerActivity.showSnackbar(SNACKBAR_TYPE,
                        getString(R.string.intent_not_available), MEGACHAT_INVALID_HANDLE);
                paramsSetSuccessfully = false;
            }
            if (paramsSetSuccessfully) {
                if (internalIntent) {
                    setDraggingThumbnailCallback();
                }
                launchNodeViewer(mediaIntent, node.getHandle());
            }
        }
    }

    private void putExtras(Intent intent, int indexForViewer, MegaNode node,
            int[] thumbnailLocation) {
        intent.putExtra(INTENT_EXTRA_KEY_POSITION, indexForViewer);
        intent.putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, mManagerActivity.orderCamera);

        MegaNode parentNode = megaApi.getParentNode(node);
        if (parentNode == null || parentNode.getType() == MegaNode.TYPE_ROOT) {
            intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE);
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, parentNode.getHandle());
        }

        if (mViewModel.isSearchMode()) {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_BY_ADAPTER);
            intent.putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                    mViewModel.getSearchResultNodeHandles());
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTO_SYNC_ADAPTER);
        }

        logDebug("openNode screenPosition " + Arrays.toString(thumbnailLocation));
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, thumbnailLocation);
    }

    private void launchNodeViewer(Intent intent, long handle) {
        mDraggingNodeHandle = handle;
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

    private static class CuDraggingThumbnailCallback implements DraggingThumbnailCallback {
        private final WeakReference<CameraUploadsFragment> mFragment;

        private CuDraggingThumbnailCallback(CameraUploadsFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override public void setVisibility(int visibility) {
            CameraUploadsFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.setDraggingThumbnailVisibility(fragment.mDraggingNodeHandle, visibility);
            }
        }

        @Override public void getLocationOnScreen(int[] location) {
            CameraUploadsFragment fragment = mFragment.get();
            if (fragment != null) {
                int position = fragment.mAdapter.getNodePosition(fragment.mDraggingNodeHandle);
                RecyclerView.ViewHolder viewHolder =
                        fragment.mBinding.cuList.findViewHolderForLayoutPosition(position);
                if (viewHolder == null) {
                    return;
                }
                int[] res = fragment.mAdapter.getThumbnailLocationOnScreen(viewHolder);
                System.arraycopy(res, 0, location, 0, 2);
            }
        }
    }
}
