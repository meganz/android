package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import de.palm.composestateevents.StateEvent
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.SettingsConstants.SELECTED_MEGA_FOLDER
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.presentation.settings.camerauploads.business.BusinessAccountPromptHandler
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.FileUploadDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HowToUploadDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VideoCompressionSizeInputDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VideoQualityDialog
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.navigation.pickers.openCameraUploadsFolderNodePicker
import mega.privacy.android.app.presentation.settings.camerauploads.navigation.pickers.openCameraUploadsLocalFolderPicker
import mega.privacy.android.app.presentation.settings.camerauploads.permissions.CameraUploadsPermissionsHandler
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsFolderNodeTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsLocalFolderTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FileUploadTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HowToUploadTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.IncludeLocationTagsTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KeepFileNamesTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MediaUploadsTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.RequireChargingDuringVideoCompressionTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VideoCompressionTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VideoQualityTile
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A Composable that holds views displaying the main Settings Camera Uploads screen
 *
 * @param uiState The Settings Camera Uploads UI State
 * @param onBusinessAccountPromptDismissed Lambda to execute when the User dismisses the Business
 * Account prompt
 * @param onCameraUploadsProcessStarted Lambda to execute when starting the Camera Uploads process
 * @param onCameraUploadsStateChanged Lambda to execute when the Camera Uploads state changes
 * @param onChargingDuringVideoCompressionStateChanged Lambda to execute when the Device charging
 * state has changed when compressing Videos
 * @param onHowToUploadPromptOptionSelected Lambda to execute when the User selects a new
 * @param onIncludeLocationTagsStateChanged Lambda to execute when the Include Location Tags state
 * changes
 * [UploadConnectionType] from the How to Upload prompt
 * @param onKeepFileNamesStateChanged Lambda to execute when the Keep File Names state changes
 * @param onLocalPrimaryFolderSelected Lambda to execute when selecting the new Camera Uploads
 * Local Primary Folder
 * @param onMediaPermissionsGranted Lambda to execute when the User has granted the Media Permissions
 * @param onMediaUploadsStateChanged Lambda to execute when the Media Uploads state changes
 * @param onNewVideoCompressionSizeLimitProvided Lambda to execute upon providing a new maximum
 * aggregate Video Size that can be compressed without having to charge the Device
 * @param onPrimaryFolderNodeSelected Lambda to execute when selecting the new Camera Uploads
 * Primary Folder Node
 * @param onRegularBusinessAccountSubUserPromptAcknowledged Lambda to execute when the Business
 * Account Sub-User acknowledges that the Business Account Administrator can access the content
 * in Camera Uploads
 * @param onRequestPermissionsStateChanged Lambda to execute whether a Camera Uploads permissions
 * request should be done (triggered) or not (consumed)
 * @param onUploadOptionUiItemSelected Lambda to execute when the User selects a new
 * [UploadOptionUiItem] from the File Upload prompt
 * @param onVideoQualityUiItemSelected Lambda to execute when the User selects a new
 * [VideoQualityUiItem] from the Video Quality prompt
 */
