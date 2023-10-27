package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon

/**
 * Open location menu action
 */
class OpenLocationMenuAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_upload_pick_file)

    @Composable
    override fun getDescription() = stringResource(id = R.string.search_open_location)

    override val orderInCategory: Int
        get() = 130
    override val testTag: String
        get() = "menu_action:open_location"
}