package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.mobile.analytics.event.VideoSectionSearchButtonPressedEvent

@Composable
internal fun VideoSectionTopBar(
    tab: VideoSectionTab,
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    searchState: SearchWidgetState,
    query: String?,
    onSearchTextChanged: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onMenuActionClicked: (VideoSectionMenuAction?) -> Unit,
    menuItems: List<ToolbarMenuItem>,
    handler: NodeActionHandler,
    navHostController: NavHostController,
    clearSelection: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    when {
        isActionMode -> {
            if (tab == VideoSectionTab.All) {
                val actions = menuItems.map {
                    MenuActionWithClick(
                        menuAction = it.action,
                        onClick = it.control(
                            clearSelection,
                            handler::handleAction,
                            navHostController,
                            coroutineScope
                        )
                    )
                }
                SelectModeAppBar(
                    title = "$selectedSize",
                    actions = actions,
                    onNavigationPressed = onBackPressed
                )
            } else {
                SelectModeAppBar(
                    title = selectedSize.toString(),
                    actions = getPlaylistTabMenuItems(),
                    onActionPressed = {
                        onMenuActionClicked(it as? VideoSectionMenuAction)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG),
                    elevation = AppBarDefaults.TopAppBarElevation,
                    onNavigationPressed = onBackPressed
                )
            }
        }

        else -> LegacySearchAppBar(
            modifier = Modifier.testTag(VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG),
            searchWidgetState = searchState,
            typedSearch = query ?: "",
            onSearchTextChange = onSearchTextChanged,
            onCloseClicked = onCloseClicked,
            onBackPressed = onBackPressed,
            onSearchClicked = {
                Analytics.tracker.trackEvent(VideoSectionSearchButtonPressedEvent)
                onSearchClicked()
            },
            elevation = false,
            title = title,
            hintId = R.string.hint_action_search,
            isHideAfterSearch = true,
            leadingActions = listOf(VideoSectionMenuAction.VideoRecentlyWatchedAction),
            onActionPressed = { onMenuActionClicked(it as? VideoSectionMenuAction) },
            windowInsets = WindowInsets(0.dp)
        )
    }
}

@Composable
internal fun VideoSectionTopBar(
    tab: VideoSectionTab,
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    searchState: SearchWidgetState,
    query: String?,
    onMenuActionClicked: (VideoSectionMenuAction?) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    isRemoveLinkMenuActionVisible: Boolean,
) {
    when {
        isActionMode -> {
            SelectModeAppBar(
                title = selectedSize.toString(),
                actions = if (selectedSize == 0) {
                    emptyList()
                } else {
                    if (tab == VideoSectionTab.All) {
                        getAllTabMenuItems(
                            isHideMenuActionVisible = isHideMenuActionVisible,
                            isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                            isRemoveLinkVisible = selectedSize == 1 && isRemoveLinkMenuActionVisible,
                            isRenameVisible = selectedSize == 1
                        )
                    } else {
                        getPlaylistTabMenuItems()
                    }
                },
                onActionPressed = {
                    onMenuActionClicked(it as? VideoSectionMenuAction)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG),
                elevation = AppBarDefaults.TopAppBarElevation,
                onNavigationPressed = onBackPressed
            )
        }

        else -> LegacySearchAppBar(
            modifier = Modifier.testTag(VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG),
            searchWidgetState = searchState,
            typedSearch = query ?: "",
            onSearchTextChange = onSearchTextChanged,
            onCloseClicked = onCloseClicked,
            onBackPressed = onBackPressed,
            onSearchClicked = {
                Analytics.tracker.trackEvent(VideoSectionSearchButtonPressedEvent)
                onSearchClicked()
            },
            elevation = false,
            title = title,
            hintId = R.string.hint_action_search,
            isHideAfterSearch = true,
            leadingActions = listOf(VideoSectionMenuAction.VideoRecentlyWatchedAction),
            onActionPressed = { onMenuActionClicked(it as? VideoSectionMenuAction) },
            windowInsets = WindowInsets(0.dp)
        )
    }
}

private fun getPlaylistTabMenuItems() =
    listOf(
        VideoSectionMenuAction.VideoSectionRemoveAction,
        VideoSectionMenuAction.VideoSectionSelectAllAction,
        VideoSectionMenuAction.VideoSectionClearSelectionAction
    )

private fun getAllTabMenuItems(
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    isRemoveLinkVisible: Boolean,
    isRenameVisible: Boolean,
) =
    mutableListOf<VideoSectionMenuAction>().apply {
        add(VideoSectionMenuAction.VideoSectionDownloadAction)
        add(VideoSectionMenuAction.VideoSectionGetLinkAction)
        add(VideoSectionMenuAction.VideoSectionSendToChatAction)
        add(VideoSectionMenuAction.VideoSectionShareAction)
        add(VideoSectionMenuAction.VideoSectionSelectAllAction)
        add(VideoSectionMenuAction.VideoSectionClearSelectionAction)
        if (isHideMenuActionVisible) {
            add(VideoSectionMenuAction.VideoSectionHideAction)
        }
        if (isUnhideMenuActionVisible) {
            add(VideoSectionMenuAction.VideoSectionUnhideAction)
        }
        if (isRemoveLinkVisible) {
            add(VideoSectionMenuAction.VideoSectionRemoveLinkAction)
        }
        if (isRenameVisible) {
            add(VideoSectionMenuAction.VideoSectionRenameAction)
        }
        add(VideoSectionMenuAction.VideoSectionMoveAction)
        add(VideoSectionMenuAction.VideoSectionCopyAction)
        add(VideoSectionMenuAction.VideoSectionRubbishBinAction)
    }

/**
 * Test tag for search top bar
 */
const val VIDEO_SECTION_SEARCH_TOP_BAR_TEST_TAG = "video_section_view:top_bar_search"

/**
 * Test tag for selected mode top bar
 */
const val VIDEO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG = "video_section_view:top_bar_selected_mode"