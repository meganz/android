package mega.privacy.android.feature.sync.ui.synclist.folders

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.BOTTOM_PADDING
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.CardExpanded
import mega.privacy.android.feature.sync.ui.views.SyncItemView
import mega.privacy.android.feature.sync.ui.views.SyncTypePreviewProvider
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.skeleton.CardItemLoadingSkeleton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.SyncCardExpandedEvent
import mega.privacy.mobile.analytics.event.SyncFoldersListDisplayedEvent

@Composable
internal fun SyncFoldersScreen(
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (CardExpanded) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (SyncUiItem) -> Unit,
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenDeviceFolderClicked: (String) -> Unit,
    onOpenMegaFolderClicked: (SyncUiItem) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    isStorageOverQuota: Boolean,
    isLoading: Boolean,
    deviceName: String,
    modifier: Modifier = Modifier,
    onLocalFolderSelected: (SyncUiItem, Uri) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = if (syncUiItems.isEmpty()) 0.dp else BOTTOM_PADDING.dp
            ),
        ) {
            if (isLoading) {
                item {
                    SyncFoldersScreenLoadingState()
                }
            } else if (syncUiItems.isEmpty()) {
                item {
                    SyncFoldersScreenEmptyState(
                        onAddNewSyncClicked = onAddNewSyncClicked,
                        onAddNewBackupClicked = onAddNewBackupClicked,
                        buttonsEnabled = isStorageOverQuota.not(),
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth()
                            .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
                    )
                }
            } else {
                items(count = syncUiItems.size, key = {
                    syncUiItems[it].id
                }) { itemIndex ->
                    SyncItemView(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        syncUiItems = syncUiItems,
                        itemIndex = itemIndex,
                        cardExpanded = { syncUiItem, expanded ->
                            if (expanded) {
                                Analytics.tracker.trackEvent(SyncCardExpandedEvent)
                            }
                            cardExpanded(CardExpanded(syncUiItem, expanded))
                        },
                        pauseRunClicked = pauseRunClicked,
                        removeFolderClicked = removeFolderClicked,
                        issuesInfoClicked = issuesInfoClicked,
                        onOpenDeviceFolderClicked = onOpenDeviceFolderClicked,
                        onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
                        isLowBatteryLevel = isLowBatteryLevel,
                        isStorageOverQuota = isStorageOverQuota,
                        errorRes = syncUiItems[itemIndex].error,
                        deviceName = deviceName,
                        onLocalFolderSelected = onLocalFolderSelected
                    )
                }
            }
        }
    }

    LaunchedEffect(syncUiItems) {
        if (syncUiItems.isNotEmpty()) {
            Analytics.tracker.trackEvent(SyncFoldersListDisplayedEvent)
        }
    }
}


@Composable
private fun SyncFoldersScreenEmptyState(
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    modifier: Modifier = Modifier,
    buttonsEnabled: Boolean = true,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(all = if (isLandscape) 0.dp else 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(R.drawable.no_syncs_placeholder),
            contentDescription = "Sync folders empty state image",
        )
        MegaText(
            text = stringResource(id = sharedResR.string.device_center_sync_backup_list_empty_state_title),
            textColor = TextColor.Primary,
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.h6Medium,
            textAlign = TextAlign.Center,
        )
        MegaText(
            text = stringResource(id = sharedResR.string.device_center_sync_backup_list_empty_state_message),
            textColor = TextColor.Secondary,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT),
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
        )
        RaisedDefaultMegaButton(
            textId = sharedResR.string.device_center_sync_add_new_syn_button_option,
            onClick = onAddNewSyncClicked,
            enabled = buttonsEnabled,
            modifier = Modifier
                .padding(top = if (isLandscape) 32.dp else 48.dp)
                .defaultMinSize(minWidth = 232.dp)
                .testTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_SYNC_BUTTON),
        )
        RaisedDefaultMegaButton(
            textId = sharedResR.string.device_center_sync_add_new_backup_button_option,
            onClick = onAddNewBackupClicked,
            enabled = buttonsEnabled,
            modifier = Modifier
                .padding(top = 24.dp)
                .defaultMinSize(minWidth = 232.dp)
                .testTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BACKUP_BUTTON),
        )
    }
}

/**
 * A Composable that displays the initial Loading state
 */
@Composable
private fun SyncFoldersScreenLoadingState() {
    Column(
        modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_LOADING_STATE),
        content = {
            for (i in 1..4) {
                CardItemLoadingSkeleton(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 16.dp
                    )
                )
            }
        }
    )
}

@CombinedThemePreviews
@Composable
@Preview(name = "5-inch Device Portrait", widthDp = 360, heightDp = 640)
@Preview(name = "5-inch Device Portrait", widthDp = 640, heightDp = 360)
private fun SyncFoldersScreenEmptyStatePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            syncUiItems = emptyList(),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            isStorageOverQuota = false,
            isLoading = false,
            deviceName = "Device Name",
        )
    }
}

/**
 * A Preview Composable that displays the Loading state
 */
@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenLoadingStatePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            syncUiItems = emptyList(),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            isStorageOverQuota = false,
            isLoading = true,
            deviceName = "Device Name",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            listOf(
                SyncUiItem(
                    id = 1,
                    syncType = syncType,
                    folderPairName = "Folder pair name",
                    status = SyncStatus.SYNCING,
                    hasStalledIssues = false,
                    deviceStoragePath = "/path/to/local/folder",
                    megaStoragePath = "/path/to/mega/folder",
                    megaStorageNodeId = NodeId(1234L),
                    expanded = false,
                    uriPath = UriPath("/path/to/local/folder")
                )
            ),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            isStorageOverQuota = false,
            isLoading = false,
            deviceName = "Device Name",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingWithStalledIssuesPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            listOf(
                SyncUiItem(
                    id = 1,
                    syncType = syncType,
                    folderPairName = "Folder pair name",
                    status = SyncStatus.SYNCING,
                    hasStalledIssues = true,
                    deviceStoragePath = "/path/to/local/folder",
                    megaStoragePath = "/path/to/mega/folder",
                    megaStorageNodeId = NodeId(1234L),
                    expanded = false,
                    uriPath = UriPath("/path/to/local/folder")
                )
            ),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            isStorageOverQuota = false,
            isLoading = false,
            deviceName = "Device Name",
        )
    }
}

internal const val TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT =
    "sync_list_screen_empty_status_text"
internal const val TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_SYNC_BUTTON =
    "sync_list_screen:empty_status:sync_button"
internal const val TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BACKUP_BUTTON =
    "sync_list_screen:empty_status:backup_button"
internal const val TEST_TAG_SYNC_LIST_SCREEN_FAB = "sync_list_screen:fab"
internal const val TEST_TAG_SYNC_LIST_SCREEN_LOADING_STATE = "sync_list_screen:loading_state"
