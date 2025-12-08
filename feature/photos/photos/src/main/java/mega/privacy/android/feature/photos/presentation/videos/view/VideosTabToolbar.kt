package mega.privacy.android.feature.photos.presentation.videos.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaAppBarAction.CameraUpload.CameraUploadStatus
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.shared.resources.R

@Composable
fun VideosTabToolbar(
    searchQuery: String?,
    updateSearchQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    isSearchMode: Boolean = true,
    onBackPressed: () -> Unit = {},
    onSearchingModeChanged: ((Boolean) -> Unit)? = null,
) {
    MegaSearchTopAppBar(
        modifier = modifier.testTag(VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG),
        navigationType = AppBarNavigationType.Back(onBackPressed),
        title = title,
        query = searchQuery,
        onQueryChanged = updateSearchQuery,
        isSearchingMode = isSearchMode,
        onSearchingModeChanged = onSearchingModeChanged,
        actions = emptyList()
    )
}

const val VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG = "VIDEOS_TAB_SEARCH_TOP_APP_BAR_TAG"