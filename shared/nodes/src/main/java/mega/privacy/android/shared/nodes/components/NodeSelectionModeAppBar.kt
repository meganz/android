package mega.privacy.android.shared.nodes.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.selection.NodeSelection
import mega.privacy.android.shared.resources.R as sharedResR
import java.text.NumberFormat

/**
 * Selection-mode app bar driven by a [NodeSelection].
 *
 * @param total the folder's total number of nodes when the list is paged and more may exist than
 * are loaded; the title then reads "count of total" so the user sees what select-all covers
 */
@Composable
fun NodeSelectionModeAppBar(
    selection: NodeSelection<*>,
    modifier: Modifier = Modifier,
    total: Int? = null,
) {
    NodeSelectionModeAppBar(
        count = selection.selectedCount,
        isAllSelected = selection.isAllSelected,
        isSelecting = selection.isSelecting,
        onSelectAllClicked = selection::selectAll,
        onCancelSelectionClicked = selection::deselectAll,
        modifier = modifier,
        total = total,
    )
}

@Composable
fun NodeSelectionModeAppBar(
    count: Int,
    isAllSelected: Boolean,
    isSelecting: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    modifier: Modifier = Modifier,
    total: Int? = null,
) {
    MegaTopAppBar(
        modifier = modifier,
        navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
        title = when {
            isSelecting -> stringResource(sharedResR.string.app_bar_selection_mode_description)
            total != null -> stringResource(
                sharedResR.string.app_bar_selection_mode_count_of_total,
                formatCount(count),
                formatCount(total),
            )

            else -> formatCount(count)
        },
        actions = buildList {
            if (isSelecting) {
                add(CommonMenuAction.Selecting)
            } else if (!isAllSelected) {
                add(CommonMenuAction.SelectAll)
            }
        },
        onActionPressed = {
            when (it) {
                is CommonMenuAction.SelectAll -> onSelectAllClicked()
                else -> Unit
            }
        }
    )
}

/** Grouped for the current locale, "213,456" in English, so large folder totals stay readable */
@Composable
private fun formatCount(value: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale) { NumberFormat.getIntegerInstance(locale).format(value) }
}
