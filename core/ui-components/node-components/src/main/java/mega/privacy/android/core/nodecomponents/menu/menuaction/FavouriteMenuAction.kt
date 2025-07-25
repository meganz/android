package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Favourite menu action
 */
class FavouriteMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Heart)

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.file_properties_favourite)

    override val orderInCategory: Int
        get() = 80
    override val testTag: String
        get() = "menu_action:favourite"
}