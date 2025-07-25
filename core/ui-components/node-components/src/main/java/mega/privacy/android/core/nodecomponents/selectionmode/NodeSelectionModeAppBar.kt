package mega.privacy.android.core.nodecomponents.selectionmode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.TopAppBarActionWithClick
import java.util.Locale

@Composable
fun NodeSelectionModeAppBar(
    count: Int,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
        title = String.format(Locale.ROOT, "%s", count),
        actions = listOf(
            TopAppBarActionWithClick(
                onClick = onSelectAllClicked,
                topAppBarAction = SelectAll
            )
        )
    )
}

data object SelectAll : TopAppBarAction {
    override val testTag: String
        get() = "node_selection_mode_app_bar:select_all"

    @Composable
    override fun getDescription() = "Select all" // TODO

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
}