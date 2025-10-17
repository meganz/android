package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.icon.pack.IconPack
import javax.inject.Inject

class RemoveOfflineMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudOff)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.node_menu_action_remove_offline)

    override val orderInCategory: Int
        get() = 151

    override val testTag: String
        get() = "menu_action:remove_offline"
}