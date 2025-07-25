package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Verify contact menu action
 */
class VerifyMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Key02)

    @Composable
    override fun getDescription() =
        stringResource(id = SharedResR.string.shared_items_bottom_sheet_menu_verify_user)

    override val orderInCategory = 40

    override val testTag: String
        get() = "menu_action:verify"
}