@Composable
internal fun SettingsCameraUploadsView(
    uiState: SettingsCameraUploadsUiState,
    onBusinessAccountPromptDismissed: () -> Unit,
    onCameraUploadsProcessStarted: () -> Unit,
    onCameraUploadsStateChanged: (Boolean) -> Unit,
    onChargingDuringVideoCompressionStateChanged: (Boolean) -> Unit,
    onHowToUploadPromptOptionSelected: (UploadConnectionType) -> Unit,
    onIncludeLocationTagsStateChanged: (Boolean) -> Unit,
    onKeepFileNamesStateChanged: (Boolean) -> Unit,
    onLocalPrimaryFolderSelected: (String?) -> Unit,
    onMediaPermissionsGranted: () -> Unit,
    onMediaUploadsStateChanged: (Boolean) -> Unit,
    onNewVideoCompressionSizeLimitProvided: (Int) -> Unit,
    onPrimaryFolderNodeSelected: (NodeId) -> Unit,
    onRegularBusinessAccountSubUserPromptAcknowledged: () -> Unit,
    onRequestPermissionsStateChanged: (StateEvent) -> Unit,
    onUploadOptionUiItemSelected: (UploadOptionUiItem) -> Unit,
    onVideoQualityUiItemSelected: (VideoQualityUiItem) -> Unit,
) {
    val context = LocalContext.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var showFileUploadPrompt by rememberSaveable { mutableStateOf(false) }
    var showHowToUploadPrompt by rememberSaveable { mutableStateOf(false) }
    var showVideoCompressionSizeInputPrompt by rememberSaveable { mutableStateOf(false) }
    var showVideoQualityPrompt by rememberSaveable { mutableStateOf(false) }

    /**
     * Flag that controls the start of Camera Uploads when the onPause() Lifecycle Event is triggered
     * (e.g. User leaves the Screen)
     *
     * This is false when the User attempts to change the Camera / Media Uploads Folder, be it a
     * Local Folder or Folder Node
     */
    var canStartCameraUploads by rememberSaveable {
        mutableStateOf(true)
    }

    val cameraUploadsLocalFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val primaryFolderLocalPath = it.data?.getStringExtra(FileStorageActivity.EXTRA_PATH)
        onLocalPrimaryFolderSelected.invoke(primaryFolderLocalPath)
    }
    val cameraUploadsFolderNodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val primaryFolderNodeId = NodeId(it.data?.getLongExtra(SELECTED_MEGA_FOLDER, -1L) ?: -1L)
        onPrimaryFolderNodeSelected.invoke(primaryFolderNodeId)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Check if Camera Uploads can be started
                if (canStartCameraUploads) {
                    onCameraUploadsProcessStarted.invoke()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // Re-enable the flag that starts the Camera Uploads process when leaving the screen
                canStartCameraUploads = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MegaScaffold(
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(SETTINGS_CAMERA_UPLOADS_TOOLBAR),
                title = stringResource(R.string.section_photo_sync),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
            )
        },
        content = { padding ->
            CameraUploadsPermissionsHandler(
                modifier = Modifier.padding(padding),
                requestPermissions = uiState.requestPermissions,
                onMediaPermissionsGranted = onMediaPermissionsGranted,
                onRequestPermissionsStateChanged = onRequestPermissionsStateChanged,
            )
            BusinessAccountPromptHandler(
                businessAccountPromptType = uiState.businessAccountPromptType,
                onRegularBusinessAccountSubUserPromptAcknowledged = onRegularBusinessAccountSubUserPromptAcknowledged,
                onBusinessAccountPromptDismissed = onBusinessAccountPromptDismissed,
            )
            if (showVideoQualityPrompt) {
                VideoQualityDialog(
                    currentVideoQualityUiItem = uiState.videoQualityUiItem,
                    onOptionSelected = { newVideoQualityUiItem ->
                        showVideoQualityPrompt = false
                        onVideoQualityUiItemSelected.invoke(newVideoQualityUiItem)
                    },
                    onDismissRequest = { showVideoQualityPrompt = false },
                )
            }
            if (showFileUploadPrompt) {
                FileUploadDialog(
                    currentUploadOptionUiItem = uiState.uploadOptionUiItem,
                    onOptionSelected = { newUploadOptionUiItem ->
                        showFileUploadPrompt = false
                        onUploadOptionUiItemSelected.invoke(newUploadOptionUiItem)
                    },
                    onDismissRequest = { showFileUploadPrompt = false },
                )
            }
            if (showHowToUploadPrompt) {
                HowToUploadDialog(
                    currentUploadConnectionType = uiState.uploadConnectionType,
                    onOptionSelected = { newUploadConnectionType ->
                        showHowToUploadPrompt = false
                        onHowToUploadPromptOptionSelected.invoke(newUploadConnectionType)
                    },
                    onDismissRequest = { showHowToUploadPrompt = false },
                )
            }
            if (showVideoCompressionSizeInputPrompt) {
                VideoCompressionSizeInputDialog(
                    onNewSizeProvided = { newVideoCompressionSize ->
                        showVideoCompressionSizeInputPrompt = false
                        onNewVideoCompressionSizeLimitProvided.invoke(newVideoCompressionSize)
                    },
                    onDismiss = { showVideoCompressionSizeInputPrompt = false },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
            ) {
                CameraUploadsTile(
                    isChecked = uiState.isCameraUploadsEnabled,
                    onCheckedChange = onCameraUploadsStateChanged,
                )
                if (uiState.isCameraUploadsEnabled) {
                    HowToUploadTile(
                        uploadConnectionType = uiState.uploadConnectionType,
                        onItemClicked = { showHowToUploadPrompt = true },
                    )
                    FileUploadTile(
                        uploadOptionUiItem = uiState.uploadOptionUiItem,
                        onItemClicked = { showFileUploadPrompt = true },
                    )
                    if (uiState.canChangeLocationTagsState) {
                        IncludeLocationTagsTile(
                            isChecked = uiState.shouldIncludeLocationTags,
                            onCheckedChange = onIncludeLocationTagsStateChanged,
                        )
                    }
                    if (uiState.canChangeVideoQuality) {
                        VideoQualityTile(
                            videoQualityUiItem = uiState.videoQualityUiItem,
                            onItemClicked = { showVideoQualityPrompt = true },
                        )
                    }
                    if (uiState.canChangeChargingDuringVideoCompressionState) {
                        RequireChargingDuringVideoCompressionTile(
                            maximumNonChargingVideoCompressionSize = uiState.maximumNonChargingVideoCompressionSize,
                            isChecked = uiState.requireChargingDuringVideoCompression,
                            onCheckedChange = onChargingDuringVideoCompressionStateChanged,
                        )
                        if (uiState.requireChargingDuringVideoCompression) {
                            VideoCompressionTile(
                                maximumNonChargingVideoCompressionSize = uiState.maximumNonChargingVideoCompressionSize,
                                onItemClicked = { showVideoCompressionSizeInputPrompt = true },
                            )
                        }
                    }
                    KeepFileNamesTile(
                        isChecked = uiState.shouldKeepUploadFileNames,
                        onCheckedChange = onKeepFileNamesStateChanged,
                    )
                    CameraUploadsLocalFolderTile(
                        primaryFolderPath = uiState.primaryFolderPath,
                        onItemClicked = {
                            canStartCameraUploads = false
                            openCameraUploadsLocalFolderPicker(
                                context = context,
                                launcher = cameraUploadsLocalFolderLauncher,
                            )
                        },
                    )
                    CameraUploadsFolderNodeTile(
                        primaryFolderName = uiState.primaryFolderName.takeIf { !it.isNullOrBlank() }
                            ?: stringResource(R.string.section_photo_sync),
                        onItemClicked = {
                            canStartCameraUploads = false
                            openCameraUploadsFolderNodePicker(
                                context = context,
                                launcher = cameraUploadsFolderNodeLauncher,
                            )
                        },
                    )
                    MediaUploadsTile(
                        isMediaUploadsEnabled = uiState.isMediaUploadsEnabled,
                        onItemClicked = onMediaUploadsStateChanged,
                    )
                }
            }
        },
    )
}

