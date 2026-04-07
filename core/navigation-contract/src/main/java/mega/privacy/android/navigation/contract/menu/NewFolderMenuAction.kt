package mega.privacy.android.navigation.contract.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Create New Folder menu action
 */
data object NewFolderMenuAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.FolderPlus01)

    @Composable
    override fun getDescription() =
        stringResource(id = sharedR.string.general_new_folder)

    override val testTag: String
        get() = "app_bar:create_new_folder"
}
