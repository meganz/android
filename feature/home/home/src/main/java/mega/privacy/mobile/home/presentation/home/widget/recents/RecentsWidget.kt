package mega.privacy.mobile.home.presentation.home.widget.recents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.RecentsBucketScreenNavKey
import mega.privacy.android.navigation.destination.RecentsScreenNavKey
import mega.privacy.mobile.home.presentation.home.widget.recents.view.RecentsWidgetHeader
import mega.privacy.mobile.home.presentation.recents.RecentsViewModel
import mega.privacy.mobile.home.presentation.recents.RecentsWidgetConstants
import mega.privacy.mobile.home.presentation.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiState
import mega.privacy.mobile.home.presentation.recents.view.RecentDateHeader
import mega.privacy.mobile.home.presentation.recents.view.RecentsEmptyView
import mega.privacy.mobile.home.presentation.recents.view.RecentsHiddenView
import mega.privacy.mobile.home.presentation.recents.view.RecentsListItemView
import mega.privacy.mobile.home.presentation.recents.view.RecentsLoadingView
import mega.privacy.mobile.home.presentation.recents.view.RecentsOptionsBottomSheet
import javax.inject.Inject

class RecentsWidget @Inject constructor() : HomeWidget {

    override val identifier: String = "RecentsWidgetProvider"
    override val defaultOrder: Int = 2
    override val canDelete: Boolean = false
    override suspend fun getWidgetName() = LocalizedText.StringRes(R.string.section_recents)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        onNavigate: (NavKey) -> Unit,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<RecentsViewModel, RecentsViewModel.Factory> { factory ->
            factory.create(maxBucketCount = RecentsWidgetConstants.WIDGET_MAX_BUCKETS)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val snackBarEventQueue = rememberSnackBarQueue()
        val snackBarHostState = LocalSnackBarHostState.current
        var openedFileNode by remember { mutableStateOf<Pair<TypedFileNode, NodeSourceType>?>(null) }
        var showOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
        val context = LocalContext.current
        RecentsView(
            uiState = uiState,
            modifier = modifier,
            onFileClicked = { node, source ->
                openedFileNode = node to source
            },
            onBucketClicked = { item ->
                onNavigate(
                    RecentsBucketScreenNavKey(
                        identifier = item.bucket.identifier,
                        folderName = item.parentFolderName.get(context),
                        nodeSourceType = item.nodeSourceType,
                        timestamp = item.bucket.timestamp,
                        fileCount = item.bucket.nodes.size,
                    )
                )
            },
            onWidgetOptionsClicked = { showOptionsBottomSheet = true },
            onShowRecentActivity = viewModel::showRecentActivity,
            onUploadClicked = {
                onNavigate(HomeFabOptionsBottomSheetNavKey)
            },
            onViewAllClicked = {
                onNavigate(RecentsScreenNavKey)
            }
        )

        openedFileNode?.let { (node, source) ->
            HandleNodeAction3(
                typedFileNode = node,
                snackBarHostState = snackBarHostState,
                coroutineScope = coroutineScope,
                onActionHandled = { openedFileNode = null },
                nodeSourceType = source,
                onDownloadEvent = transferHandler::setTransferEvent,
                onNavigate = onNavigate,
            )
        }

        RecentsOptionsBottomSheet(
            isVisible = showOptionsBottomSheet,
            onDismiss = { showOptionsBottomSheet = false },
            isHideRecentsEnabled = uiState.isHideRecentsEnabled,
            onShowRecentActivity = viewModel::showRecentActivity,
            onHideRecentActivity = {
                viewModel.hideRecentActivity()
                coroutineScope.launch {
                    snackBarEventQueue.queueMessage("Your recent activity has been hidden") // TODO: Localize
                }
            },
        )
    }
}

@Composable
fun RecentsView(
    uiState: RecentsUiState,
    onFileClicked: (TypedFileNode, NodeSourceType) -> Unit,
    onBucketClicked: (RecentsUiItem) -> Unit = {},
    onShowRecentActivity: () -> Unit,
    onWidgetOptionsClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAllClicked: () -> Unit,
) {
    Column(modifier = modifier) {
        RecentsWidgetHeader(
            modifier = Modifier,
            onOptionsClicked = onWidgetOptionsClicked
        )
        when {
            uiState.isHideRecentsEnabled -> {
                RecentsHiddenView(
                    modifier = modifier,
                    onShowActivityClicked = onShowRecentActivity,
                )
            }

            uiState.isLoading -> {
                RecentsLoadingView()
            }

            uiState.isEmpty -> {
                RecentsEmptyView(
                    onUploadClicked = onUploadClicked
                )
            }

            else -> {
                val grouped = remember(uiState.recentActionItems) {
                    uiState.recentActionItems.groupBy { it.timestampText.dateOnlyTimestamp }
                }
                grouped.forEach { (dateTimestamp, itemsForDate) ->
                    RecentDateHeader(dateTimestamp)

                    itemsForDate.forEach { item ->
                        RecentsListItemView(
                            item = item,
                            onItemClicked = {
                                if (item.isSingleNode) {
                                    item.firstNode?.let { node ->
                                        onFileClicked(node, item.nodeSourceType)
                                    }
                                } else {
                                    onBucketClicked(item)
                                }
                            },
                            onMenuClicked = {
                                // TODO: Handle menu click
                            }
                        )
                    }
                }

                if (uiState.recentActionItems.size >= RecentsWidgetConstants.WIDGET_MAX_BUCKETS) {
                    TextButton(
                        onClick = onViewAllClicked,
                        modifier = Modifier.testTag(RECENTS_VIEW_ALL_BUTTON_TEST_TAG),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                        )
                    ) {
                        MegaText(
                            text = "View all", // TODO: localize
                            style = AppTheme.typography.titleSmall.copy(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.W500
                            )
                        )
                    }
                }
            }
        }
    }
}

