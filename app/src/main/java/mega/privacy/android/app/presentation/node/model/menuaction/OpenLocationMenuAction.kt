package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Open location menu action
 */
class OpenLocationMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Regular.Outline.FolderOpen)

    @Composable
    override fun getDescription() = stringResource(id = R.string.search_open_location)

    override val orderInCategory: Int
        get() = 130
    override val testTag: String
        get() = "menu_action:open_location"
}