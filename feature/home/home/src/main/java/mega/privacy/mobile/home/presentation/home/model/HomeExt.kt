package mega.privacy.mobile.home.presentation.home.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey

/**
 * Temporary extension property to get the appropriate [NavKey] for search based on the feature flag.
 */
val HomeUiState.Data.searchNavKey: NavKey
    get() = if (isSearchRevampEnabled) {
        SearchNavKey(
            parentHandle = -1L,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
    } else {
        LegacySearchNavKey(
            parentHandle = -1L,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
    }

