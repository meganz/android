package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove share menu action
 */
class RemoveShareMenuAction @Inject constructor(
    override val orderInCategory: Int,
) : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.FolderGear01)

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_clean_shares_menu)


    override val testTag: String = "menu_action:remove_share"
}