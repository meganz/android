package mega.privacy.android.app.mediaplayer.queue.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.queue.model.VideoPlayerMenuAction
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar

@Composable
internal fun VideoQueueTopBar(
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    searchState: SearchWidgetState,
    query: String?,
    onMenuActionClick: (VideoPlayerMenuAction?) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    when {
        isActionMode -> {
            SelectModeAppBar(
                title = if (selectedSize == 0) {
                    stringResource(id = R.string.title_select_tracks)
                } else {
                    selectedSize.toString()
                },
                actions = if (selectedSize == 0) {
                    emptyList()
                } else {
                    listOf(VideoPlayerMenuAction.VideoQueueRemoveAction)
                },
                onActionPressed = {
                    onMenuActionClick(it as? VideoPlayerMenuAction)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(VIDEO_QUEUE_SELECTED_MODE_TOP_BAR_TEST_TAG),
                elevation = AppBarDefaults.TopAppBarElevation,
                onNavigationPressed = onBackPressed
            )
        }

        else -> LegacySearchAppBar(
            modifier = Modifier.testTag(VIDEO_QUEUE_SEARCH_TOP_BAR_TEST_TAG),
            searchWidgetState = searchState,
            typedSearch = query ?: "",
            onSearchTextChange = onSearchTextChange,
            onCloseClicked = onCloseClicked,
            onBackPressed = onBackPressed,
            onSearchClicked = onSearchClicked,
            elevation = false,
            title = title,
            hintId = R.string.hint_action_search,
            isHideAfterSearch = true,
            actions = listOf(VideoPlayerMenuAction.VideoQueueSelectAction),
            onActionPressed = { onMenuActionClick(it as? VideoPlayerMenuAction) }
        )
    }
}

/**
 * Test tag for search top bar of video selected
 */
const val VIDEO_QUEUE_SEARCH_TOP_BAR_TEST_TAG = "video_queue_view:top_bar_search"

/**
 * Test tag for selected mode top bar
 */
const val VIDEO_QUEUE_SELECTED_MODE_TOP_BAR_TEST_TAG = "video_queue_view:top_bar_selected_mode"