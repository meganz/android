package mega.privacy.android.feature.devicecenter.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.sync.ui.SyncEmptyState

/**
 * A [androidx.compose.runtime.Composable] which displays an Items not found state
 */
@Composable
internal fun DeviceCenterNoItemsFound() {
    SyncEmptyState(
        iconId = R.drawable.ic_search_02,
        iconSize = 128.dp,
        iconDescription = "No results found for search",
        textId = mega.privacy.android.feature.devicecenter.R.string.device_center_empty_screen_no_results,
        testTag = DEVICE_CENTER_NO_ITEMS_FOUND_STATE
    )
}

internal const val DEVICE_CENTER_NO_ITEMS_FOUND_STATE = "device_center_content:no_items_found_state"