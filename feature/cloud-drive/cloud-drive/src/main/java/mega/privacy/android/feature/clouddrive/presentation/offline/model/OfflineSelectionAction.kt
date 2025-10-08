package mega.privacy.android.feature.clouddrive.presentation.offline.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface OfflineSelectionAction {
    data object Download : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Download)

        override val testTag: String = "offline_selection_action:download"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_save_to_device)
    }

    data object Share : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)

        override val testTag: String = "offline_selection_action:share"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.general_share)
    }

    data object Delete : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

        override val testTag: String = "offline_selection_action:delete"

        @Composable
        override fun getDescription(): String =
            stringResource(R.string.offline_screen_selection_menu_remove_from_offline)
    }

    data object SelectAll : MenuActionWithIcon {
        override val testTag: String = "node_selection_action:select_all"

        @Composable
        override fun getDescription() = stringResource(sharedR.string.action_select_all)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    companion object {
        val topBarItems = listOf(SelectAll)
        val bottomBarItems = listOf(Download, Share, Delete)
    }
}