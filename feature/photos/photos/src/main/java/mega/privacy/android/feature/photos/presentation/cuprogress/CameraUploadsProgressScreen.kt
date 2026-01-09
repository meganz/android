package mega.privacy.android.feature.photos.presentation.cuprogress

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.transfers.extension.getProgressPercentString
import mega.privacy.android.core.transfers.extension.getProgressSizeString
import mega.privacy.android.core.transfers.extension.getSpeedString
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.photos.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.CameraUploadsActiveTransferItem
import mega.privacy.android.feature.transfers.components.CameraUploadsInQueueTransferItem
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as SharedR
import java.math.BigInteger

@Composable
internal fun CameraUploadsProgressRoute(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraUploadsProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CameraUploadsProgressScreen(
        modifier = modifier,
        uiState = uiState,
        cameraUploadsTransferItemUiState = viewModel::getTransferItemUiState,
        onNavigateUp = onNavigateUp,
        addTransfer = viewModel::addTransfer
    )
}

@Composable
internal fun CameraUploadsProgressScreen(
    uiState: CameraUploadsProgressUiState,
    cameraUploadsTransferItemUiState: (id: Int) -> StateFlow<CameraUploadsTransferItemUiState>,
    onNavigateUp: () -> Unit,
    addTransfer: (transfer: InProgressTransfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(CAMERA_UPLOADS_PROGRESS_SCREEN_TOP_BAR_TAG),
                title = stringResource(SharedR.string.camera_uploads_tranfer_top_bar_title),
                subtitle = pluralStringResource(
                    SharedR.plurals.camera_uploads_tranfer_top_bar_subtitle,
                    uiState.pendingCount,
                    uiState.pendingCount,
                ).takeIf { uiState.pendingCount > 0 },
                navigationType = AppBarNavigationType.Back(onNavigationIconClicked = onNavigateUp)
            )
        }
    ) { paddingValues ->
        Content(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            cameraUploadsTransferItemUiState = cameraUploadsTransferItemUiState,
            addTransfer = addTransfer
        )
    }
}