internal const val RECENTS_VIEW_ALL_BUTTON_TEST_TAG = "recents_widget:view_all_button"


@CombinedThemePreviews
@Composable
private fun RecentsViewPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsUiState(
                recentActionItems = mockRecentsUiItemList(),
                isNodesLoading = false,
                isHiddenNodeSettingsLoading = false
            ),
            onFileClicked = { _, _ -> },
            onWidgetOptionsClicked = {},
            onShowRecentActivity = {},
            onUploadClicked = {},
            onViewAllClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsHiddenViewPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsUiState(
                recentActionItems = emptyList(),
                isNodesLoading = false,
                isHideRecentsEnabled = true
            ),
            onFileClicked = { _, _ -> },
            onWidgetOptionsClicked = {},
            onShowRecentActivity = {},
            onUploadClicked = {},
            onViewAllClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsViewEmptyPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsUiState(
                recentActionItems = emptyList(),
                isNodesLoading = false,
                isHiddenNodeSettingsLoading = false
            ),
            onFileClicked = { _, _ -> },
            onWidgetOptionsClicked = {},
            onShowRecentActivity = {},
            onUploadClicked = {},
            onViewAllClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsViewLoadingPreview() {
    AndroidThemeForPreviews {
        RecentsView(
            uiState = RecentsUiState(
                recentActionItems = emptyList(),
                isNodesLoading = true,
                isHiddenNodeSettingsLoading = true
            ),
            onFileClicked = { _, _ -> },
            onWidgetOptionsClicked = {},
            onShowRecentActivity = {},
            onUploadClicked = {},
            onViewAllClicked = {}
        )
    }
}

internal fun createMockRecentsUiItem(
    title: RecentActionTitleText,
    parentFolderName: LocalizedText,
    timestamp: Long,
    icon: Int = IconPackR.drawable.ic_generic_medium_solid,
    shareIcon: Int? = null,
    isMediaBucket: Boolean = false,
    isUpdate: Boolean = false,
    updatedByText: LocalizedText? = null,
    userName: String? = null,
    isFavourite: Boolean = false,
    nodeLabel: NodeLabel? = null,
): RecentsUiItem {
    val mockBucket = RecentActionBucket(
        identifier = "bucket_$timestamp",
        timestamp = timestamp,
        userEmail = "test@example.com",
        parentNodeId = NodeId(1L),
        isUpdate = isUpdate,
        isMedia = isMediaBucket,
        nodes = emptyList(),
    )
    return RecentsUiItem(
        title = title,
        icon = icon,
        shareIcon = shareIcon,
        parentFolderName = parentFolderName,
        timestampText = RecentsTimestampText(timestamp),
        isMediaBucket = isMediaBucket,
        isUpdate = isUpdate,
        updatedByText = updatedByText,
        userName = userName,
        isFavourite = isFavourite,
        nodeLabel = nodeLabel,
        bucket = mockBucket,
        isSingleNode = !isMediaBucket,
        isSensitive = false
    )
}


internal fun mockRecentsUiItemList() = listOf(
    createMockRecentsUiItem(
        title = RecentActionTitleText.MediaBucketImagesOnly(5),
        parentFolderName = LocalizedText.Literal("Photos"),
        timestamp = System.currentTimeMillis() / 1000 - 3600,
        icon = IconPackR.drawable.ic_image_stack_medium_solid,
        isMediaBucket = true,
        shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
    ),
    createMockRecentsUiItem(
        title = RecentActionTitleText.SingleNode("Document.pdf"),
        parentFolderName = LocalizedText.Literal("Cloud Drive"),
        timestamp = System.currentTimeMillis() / 1000 - 207203,
        icon = IconPackR.drawable.ic_pdf_medium_solid,
        isFavourite = true,
        nodeLabel = NodeLabel.RED,
    ),
    createMockRecentsUiItem(
        title = RecentActionTitleText.RegularBucket("Presentation.pptx", 3),
        parentFolderName = LocalizedText.Literal("Work"),
        timestamp = System.currentTimeMillis() / 1000 - 207202,
        icon = IconPackR.drawable.ic_generic_medium_solid,
        isUpdate = true,
        updatedByText = LocalizedText.StringRes(
            R.string.update_action_bucket,
            listOf("John Doe")
        ),
        userName = "John Doe",
    ),
    createMockRecentsUiItem(
        title = RecentActionTitleText.SingleNode("Test.pptx"),
        parentFolderName = LocalizedText.Literal("Work"),
        timestamp = System.currentTimeMillis() / 1000 - 207201,
        icon = IconPackR.drawable.ic_generic_medium_solid,
        isUpdate = true,
        updatedByText = LocalizedText.StringRes(
            R.string.update_action_bucket,
            listOf("John Doe")
        ),
        userName = "John Doe",
    ),
)