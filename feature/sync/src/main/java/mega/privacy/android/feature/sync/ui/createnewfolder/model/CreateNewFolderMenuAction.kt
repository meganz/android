package mega.privacy.android.feature.sync.ui.createnewfolder.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Create New Folder menu action
 */
internal class CreateNewFolderMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.FolderPlus01)

    @Composable
    override fun getDescription() =
        stringResource(id = sharedR.string.general_menu_create_new_folder)

    override val testTag: String
        get() = "menu_action:create_new_folder"
}
