package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey

/**
 * Temporary extension property to get the appropriate [NavKey] for search based on the feature flag.
 */
val CloudDriveUiState.searchNavKey: NavKey
    get() = if (isSearchRevampEnabled) {
        SearchNavKey(
            parentHandle = currentFolderId.longValue,
            nodeSourceType = nodeSourceType
        )
    } else {
        LegacySearchNavKey(
            parentHandle = currentFolderId.longValue,
            nodeSourceType = nodeSourceType
        )
    }