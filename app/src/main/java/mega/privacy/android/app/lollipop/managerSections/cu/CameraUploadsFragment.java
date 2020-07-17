package mega.privacy.android.app.lollipop.managerSections.cu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
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
import mega.privacy.android.app.lollipop.adapters.CameraUploadsAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.BUSINESS_CU_FRAGMENT_CU;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetCUTimestampsAndCache;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG;
import static mega.privacy.android.app.utils.Constants.BUFFER_COMP;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_GRID;
import static mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME;
import static mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.FileUtils.findVideoLocalPath;
import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;
import static mega.privacy.android.app.utils.JobUtil.stopRunningCameraUploadService;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.Util.checkFingerprint;
import static mega.privacy.android.app.utils.Util.px2dp;
import static mega.privacy.android.app.utils.Util.showSnackbar;

public class CameraUploadsFragment extends BaseFragment implements CameraUploadsAdapter.Listener {
  public static final int TYPE_CAMERA = 0;
  public static final int TYPE_MEDIA = 1;

  private static final String ARG_TYPE = "type";

  private static final int SPAN_LARGE_GRID = 3;
  private static final int SPAN_SMALL_GRID = 7;
  private static final int MARGIN_LARGE_GRID = 6;
  private static final int MARGIN_SMALL_GRID = 3;

  @SuppressLint("StaticFieldLeak") private static CameraUploadsFragment sInstanceForDragging;

  private int type = TYPE_CAMERA;

  private FragmentCameraUploadsFirstLoginBinding firstLoginBinding;
  private FragmentCameraUploadsBinding binding;
  private CameraUploadsAdapter adapter;

  private CuViewModel viewModel;
  private long draggingNodeHandle = -1;

  public static CameraUploadsFragment newInstance(int type) {
    CameraUploadsFragment fragment = new CameraUploadsFragment();

    Bundle args = new Bundle();
    args.putInt(ARG_TYPE, type);
    fragment.setArguments(args);

    return fragment;
  }

  /**
   * Get the location on screen of the dragging node thumbnail.
   *
   * TODO: we need figure out a better way to implement the drag feature, this kind of static hack
   * should be avoided.
   *
   * @param location out param for location
   */
  public static void getDraggingThumbnailLocationOnScreen(int[] location) {
    CameraUploadsFragment fragment = sInstanceForDragging;
    if (fragment == null) {
      return;
    }
    int position = fragment.adapter.getNodePosition(fragment.draggingNodeHandle);
    RecyclerView.ViewHolder viewHolder =
        fragment.binding.cuList.findViewHolderForLayoutPosition(position);
    if (viewHolder == null) {
      return;
    }
    int[] res = fragment.adapter.getThumbnailLocationOnScreen(viewHolder);
    System.arraycopy(res, 0, location, 0, 2);
  }

  public static void setDraggingThumbnailVisibility(int visibility) {
    CameraUploadsFragment fragment = sInstanceForDragging;
    if (fragment == null) {
      return;
    }
    fragment.setDraggingThumbnailVisibility(fragment.draggingNodeHandle, visibility);
  }

  public int getItemCount() {
    return adapter.getItemCount();
  }

  public void setOrderBy(int orderBy) {
    reloadNodes(orderBy);
  }

  public void setSearchDate(long[] searchDate, int orderBy) {
    viewModel.setSearchDate(searchDate, orderBy);
  }

  public void reloadNodes(int orderBy) {
    viewModel.loadCuNodes(orderBy);
  }

  public void checkScroll() {
    if (viewModel.isSelecting() && binding.cuList.canScrollVertically(-1)) {
      ((ManagerActivityLollipop) context).changeActionBarElevation(true);
    } else {
      ((ManagerActivityLollipop) context).changeActionBarElevation(false);
    }
  }

  public void selectAll() {
    viewModel.selectAll();
  }

  public int onBackPressed() {
    if (((ManagerActivityLollipop) context).getFirstLogin()) {
      viewModel.setCamSyncEnabled(false);
      ((ManagerActivityLollipop) context).setFirstLogin(false);
      ((ManagerActivityLollipop) context).refreshMenu();
    }

    if (((ManagerActivityLollipop) context).isFirstNavigationLevel()) {
      return 0;
    } else {
      reloadNodes(((ManagerActivityLollipop) context).orderCamera);
      ((ManagerActivityLollipop) context).invalidateOptionsMenu();
      ((ManagerActivityLollipop) context).setIsSearchEnabled(false);
      ((ManagerActivityLollipop) context).setToolbarTitle();
      return 1;
    }
  }

  public void onStoragePermissionRefused() {
    showSnackbar(context, getString(R.string.on_refuse_storage_permission));
    toCloudDrive();
  }

