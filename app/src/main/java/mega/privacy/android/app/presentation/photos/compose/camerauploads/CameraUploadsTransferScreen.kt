package mega.privacy.android.app.presentation.photos.compose.camerauploads

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.extensions.transfers.getProgressPercentString
import mega.privacy.android.app.presentation.extensions.transfers.getProgressSizeString
import mega.privacy.android.app.presentation.extensions.transfers.getSpeedString
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowMenuAction
import mega.privacy.android.app.presentation.photos.model.CameraUploadsTransferType
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.CameraUploadsTransferViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.CameraUploadsActiveTransferItem
import mega.privacy.android.feature.transfers.components.CameraUploadsInQueueTransferItem
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyColumn
import mega.privacy.android.shared.resources.R as sharedR
import java.math.BigInteger

@Composable
fun CameraUploadsTransferScreen(
    timelineViewModel: TimelineViewModel,
    navHostController: NavHostController,
    onSettingOptionClick: () -> Unit,
    cameraUploadsTransferViewModel: CameraUploadsTransferViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val uiState by timelineViewModel.state.collectAsStateWithLifecycle()
    val types by cameraUploadsTransferViewModel.cameraUploadsTransfers.collectAsStateWithLifecycle()
    var subtitle by remember { mutableStateOf<String?>(null) }

    BackHandler {
        navHostController.popBackStack()
    }

    LaunchedEffect(uiState) {
        subtitle = if (uiState.pending > 0) {
            resources.getQuantityString(
                sharedR.plurals.camera_uploads_tranfer_top_bar_subtitle,
                uiState.pending,
                uiState.pending
            )
        } else {
            null
        }
    }

    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.camera_uploads_tranfer_top_bar_title),
                subtitle = subtitle,
                navigationType = AppBarNavigationType.Back {
                    navHostController.popBackStack()
                },
                actions = listOf(SlideshowMenuAction.SettingOptionsMenuAction),
                onActionPressed = { onSettingOptionClick() }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CameraUploadsTranferView(uiState = uiState, types = types)
        }
    }
}

