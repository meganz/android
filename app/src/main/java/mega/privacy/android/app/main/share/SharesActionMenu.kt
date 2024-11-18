package mega.privacy.android.app.main.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

internal sealed interface SharesActionMenu : MenuActionWithIcon {
    data object More : SharesActionMenu {
        @Composable
        override fun getIconPainter(): Painter = painterResource(id = R.drawable.ic_more)

        @Composable
        override fun getDescription(): String = "More"

        override val orderInCategory = 2
        override val testTag = "shares_view:action_more"
    }

    data object Search : SharesActionMenu {
        @Composable
        override fun getIconPainter(): Painter =
            painterResource(id = mega.privacy.android.legacy.core.ui.R.drawable.ic_search)

        @Composable
        override fun getDescription(): String = "Search"

        override val orderInCategory = 1
        override val testTag = "shares_view:action_search"
    }
}