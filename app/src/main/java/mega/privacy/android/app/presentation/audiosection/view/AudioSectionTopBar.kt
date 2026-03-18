package mega.privacy.android.app.presentation.audiosection.view

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import mega.privacy.android.legacy.core.ui.controls.appbar.CollapsedSearchAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar

@Composable
fun AudioSectionTopBar(
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    onSearchClicked: () -> Unit,
    onBackPressed: () -> Unit,
    menuItems: List<ToolbarMenuItem>,
    handler: NodeActionHandler,
    navHostController: NavHostController,
    clearSelection: () -> Unit,
    selectAllAction: () -> Unit = { },
) {
    val coroutineScope = rememberCoroutineScope()
    when {
        isActionMode -> {
            val actions = menuItems.map {
                val actionClick = it.control(
                    clearSelection,
                    handler::handleAction,
                    navHostController,
                    coroutineScope
                )
                MenuActionWithClick(
                    menuAction = it.action,
                    onClick = {
                        if (it.action is SelectAllMenuAction) {
                            selectAllAction()
                        } else {
                            actionClick()
                        }
                    }
                )
            }
            SelectModeAppBar(
                modifier = Modifier.testTag(AUDIO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG),
                title = "$selectedSize",
                actions = actions,
                onNavigationPressed = onBackPressed
            )
        }

        else -> CollapsedSearchAppBar(
            modifier = Modifier.testTag(AUDIO_SECTION_SEARCH_TOP_BAR_TEST_TAG),
            onBackPressed = onBackPressed,
            onSearchClicked = onSearchClicked,
            elevation = false,
            title = title,
            windowInsets = WindowInsets(0.dp)
        )
    }
}

/**
 * Test tag for search top bar
 */
const val AUDIO_SECTION_SEARCH_TOP_BAR_TEST_TAG = "audio_section_view:top_bar_search"

/**
 * Test tag for selected mode top bar
 */
const val AUDIO_SECTION_SELECTED_MODE_TOP_BAR_TEST_TAG = "audio_section_view:top_bar_selected_mode"