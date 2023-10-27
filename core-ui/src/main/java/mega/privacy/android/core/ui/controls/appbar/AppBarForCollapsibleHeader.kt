package mega.privacy.android.core.ui.controls.appbar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import mega.privacy.android.core.ui.controls.layouts.CollapsibleHeaderWithTitle
import mega.privacy.android.core.ui.controls.layouts.LocalCollapsibleHeaderTitleTransition
import mega.privacy.android.core.ui.controls.layouts.ScaffoldWithCollapsibleHeader
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * MegaAppBarForCollapsibleHeader
 *
 * Special appbar to be used in [ScaffoldWithCollapsibleHeader] together with [CollapsibleHeaderWithTitle] to have a collapsible title. Check ScaffoldWithCollapsibleHeader preview for an example
 *
 *  @param appBarType [AppBarType]
 *  @param title Title.
 *  @param modifier [Modifier]
 *  @param onNavigationPressed Action for navigation button.
 *  @param badgeCount Count if should show a badge, null otherwise.
 *  @param actions Available options.
 *  @param onActionPressed Action for each available option.
 *  @param maxActionsToShow The Max [actions] to be shown, if there are more they will be under three dots menu
 *  @param enabled if false, the navigation icon and actions will be disabled
 *  @param elevation Elevation.
 *
 */
@Composable
fun AppBarForCollapsibleHeader(
    appBarType: AppBarType,
    title: String,
    modifier: Modifier = Modifier,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    actions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 4,
    enabled: Boolean = true,
    elevation: Dp = LocalMegaAppBarElevation.current,
) = BaseMegaAppBar(
    appBarType = appBarType,
    title = {
        MegaAppBarTitle(
            title, modifier = Modifier
                .offset(y = LocalCollapsibleHeaderTitleTransition.current.offset)
                .alpha(1 - LocalCollapsibleHeaderTitleTransition.current.expandedAlpha)
        )
    },
    modifier = modifier,
    onNavigationPressed = onNavigationPressed,
    badgeCount = badgeCount,
    actions = actions,
    onActionPressed = onActionPressed,
    maxActionsToShow = maxActionsToShow,
    enabled = enabled,
    elevation = elevation
)

@CombinedThemePreviews
@Composable
private fun MegaAppBarForCollapsibleHeaderPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        //this component gets the colors from ScaffoldWithCollapsibleHeader, to preview it here we need to provide these colors
        CompositionLocalProvider(
            LocalMegaAppBarColors provides MegaAppBarColors(
                iconsTintColor = MegaTheme.colors.icon.primary,
                titleColor = MegaTheme.colors.text.primary,
                backgroundAlpha = 1f,
            )
        ) {
            AppBarForCollapsibleHeader(AppBarType.MENU, "Title")
        }
    }
}