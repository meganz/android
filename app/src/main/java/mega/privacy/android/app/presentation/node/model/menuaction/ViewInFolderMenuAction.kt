package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon

/**
 * View in folder menu action
 */
class ViewInFolderMenuAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_upload_pick_file)

    @Composable
    override fun getDescription() = stringResource(id = R.string.view_in_folder_label)

    override val orderInCategory = 60
    override val testTag: String
        get() = "menu_action:view_in_folder"
}