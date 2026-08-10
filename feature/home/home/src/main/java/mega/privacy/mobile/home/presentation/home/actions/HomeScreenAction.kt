package mega.privacy.mobile.home.presentation.home.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack

sealed interface HomeScreenAction : MenuAction {

    data object Customize : MenuActionWithIcon {
        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SlidersVertical02)

        override val testTag = "home_screen_action:customize"

        @Composable
        override fun getDescription() = "Customize"
    }
}
