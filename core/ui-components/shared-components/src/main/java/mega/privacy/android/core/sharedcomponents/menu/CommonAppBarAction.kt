package mega.privacy.android.core.sharedcomponents.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack

sealed interface CommonAppBarAction : MenuActionWithIcon {
    data object Search : MenuActionWithIcon {
        override val testTag: String = "app_bar:search"

        @Composable
        override fun getDescription() = "Search"

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchSmall)
    }

    data object More : MenuActionWithIcon {
        override val testTag: String = "app_bar:more"

        @Composable
        override fun getDescription() = "More"

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }
}