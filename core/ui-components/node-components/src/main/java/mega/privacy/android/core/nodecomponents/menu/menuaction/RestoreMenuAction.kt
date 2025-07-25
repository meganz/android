package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Restore menu action
 */
class RestoreMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.context_restore)

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.RotateCcw)

    override val orderInCategory = 250

    override val testTag: String = "menu_action:restore"
}