  public void scrollToNode(long handle) {
    logDebug("scrollToNode, handle " + handle);
    if (binding != null) {
      int position = adapter.getNodePosition(handle);
      logDebug("scrollToNode, handle " + handle + ", position " + position);
      if (position != -1) {
        binding.cuList.scrollToPosition(position);
        notifyThumbnailLocationOnScreen();
      }
    }
  }

  public void hideDraggingThumbnail(long handle) {
    logDebug("hideDraggingThumbnail: " + handle);

    if (viewModel != null) {
      setDraggingThumbnailVisibility(draggingNodeHandle, View.VISIBLE);
      setDraggingThumbnailVisibility(handle, View.GONE);
      draggingNodeHandle = handle;
      notifyThumbnailLocationOnScreen();
    }
  }

  private void setDraggingThumbnailVisibility(long handle, int visibility) {
    int position = adapter.getNodePosition(handle);
    RecyclerView.ViewHolder viewHolder = binding.cuList.findViewHolderForLayoutPosition(position);
    if (viewHolder == null) {
      return;
    }
    adapter.setThumbnailVisibility(viewHolder, visibility);
  }

  private void notifyThumbnailLocationOnScreen() {
    int position = adapter.getNodePosition(draggingNodeHandle);
    RecyclerView.ViewHolder viewHolder = binding.cuList.findViewHolderForLayoutPosition(position);
    if (viewHolder == null) {
      return;
    }
    int[] res = adapter.getThumbnailLocationOnScreen(viewHolder);
    res[0] += res[2] / 2;
    res[1] += res[3] / 2;
    Intent intent =  new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG);
    intent.putExtra("screenPosition", res);
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
  }

  private void toCloudDrive() {
    viewModel.setCamSyncEnabled(false);
    ((ManagerActivityLollipop) context).setFirstLogin(false);
    ((ManagerActivityLollipop) context).setInitialCloudDrive();
  }

  private void requestCameraUploadPermission(String[] permissions, int requestCode) {
    ActivityCompat.requestPermissions((ManagerActivityLollipop) context, permissions, requestCode);
  }

  public void enableCuForBusinessFirstTime() {
    if (firstLoginBinding == null) {
      return;
    }

    boolean enableCellularSync = firstLoginBinding.cellularConnectionSwitch.isChecked();
    boolean syncVideo = firstLoginBinding.uploadVideosSwitch.isChecked();

    viewModel.enableCuForBusinessFirstTime(enableCellularSync, syncVideo);

    ((ManagerActivityLollipop) context).setFirstLogin(false);
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
      ((ManagerActivityLollipop) context).refreshCameraUpload();
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
          new ArrayAdapter<>(context, R.layout.select_dialog_singlechoice, android.R.id.text1,
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
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
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
    ((ManagerActivityLollipop) context).refreshCameraUpload();

    new Handler().postDelayed(() -> {
      logDebug("Starting CU");
      startCameraUploadService(context);
    }, 1000);
  }

  public void resetSwitchButtonLabel() {
    if (binding == null) {
      return;
    }

    binding.turnOnCuLayout.setVisibility(View.VISIBLE);
    binding.turnOnCuText.setText(
        getString(R.string.settings_camera_upload_turn_on).toUpperCase(Locale.getDefault()));
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    if (args != null) {
      type = getArguments().getInt(ARG_TYPE, TYPE_CAMERA);
    }
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    CuViewModelFactory viewModelFactory =
        new CuViewModelFactory(megaApi, DatabaseHandler.getDbHandler(context), type);
    viewModel = new ViewModelProvider(this, viewModelFactory).get(CuViewModel.class);

    if (type == TYPE_CAMERA && ((ManagerActivityLollipop) context).getFirstLogin()) {
      ((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
      viewModel.setInitialPreferences();

      firstLoginBinding =
          FragmentCameraUploadsFirstLoginBinding.inflate(inflater, container, false);

      new ListenScrollChangesHelper().addViewToListen(firstLoginBinding.camSyncScrollView,
          (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (firstLoginBinding.camSyncScrollView.canScrollVertically(-1)) {
              ((ManagerActivityLollipop) context).changeActionBarElevation(true);
            } else {
              ((ManagerActivityLollipop) context).changeActionBarElevation(false);
            }
          });

      firstLoginBinding.camSyncButtonOk.setOnClickListener(v -> {
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
        String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
        if (hasPermissions(context, permissions)) {
          ((ManagerActivityLollipop) context).checkIfShouldShowBusinessCUAlert(
              BUSINESS_CU_FRAGMENT_CU, true);
        } else {
          requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
        }
        ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
      });
      firstLoginBinding.camSyncButtonSkip.setOnClickListener(v -> {
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
        toCloudDrive();
      });

      return firstLoginBinding.getRoot();
    } else {
      binding = FragmentCameraUploadsBinding.inflate(inflater, container, false);
      return binding.getRoot();
    }
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (binding == null) {
      return;
    }

    boolean smallGrid = ((ManagerActivityLollipop) context).isSmallGridCameraUploads;
    int spanCount = smallGrid ? SPAN_SMALL_GRID : SPAN_LARGE_GRID;

    binding.cuList.setHasFixedSize(true);
    GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
    binding.cuList.setLayoutManager(layoutManager);
    binding.cuList.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        checkScroll();
      }
    });

    int gridMargin = smallGrid ? MARGIN_SMALL_GRID : MARGIN_LARGE_GRID;
    int gridWidth = outMetrics.widthPixels / spanCount - gridMargin * 2;
    int icSelectedWidth = px2dp(smallGrid ? 16 : 23, outMetrics);
    int icSelectedMargin = px2dp(smallGrid ? 3 : 7, outMetrics);
    int roundCornerRadius = px2dp(4, outMetrics);
    int selectedPadding = px2dp(1, outMetrics);
    CameraUploadsAdapter.ItemSizeConfig itemSizeConfig = new CameraUploadsAdapter.ItemSizeConfig(
        smallGrid, gridWidth, gridMargin, icSelectedWidth, icSelectedMargin, roundCornerRadius,
        selectedPadding);

    adapter = new CameraUploadsAdapter(this, spanCount, itemSizeConfig);
    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override public int getSpanSize(int position) {
        return adapter.getSpanSize(position);
      }
    });

    binding.cuList.setAdapter(adapter);
    binding.scroller.setRecyclerView(binding.cuList);

    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      binding.emptyHintImage.setImageResource(R.drawable.uploads_empty_landscape);
    } else {
      binding.emptyHintImage.setImageResource(R.drawable.ic_empty_camera_uploads);
    }

    viewModel.cuNodes().observe(getViewLifecycleOwner(), nodes -> {
      boolean showScroller =
          nodes.size() >= (smallGrid ? MIN_ITEMS_SCROLLBAR_GRID : MIN_ITEMS_SCROLLBAR);
      binding.scroller.setVisibility(showScroller ? View.VISIBLE : View.GONE);
      adapter.setNodes(nodes);
      ((ManagerActivityLollipop) context).updateCuFragmentOptionsMenu();

      binding.emptyHint.setVisibility(nodes.isEmpty() ? View.VISIBLE : View.GONE);
      if (nodes.isEmpty()) {
        binding.cuList.setVisibility(View.GONE);
        binding.scroller.setVisibility(View.GONE);

        binding.emptyHintImage.setVisibility(viewModel.isSearchMode() ? View.GONE : View.VISIBLE);
        if (viewModel.isSearchMode()) {
          binding.emptyHintText.setText(R.string.no_results_found);
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
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
          } else {
            result = Html.fromHtml(textToShow);
          }
          binding.emptyHintText.setText(result);
        }
      }
    });

    viewModel.nodeToOpen()
        .observe(getViewLifecycleOwner(), pair -> openNode(pair.first, pair.second));

    viewModel.nodeToAnimate().observe(getViewLifecycleOwner(), pair -> {
      if (pair.first < 0 || pair.first >= adapter.getItemCount()) {
        return;
      }

      adapter.showSelectionAnimation(pair.first, pair.second,
          binding.cuList.findViewHolderForLayoutPosition(pair.first));
    });

    viewModel.actionBarTitle().observe(getViewLifecycleOwner(), title -> {
      ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
      if (actionBar != null && viewModel.isSearchMode()) {
        actionBar.setTitle(title);
      }
    });

    if (type == TYPE_CAMERA) {
      binding.turnOnCuText.setText(
          getString(R.string.settings_camera_upload_turn_on).toUpperCase(Locale.getDefault()));
    } else {
      binding.turnOnCuText.setText(
          getString(R.string.settings_set_up_automatic_uploads).toUpperCase(Locale.getDefault()));
    }

    binding.turnOnCuLayout.setOnClickListener(v -> {
      ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();
      String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };

      if (type == TYPE_CAMERA) {
        if (hasPermissions(context, permissions)) {
          ((ManagerActivityLollipop) context).checkIfShouldShowBusinessCUAlert(
              BUSINESS_CU_FRAGMENT_CU, false);
        } else {
          requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF);
        }
      } else {
        ((ManagerActivityLollipop) context).moveToSettingsSection();
      }
    });

    viewModel.camSyncEnabled()
        .observe(getViewLifecycleOwner(),
            enabled -> binding.turnOnCuLayout.setVisibility(enabled ? View.GONE : View.VISIBLE));
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case REQUEST_CAMERA_ON_OFF: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          ((ManagerActivityLollipop) context).checkIfShouldShowBusinessCUAlert(
              BUSINESS_CU_FRAGMENT_CU, false);
        }
        break;
      }
      case REQUEST_CAMERA_ON_OFF_FIRST_TIME: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          ((ManagerActivityLollipop) context).checkIfShouldShowBusinessCUAlert(
              BUSINESS_CU_FRAGMENT_CU, true);
        }
        break;
      }
    }
  }

  @Override public void onResume() {
    super.onResume();

    reloadNodes(((ManagerActivityLollipop) context).orderCamera);
    sInstanceForDragging = null;
  }

  private void openNode(int position, CuNode cuNode) {
    if (position < 0 || position >= adapter.getItemCount()) {
      return;
    }

    int[] thumbnailLocation = adapter.getThumbnailLocationOnScreen(
        binding.cuList.findViewHolderForLayoutPosition(position));
    if (thumbnailLocation == null) {
      return;
    }

    MegaNode node = cuNode.getNode();
    if (node == null) {
      return;
    }

    if (MimeTypeThumbnail.typeForName(node.getName()).isImage()) {
      Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
      putExtras(intent, cuNode.getIndex(), node, thumbnailLocation);
      launchNodeViewer(intent, node.getHandle());
    } else if (MimeTypeThumbnail.typeForName(node.getName()).isVideoReproducible()) {
      String mimeType = MimeTypeThumbnail.typeForName(node.getName()).getType();

      Intent mediaIntent;
      boolean internalIntent;
      if (MimeTypeThumbnail.typeForName(node.getName()).isVideoNotSupported()) {
        mediaIntent = new Intent(Intent.ACTION_VIEW);
        internalIntent = false;
      } else {
        internalIntent = true;
        mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
      }

      putExtras(mediaIntent, cuNode.getIndex(), node, thumbnailLocation);

      mediaIntent.putExtra("HANDLE", node.getHandle());
      mediaIntent.putExtra("FILENAME", node.getName());

      String localPath = null;
      try {
        localPath = findVideoLocalPath(context, node);
      } catch (Exception e) {
        logWarning(e.getMessage());
      }
      if (localPath != null && checkFingerprint(megaApi, node, localPath)) {
        File mediaFile = new File(localPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(
            Environment.getExternalStorageDirectory().getPath())) {
          mediaIntent.setDataAndType(FileProvider.getUriForFile(context,
              "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList
              .typeForName(node.getName()).getType());
        } else {
          mediaIntent.setDataAndType(Uri.fromFile(mediaFile),
              MimeTypeThumbnail.typeForName(node.getName()).getType());
        }
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } else {
        if (megaApi.httpServerIsRunning() == 0) {
          megaApi.httpServerStart();
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager
            = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
          activityManager.getMemoryInfo(mi);
        }

        if (mi.totalMem > BUFFER_COMP) {
          megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
        } else {
          megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
        }

        String url = megaApi.httpServerGetLocalLink(node);
        mediaIntent.setDataAndType(Uri.parse(url), mimeType);
      }
      if (internalIntent) {
        launchNodeViewer(mediaIntent, node.getHandle());
      } else {
        if (isIntentAvailable(context, mediaIntent)) {
          launchNodeViewer(mediaIntent, node.getHandle());
        } else {
          ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE,
              getString(R.string.intent_not_available), -1);
        }
      }
    } else {
      ArrayList<Long> handleList = new ArrayList<>();
      handleList.add(node.getHandle());
      NodeController nC = new NodeController(context);
      nC.prepareForDownload(handleList, true);
    }
  }

  private void putExtras(Intent intent, int index, MegaNode node, int[] thumbnailLocation) {
    intent.putExtra("position", index);
    intent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderCamera);

    MegaNode parentNode = megaApi.getParentNode(node);
    if (parentNode == null || parentNode.getType() == MegaNode.TYPE_ROOT) {
      intent.putExtra("parentNodeHandle", -1);
    } else {
      intent.putExtra("parentNodeHandle", parentNode.getHandle());
    }

    if (viewModel.isSearchMode()) {
      intent.putExtra("adapterType", SEARCH_BY_ADAPTER);
      intent.putExtra("handlesNodesSearch", viewModel.getSearchResultNodeHandles());
    } else {
      intent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);
    }

    logDebug("openNode screenPosition " + Arrays.toString(thumbnailLocation));
    intent.putExtra("screenPosition", thumbnailLocation);
  }

  private void launchNodeViewer(Intent intent, long handle) {
    context.startActivity(intent);
    requireActivity().overridePendingTransition(0, 0);
    sInstanceForDragging = this;
    draggingNodeHandle = handle;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override public void onNodeClicked(int position, CuNode node) {
    viewModel.onNodeClicked(position, node);
  }

  @Override public void onNodeLongClicked(int position, CuNode node) {
    viewModel.onNodeLongClicked(position, node);
  }
}
