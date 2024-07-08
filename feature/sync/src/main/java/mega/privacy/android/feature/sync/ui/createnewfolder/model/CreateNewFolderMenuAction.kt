package mega.privacy.android.feature.sync.ui.createnewfolder.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Create New Folder menu action
 */
class CreateNewFolderMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        painterResource(id = R.drawable.ic_folder_plus_01_medium_regular_outline)

    @Composable
    override fun getDescription() =
        stringResource(id = sharedR.string.general_menu_create_new_folder)

    override val testTag: String
        get() = "menu_action:create_new_folder"
}