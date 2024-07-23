package mega.privacy.android.feature.sync.ui.synclist.folders

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedResR
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
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
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.CardExpanded
import mega.privacy.android.feature.sync.ui.views.SyncItemView
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.skeleton.CardItemLoadingSkeleton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.SyncListEmptyStateUpgradeButtonPressedEvent

@Composable
internal fun SyncFoldersScreen(
    syncUiItems: List<SyncUiItem>,
    cardExpanded: (CardExpanded) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (folderPairId: Long) -> Unit,
    addFolderClicked: () -> Unit,
    upgradeAccountClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    isFreeAccount: Boolean,
    isLoading: Boolean,
    showSyncsPausedErrorDialog: Boolean,
    onShowSyncsPausedErrorDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = rememberLazyListState(), modifier = modifier
                .fillMaxSize()
        ) {
            if (isLoading) {
                item {
                    SyncFoldersScreenLoadingState()
                }
            } else if (syncUiItems.isEmpty()) {
                item {
                    SyncFoldersScreenEmptyState(
                        isFreeAccount = isFreeAccount,
                        addFolderClicked = addFolderClicked,
                        upgradeNowClicked = upgradeAccountClicked,
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
                            cardExpanded(CardExpanded(syncUiItem, expanded))
                        },
                        pauseRunClicked = pauseRunClicked,
                        removeFolderClicked = removeFolderClicked,
                        issuesInfoClicked = issuesInfoClicked,
                        isLowBatteryLevel = isLowBatteryLevel,
                        isFreeAccount = isFreeAccount,
                        errorRes = syncUiItems[itemIndex].error
                    )
                }
            }
        }
        if (showSyncsPausedErrorDialog) {
            MegaAlertDialog(
                title = "",
                body = stringResource(sharedResR.string.sync_error_dialog_free_user),
                icon = iconPackR.drawable.ic_alert_triangle_color,
                confirmButtonText = stringResource(sharedResR.string.sync_error_dialog_free_user_confirm_action),
                cancelButtonText = null,
                onConfirm = onShowSyncsPausedErrorDialogDismissed,
                onDismiss = onShowSyncsPausedErrorDialogDismissed,
                bodyTextColor = TextColor.Primary,
            )
        }
        if (syncUiItems.isNotEmpty() || isLoading) {
            FloatingActionButton(
                onClick = { addFolderClicked() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
                    .testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(id = sharedResR.string.device_center_sync_add_new_syn_button_option)
                )
            }
        }
    }
}

@Composable
private fun SyncFoldersScreenEmptyState(
    isFreeAccount: Boolean,
    addFolderClicked: () -> Unit,
    upgradeNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.no_syncs_placeholder),
            contentDescription = "Sync folders empty state image",
        )
        MegaText(
            text = stringResource(id = sharedResR.string.device_center_sync_list_empty_state_title),
            textColor = TextColor.Primary,
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.h6Medium
        )
        val messageTypography =
            if (isFreeAccount) MaterialTheme.typography.subtitle2medium else MaterialTheme.typography.subtitle2
        MegaText(
            text = stringResource(id = sharedResR.string.device_center_sync_list_empty_state_message),
            textColor = if (isFreeAccount) TextColor.Primary else TextColor.Secondary,
            modifier = Modifier.padding(top = 16.dp),
            style = messageTypography.copy(textAlign = TextAlign.Center),
        )
        if (isFreeAccount) {
            MegaText(
                text = stringResource(id = sharedResR.string.device_center_sync_list_empty_state_message_free_account),
                textColor = TextColor.Secondary,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS),
                style = MaterialTheme.typography.subtitle2.copy(textAlign = TextAlign.Center),
            )
        }

        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        RaisedDefaultMegaButton(
            textId = if (isFreeAccount) sharedResR.string.general_upgrade_now_label else sharedResR.string.device_center_sync_add_new_syn_button_option,
            onClick = if (isFreeAccount) {
                Analytics.tracker.trackEvent(SyncListEmptyStateUpgradeButtonPressedEvent)
                upgradeNowClicked
            } else {
                addFolderClicked
            },
            modifier = Modifier
                .padding(top = if (isLandscape) 32.dp else if (isFreeAccount) 108.dp else 162.dp)
                .defaultMinSize(minWidth = 232.dp)
                .testTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON),
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
                        vertical = 12.dp,
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
private fun SyncFoldersScreenEmptyStatePreview(
    @PreviewParameter(BooleanProvider::class) isFreeAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            syncUiItems = emptyList(),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {},
            upgradeAccountClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            isFreeAccount = isFreeAccount,
            isLoading = false,
            showSyncsPausedErrorDialog = false,
            onShowSyncsPausedErrorDialogDismissed = {},
        )
    }
}

/**
 * A Preview Composable that displays the Loading state
 */
@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenLoadingStatePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            syncUiItems = emptyList(),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {},
            upgradeAccountClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            isFreeAccount = false,
            isLoading = true,
            showSyncsPausedErrorDialog = false,
            onShowSyncsPausedErrorDialogDismissed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            listOf(
                SyncUiItem(
                    id = 1,
                    folderPairName = "Folder pair name",
                    status = SyncStatus.SYNCING,
                    hasStalledIssues = false,
                    deviceStoragePath = "/path/to/local/folder",
                    megaStoragePath = "/path/to/mega/folder",
                    megaStorageNodeId = NodeId(1234L),
                    method = R.string.sync_two_way,
                    expanded = false,
                )
            ),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {},
            upgradeAccountClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            isFreeAccount = false,
            isLoading = false,
            showSyncsPausedErrorDialog = false,
            onShowSyncsPausedErrorDialogDismissed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncFoldersScreenSyncingWithStalledIssuesPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncFoldersScreen(
            listOf(
                SyncUiItem(
                    id = 1,
                    folderPairName = "Folder pair name",
                    status = SyncStatus.SYNCING,
                    hasStalledIssues = true,
                    deviceStoragePath = "/path/to/local/folder",
                    megaStoragePath = "/path/to/mega/folder",
                    megaStorageNodeId = NodeId(1234L),
                    method = R.string.sync_two_way,
                    expanded = false
                )
            ),
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            addFolderClicked = {},
            upgradeAccountClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            isFreeAccount = false,
            isLoading = false,
            showSyncsPausedErrorDialog = false,
            onShowSyncsPausedErrorDialogDismissed = {},
        )
    }
}

internal const val TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS =
    "sync_list_screen_empty_status_text_for_free_accounts"
internal const val TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON =
    "sync_list_screen_empty_status_button"
internal const val TEST_TAG_SYNC_LIST_SCREEN_FAB = "sync_list_screen:fab"
internal const val TEST_TAG_SYNC_LIST_SCREEN_LOADING_STATE = "sync_list_screen:loading_state"
