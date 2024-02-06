package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.view.toolbar.ToolbarViewModel
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.legacy.core.ui.controls.appbar.ExpandedSearchAppBar

/**
 * Search toolbar used in search activity
 *
 * @param selectionCount
 * @param searchQuery
 * @param updateSearchQuery
 * @param menuActions
 */
@Composable
fun SearchToolBar(
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    totalCount: Int,
    toolbarViewModel: ToolbarViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
) {
    LaunchedEffect(key1 = selectedNodes.size) {
        toolbarViewModel.updateToolbarState(
            selectedNodes = selectedNodes,
            resultCount = totalCount,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
    }
    val toolbarState by toolbarViewModel.state.collectAsStateWithLifecycle()
    SearchToolbarBody(
        searchQuery = searchQuery,
        updateSearchQuery = updateSearchQuery,
        selectedNodes = selectedNodes,
        menuActions = toolbarState.menuActions,
        onBackPressed = onBackPressed
    )
}

@Composable
private fun SearchToolbarBody(
    searchQuery: String,
    menuActions: List<MenuAction>,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    onBackPressed: () -> Unit,
) {
    if (selectedNodes.isNotEmpty()) {
        SelectModeAppBar(
            title = "${selectedNodes.size}",
            actions = menuActions,
            onNavigationPressed = { onBackPressed() }
        )
    } else {
        ExpandedSearchAppBar(
            text = searchQuery,
            hintId = R.string.hint_action_search,
            onSearchTextChange = { updateSearchQuery(it) },
            onCloseClicked = { onBackPressed() },
            elevation = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbarBody() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SearchToolbarBody(
            searchQuery = "searchQuery",
            updateSearchQuery = {},
            onBackPressed = {},
            selectedNodes = emptySet(),
            menuActions = emptyList()
        )
    }
}