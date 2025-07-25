package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.shared.resources.R as SharedResR
import javax.inject.Inject

/**
 * Slideshow menu action
 */
class SlideshowMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_slideshow)

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.action_slideshow)

    override val orderInCategory: Int
        get() = 110
    override val testTag: String
        get() = "menu_action:slideshow"
}