@Composable
internal fun CameraUploadsTranferView(
    uiState: TimelineViewState,
    types: List<CameraUploadsTransferType>,
) {
    val isSyncing = uiState.cameraUploadsStatus == CameraUploadsStatus.Sync
    val isUploading = uiState.cameraUploadsStatus == CameraUploadsStatus.Uploading
    when {
        isSyncing || (isUploading && types.isEmpty()) ->
            NodesViewSkeleton(
                modifier = Modifier.testTag(
                    TEST_TAG_CAMERA_UPLOADS_TRANSFER_SKELETON_LOADING_VIEW
                ), contentPadding = PaddingValues()
            )

        types.isEmpty() -> CameraUploadsTransferEmptyView()

        else -> {
            val totalItemCount = types.sumOf { section ->
                when (section) {
                    is CameraUploadsTransferType.InProgress -> section.items.size
                    is CameraUploadsTransferType.InQueue -> section.items.size
                }
            }

            FastScrollLazyColumn(
                state = rememberLazyListState(),
                totalItems = totalItemCount,
            ) {
                types.forEach { type ->
                    when (type) {
                        is CameraUploadsTransferType.InProgress -> {
                            item(key = "in progress header") {
                                MegaText(
                                    modifier = Modifier
                                        .padding(15.dp)
                                        .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER),
                                    text = stringResource(sharedR.string.camera_uploads_tranfer_header_in_progress),
                                    textColor = TextColor.Primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            items(count = type.items.size, key = { type.items[it].uniqueId }) {
                                val item = type.items[it]
                                CameraUploadsTransferItem(item = item, isInProgress = true)
                            }
                        }

                        is CameraUploadsTransferType.InQueue -> {
                            item(key = "in queue header") {
                                MegaText(
                                    modifier = Modifier
                                        .padding(15.dp)
                                        .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER),
                                    text = stringResource(sharedR.string.camera_uploads_tranfer_header_in_queue),
                                    textColor = TextColor.Primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            items(count = type.items.size, key = { type.items[it].uniqueId }) {
                                val item = type.items[it]
                                CameraUploadsTransferItem(item = item, isInProgress = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CameraUploadsTransferEmptyView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier
                    .size(120.dp)
                    .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_ICON),
                painter = painterResource(iconPackR.drawable.ic_check_circle_color),
                contentDescription = "Camera Uploads transfer empty view Check circle icon"
            )
        }

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_TITLE),
            text = stringResource(sharedR.string.camera_uploads_tranfer_empty_view_title),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 10.dp)
                .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_DESCRIPTION),
            text = stringResource(sharedR.string.camera_uploads_tranfer_empty_view_description),
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun CameraUploadsTransferItem(
    item: InProgressTransfer,
    isInProgress: Boolean,
    viewModel: ActiveTransferImageViewModel = hiltViewModel(),
) {
    val transferImageState by viewModel.getUiStateFlow(item.tag)
        .collectAsStateWithLifecycle()

    LaunchedEffect(key1 = item.tag) {
        viewModel.addTransfer(item)
    }

    if (isInProgress) {
        CameraUploadsActiveTransferItem(
            item.tag,
            transferImageState.fileTypeResId,
            transferImageState.previewUri,
            item.fileName,
            item.getProgressSizeString(),
            item.getProgressPercentString(),
            item.progress.floatValue,
            item.getSpeedString(
                areTransfersPaused = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
        )
    } else {
        CameraUploadsInQueueTransferItem(
            item.tag,
            transferImageState.fileTypeResId,
            transferImageState.previewUri,
            item.fileName,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsTransferEmptyViewPreview() {
    AndroidThemeForPreviews {
        CameraUploadsTransferEmptyView(modifier = Modifier)
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsTransferInProgressItemPreview() {
    AndroidThemeForPreviews {
        CameraUploadsTransferItem(
            InProgressTransfer.Upload(
                uniqueId = 1L,
                tag = 1,
                totalBytes = 10L * 1024 * 1024,
                isPaused = false,
                fileName = "Video name.mp4",
                speed = 4L * 1024 * 1024,
                state = TransferState.STATE_ACTIVE,
                priority = BigInteger.ONE,
                progress = Progress(0.6f),
                localPath = "",
            ),
            true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsTransferInQueueItemPreview() {
    AndroidThemeForPreviews {
        CameraUploadsTransferItem(
            InProgressTransfer.Upload(
                uniqueId = 1L,
                tag = 1,
                totalBytes = 10L * 1024 * 1024,
                isPaused = false,
                fileName = "Video name.mp4",
                speed = 0,
                state = TransferState.STATE_QUEUED,
                priority = BigInteger.ONE,
                progress = Progress(0f),
                localPath = "",
            ),
            false
        )
    }
}

/**
 * Tag for the icon of Camera Uploads transfer empty view.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_ICON =
    "camera_uploads_transfers_view:empty_view_icon"

/**
 * Tag for the title of Camera Uploads transfer empty view.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_TITLE =
    "camera_uploads_transfers_view:empty_view_title"

/**
 * Tag for the description of Camera Uploads transfer empty view.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_DESCRIPTION =
    "camera_uploads_transfers_view:empty_view_description"

/**
 * Tag for the description of Camera Uploads transfer empty view.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW = "camera_uploads_transfers_view:view_empty"

/**
 * Tag for the description of Camera Uploads transfer in progress header.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER =
    "camera_uploads_transfers_view:header_in_progress"

/**
 * Tag for the description of Camera Uploads transfer in queue header.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER =
    "camera_uploads_transfers_view:header_in_queue"

/**
 * Tag for the description of Camera Uploads transfer skeleton loading view.
 */
const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_SKELETON_LOADING_VIEW =
    "camera_uploads_transfers_view:view_skeleton_loading"