@Composable
private fun Content(
    uiState: CameraUploadsProgressUiState,
    cameraUploadsTransferItemUiState: (id: Int) -> StateFlow<CameraUploadsTransferItemUiState>,
    addTransfer: (transfer: InProgressTransfer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            uiState.isLoading ->
                NodesViewSkeleton(
                    modifier = Modifier.testTag(
                        TEST_TAG_CAMERA_UPLOADS_TRANSFER_SKELETON_LOADING_VIEW
                    ),
                    contentPadding = PaddingValues()
                )

            uiState.transfers.isEmpty() -> CameraUploadsTransferEmptyView()

            else -> {
                val totalItemCount = uiState.transfers.sumOf { section ->
                    when (section) {
                        is CameraUploadsTransferType.InProgress -> section.items.size
                        is CameraUploadsTransferType.InQueue -> section.items.size
                    }
                }

                FastScrollLazyColumn(
                    state = rememberLazyListState(),
                    totalItems = totalItemCount,
                ) {
                    uiState.transfers.forEach { type ->
                        when (type) {
                            is CameraUploadsTransferType.InProgress -> {
                                item(key = "in progress header") {
                                    MegaText(
                                        modifier = Modifier
                                            .padding(15.dp)
                                            .testTag(
                                                TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER
                                            ),
                                        text = stringResource(SharedR.string.camera_uploads_tranfer_header_in_progress),
                                        textColor = TextColor.Primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                items(items = type.items, key = { it.uniqueId }) { item ->
                                    CameraUploadsTransferItem(
                                        cameraUploadsTransferItemUiStateFlow = cameraUploadsTransferItemUiState,
                                        item = item,
                                        isInProgress = true,
                                        addTransfer = addTransfer
                                    )
                                }
                            }

                            is CameraUploadsTransferType.InQueue -> {
                                item(key = "in queue header") {
                                    MegaText(
                                        modifier = Modifier
                                            .padding(15.dp)
                                            .testTag(
                                                TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER
                                            ),
                                        text = stringResource(SharedR.string.camera_uploads_tranfer_header_in_queue),
                                        textColor = TextColor.Primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                items(items = type.items, key = { it.uniqueId }) { item ->
                                    CameraUploadsTransferItem(
                                        cameraUploadsTransferItemUiStateFlow = cameraUploadsTransferItemUiState,
                                        item = item,
                                        isInProgress = false,
                                        addTransfer = addTransfer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraUploadsTransferEmptyView(modifier: Modifier = Modifier) {
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
                painter = painterResource(IconPackR.drawable.ic_check_circle_color),
                contentDescription = "Camera Uploads transfer empty view Check circle icon"
            )
        }

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_TITLE),
            text = stringResource(SharedR.string.camera_uploads_tranfer_empty_view_title),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 10.dp)
                .testTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_DESCRIPTION),
            text = stringResource(SharedR.string.camera_uploads_tranfer_empty_view_description),
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CameraUploadsTransferItem(
    cameraUploadsTransferItemUiStateFlow: (id: Int) -> StateFlow<CameraUploadsTransferItemUiState>,
    item: InProgressTransfer,
    isInProgress: Boolean,
    addTransfer: (transfer: InProgressTransfer) -> Unit,
) {
    val cameraUploadsTransferItemUiState by cameraUploadsTransferItemUiStateFlow(item.tag).collectAsStateWithLifecycle()

    LaunchedEffect(key1 = item.tag) {
        addTransfer(item)
    }

    if (isInProgress) {
        CameraUploadsActiveTransferItem(
            tag = item.tag,
            fileTypeResId = cameraUploadsTransferItemUiState.fileTypeResId,
            previewUri = cameraUploadsTransferItemUiState.previewUri,
            fileName = item.fileName,
            progressPercentageString = item.getProgressSizeString(),
            progressSizeString = item.getProgressPercentString(),
            progress = item.progress.floatValue,
            speed = item.getSpeedString(
                areTransfersPaused = false,
                isTransferOverQuota = false,
                isStorageOverQuota = false,
            )
        )
    } else {
        CameraUploadsInQueueTransferItem(
            tag = item.tag,
            fileTypeResId = cameraUploadsTransferItemUiState.fileTypeResId,
            previewUri = cameraUploadsTransferItemUiState.previewUri,
            fileName = item.fileName,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsProgressScreenLoadingPreview() {
    AndroidThemeForPreviews {
        CameraUploadsProgressScreen(
            uiState = CameraUploadsProgressUiState(isLoading = true),
            onNavigateUp = {},
            cameraUploadsTransferItemUiState = { MutableStateFlow(CameraUploadsTransferItemUiState()) },
            addTransfer = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsProgressScreenEmptyPreview() {
    AndroidThemeForPreviews {
        CameraUploadsProgressScreen(
            uiState = CameraUploadsProgressUiState(isLoading = false),
            onNavigateUp = {},
            cameraUploadsTransferItemUiState = { MutableStateFlow(CameraUploadsTransferItemUiState()) },
            addTransfer = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsProgressScreenPreview() {
    AndroidThemeForPreviews {
        CameraUploadsProgressScreen(
            uiState = CameraUploadsProgressUiState(
                isLoading = false,
                pendingCount = 1,
                transfers = listOf(
                    CameraUploadsTransferType.InProgress(
                        items = listOf(
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
                            )
                        )
                    ),
                    CameraUploadsTransferType.InQueue(
                        items = listOf(
                            InProgressTransfer.Upload(
                                uniqueId = 2L,
                                tag = 2,
                                totalBytes = 10L * 1024 * 1024,
                                isPaused = false,
                                fileName = "Video name.mp4",
                                speed = 0,
                                state = TransferState.STATE_QUEUED,
                                priority = BigInteger.ONE,
                                progress = Progress(0f),
                                localPath = "",
                            )
                        )
                    )
                )
            ),
            onNavigateUp = {},
            cameraUploadsTransferItemUiState = { MutableStateFlow(CameraUploadsTransferItemUiState()) },
            addTransfer = {}
        )
    }
}

internal const val CAMERA_UPLOADS_PROGRESS_SCREEN_TOP_BAR_TAG =
    "camera_uploads_progress_screen:top_bar"

/**
 * Tag for the description of Camera Uploads transfer skeleton loading view.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_SKELETON_LOADING_VIEW =
    "camera_uploads_transfers_view:view_skeleton_loading"

/**
 * Tag for the description of Camera Uploads transfer empty view.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW =
    "camera_uploads_transfers_view:view_empty"

/**
 * Tag for the icon of Camera Uploads transfer empty view.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_ICON =
    "camera_uploads_transfers_view:empty_view_icon"

/**
 * Tag for the title of Camera Uploads transfer empty view.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_TITLE =
    "camera_uploads_transfers_view:empty_view_title"

/**
 * Tag for the description of Camera Uploads transfer empty view.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_DESCRIPTION =
    "camera_uploads_transfers_view:empty_view_description"

/**
 * Tag for the description of Camera Uploads transfer in progress header.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER =
    "camera_uploads_transfers_view:header_in_progress"

/**
 * Tag for the description of Camera Uploads transfer in queue header.
 */
internal const val TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER =
    "camera_uploads_transfers_view:header_in_queue"
