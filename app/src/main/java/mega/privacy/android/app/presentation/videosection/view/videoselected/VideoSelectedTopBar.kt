package mega.privacy.android.app.presentation.videosection.view.videoselected

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun VideoSelectedTopBar(
    title: String,
    selectedSize: Int,
    searchState: SearchWidgetState,
    query: String?,
    isEmpty: Boolean,
    onMenuActionClick: (FileInfoMenuAction) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    when {
        isEmpty -> MegaAppBar(
            modifier = Modifier.testTag(EMPTY_TOP_BAR_TEST_TAG),
            appBarType = AppBarType.BACK_NAVIGATION,
            title = title,
            onNavigationPressed = onBackPressed
        )

        selectedSize != 0 -> {
            SelectModeAppBar(
                title = selectedSize.toString(),
                actions = listOf(
                    FileInfoMenuAction.SelectionModeAction.SelectAll,
                    FileInfoMenuAction.SelectionModeAction.ClearSelection
                ),
                onActionPressed = {
                    onMenuActionClick(it as FileInfoMenuAction)
                },
                modifier = Modifier.fillMaxWidth().testTag(SELECTED_MODE_TOP_BAR_TEST_TAG),
                elevation = AppBarDefaults.TopAppBarElevation,
                onNavigationPressed = onBackPressed
            )
        }

        else -> LegacySearchAppBar(
            modifier = Modifier.testTag(SEARCH_TOP_BAR_TEST_TAG),
            searchWidgetState = searchState,
            typedSearch = query ?: "",
            onSearchTextChange = onSearchTextChange,
            onCloseClicked = onCloseClicked,
            onBackPressed = onBackPressed,
            onSearchClicked = onSearchClicked,
            elevation = false,
            title = title,
            hintId = R.string.hint_action_search,
            isHideAfterSearch = true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoSelectedTopBarWithEmptyPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedTopBar(
            title = "Choose files",
            selectedSize = 0,
            searchState = SearchWidgetState.COLLAPSED,
            query = null,
            isEmpty = true,
            onMenuActionClick = {},
            onSearchTextChange = {},
            onCloseClicked = {},
            onSearchClicked = {},
            onBackPressed = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoSelectedTopBarWithSelectedPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedTopBar(
            title = "Choose files",
            selectedSize = 3,
            searchState = SearchWidgetState.COLLAPSED,
            query = null,
            isEmpty = false,
            onMenuActionClick = {},
            onSearchTextChange = {},
            onCloseClicked = {},
            onSearchClicked = {},
            onBackPressed = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoSelectedTopBarWithQueryPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedTopBar(
            title = "Choose files",
            selectedSize = 0,
            searchState = SearchWidgetState.EXPANDED,
            query = "abc",
            isEmpty = false,
            onMenuActionClick = {},
            onSearchTextChange = {},
            onCloseClicked = {},
            onSearchClicked = {},
            onBackPressed = {}
        )
    }
}

/**
 * Test tag for empty top bar
 */
const val EMPTY_TOP_BAR_TEST_TAG = "video_selected_top_bar:top_bar_empty"

/**
 * Test tag for selected mode top bar
 */
const val SELECTED_MODE_TOP_BAR_TEST_TAG = "video_selected_top_bar:top_bar_selected_mode"

/**
 * Test tag for search top bar of video selected
 */
const val SEARCH_TOP_BAR_TEST_TAG = "video_selected_top_bar:top_bar_search"