/**
 * A Composable Preview for [SettingsCameraUploadsView]
 *
 * @param uiState The [SettingsCameraUploadsUiState]
 */
@CombinedThemePreviews
@Composable
private fun SettingsCameraUploadsViewPreview(
    @PreviewParameter(SettingsCameraUploadsViewParameterProvider::class) uiState: SettingsCameraUploadsUiState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SettingsCameraUploadsView(
            uiState = uiState,
            onRegularBusinessAccountSubUserPromptAcknowledged = {},
            onBusinessAccountPromptDismissed = {},
            onCameraUploadsProcessStarted = {},
            onCameraUploadsStateChanged = {},
            onChargingDuringVideoCompressionStateChanged = {},
            onHowToUploadPromptOptionSelected = {},
            onIncludeLocationTagsStateChanged = {},
            onKeepFileNamesStateChanged = {},
            onLocalPrimaryFolderSelected = {},
            onMediaPermissionsGranted = {},
            onMediaUploadsStateChanged = {},
            onNewVideoCompressionSizeLimitProvided = {},
            onPrimaryFolderNodeSelected = {},
            onRequestPermissionsStateChanged = {},
            onUploadOptionUiItemSelected = {},
            onVideoQualityUiItemSelected = {},
        )
    }
}

private class SettingsCameraUploadsViewParameterProvider
    : PreviewParameterProvider<SettingsCameraUploadsUiState> {
    override val values: Sequence<SettingsCameraUploadsUiState>
        get() = sequenceOf(
            // Initial Configuration - Camera Uploads Disabled
            SettingsCameraUploadsUiState(),
            // Camera Uploads Enabled - All Options Shown
            SettingsCameraUploadsUiState(
                isCameraUploadsEnabled = true,
                isMediaUploadsEnabled = true,
                primaryFolderName = "Camera Uploads",
                primaryFolderPath = "primary/folder/path",
                secondaryFolderPath = "secondary/folder/path",
                shouldIncludeLocationTags = true,
                shouldKeepUploadFileNames = true,
                uploadOptionUiItem = UploadOptionUiItem.PhotosAndVideos,
                videoQualityUiItem = VideoQualityUiItem.High,
            ),
        )
}

/**
 * Test Tags for Settings Camera Uploads View
 */
internal const val SETTINGS_CAMERA_UPLOADS_TOOLBAR = "settings_camera_uploads_view:mega_app_bar"