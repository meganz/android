package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import mega.privacy.android.app.presentation.node.view.toolbar.NodeToolbarViewModel
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRoute
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.legacy.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Search toolbar used in search activity
 *
 * @param searchQuery
 * @param updateSearchQuery
 * @param selectedNodes
 * @param totalCount
 * @param onBackPressed
 * @param navHostController
 * @param nodeActionHandler
 * @param clearSelection
 * @param nodeSourceType
 */
@Composable
fun SearchToolBar(
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    totalCount: Int,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    clearSelection: () -> Unit,
    nodeSourceType: NodeSourceType,
    navigationLevel: List<Pair<Long, String>>,
    toolbarViewModel: NodeToolbarViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = selectedNodes.size) {
        toolbarViewModel.updateToolbarState(
            selectedNodes = selectedNodes,
            resultCount = totalCount,
            nodeSourceType = nodeSourceType
        )
    }
    val toolbarState by toolbarViewModel.state.collectAsStateWithLifecycle()
    SearchToolbarBody(
        searchQuery = searchQuery,
        updateSearchQuery = updateSearchQuery,
        selectedNodes = selectedNodes,
        menuActions = toolbarState.toolbarMenuItems,
        onBackPressed = onBackPressed,
        navHostController = navHostController,
        handler = nodeActionHandler,
        clearSelection = clearSelection,
        navigationLevel = navigationLevel.lastOrNull(),
        nodeSourceType = nodeSourceType,
    )
}

@Composable
private fun SearchToolbarBody(
    searchQuery: String,
    menuActions: List<ToolbarMenuItem>,
    updateSearchQuery: (String) -> Unit,
    selectedNodes: Set<TypedNode>,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    handler: NodeActionHandler,
    clearSelection: () -> Unit,
    navigationLevel: Pair<Long, String>?,
    nodeSourceType: NodeSourceType,
) {
    val scope = rememberCoroutineScope()
    var enteredSelectionMode by remember { mutableStateOf(false) }
    LaunchedEffect(selectedNodes.isEmpty()) {
        if (selectedNodes.isNotEmpty()) {
            enteredSelectionMode = true
        }
    }

    if (selectedNodes.isNotEmpty()) {
        val actions = menuActions.map {
            MenuActionWithClick(
                menuAction = it.action,
                onClick = it.control(
                    clearSelection,
                    handler::handleAction,
                    navHostController,
                    scope
                )
            )
        }
        SelectModeAppBar(
            title = "${selectedNodes.size}",
            actions = actions,
            onNavigationPressed = { onBackPressed() }
        )
    } else {
        val moreAction = object : MenuActionWithIcon {
            @Composable
            override fun getIconPainter() = painterResource(id = R.drawable.ic_more)

            @Composable
            override fun getDescription() = ""
            override val testTag: String = "moreAction"
        }
        if (navigationLevel?.second?.isNotEmpty() == true) {
            MegaAppBar(
                title = navigationLevel.second,
                appBarType = AppBarType.BACK_NAVIGATION,
                elevation = 0.dp,
                onNavigationPressed = {
                    onBackPressed()
                },
                actions = listOf(moreAction),
                onActionPressed = {
                    navHostController.navigate(
                        route = nodeBottomSheetRoute.plus("/${navigationLevel.first}")
                            .plus("/${nodeSourceType.name}")
                    )
                },
            )
        } else {
            ExpandedSearchAppBar(
                text = searchQuery,
                hintId = R.string.hint_action_search,
                onSearchTextChange = { updateSearchQuery(it) },
                onCloseClicked = { onBackPressed() },
                elevation = false,
                isHideAfterSearch = true,
                transparentBackground = true,
                shouldAutoFocus = !enteredSelectionMode
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbarBody() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SearchToolbarBody(
            searchQuery = "searchQuery",
            menuActions = emptyList(),
            updateSearchQuery = {},
            selectedNodes = emptySet(),
            onBackPressed = {},
            navHostController = NavHostController(LocalContext.current),
            handler = NodeActionHandler(
                LocalContext.current as SearchActivity,
                hiltViewModel(),
            ),
            clearSelection = {},
            navigationLevel = null,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )
    }
}