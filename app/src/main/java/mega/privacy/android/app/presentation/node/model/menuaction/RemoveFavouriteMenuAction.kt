package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove favourite menu action
 */
class RemoveFavouriteMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        painterResource(id = iconPackR.drawable.ic_menu_remove_favourite)

    @Composable
    override fun getDescription() = stringResource(id = R.string.file_properties_unfavourite)

    override val orderInCategory: Int
        get() = 80
    override val testTag: String
        get() = "menu_action:remove_favourite"
}