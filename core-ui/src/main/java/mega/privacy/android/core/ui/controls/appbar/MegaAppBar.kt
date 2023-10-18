package mega.privacy.android.core.ui.controls.appbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.badge
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.utils.composeLet

/**
 * Mega app bar.
 *
 * This component has been implemented following the latest rules for the new design system.
 * If this component does not serve your purpose, discuss this with the team in charge of
 * developing the new design system. It will be updated or override depending on the needs.
 *
 * @param appBarType [AppBarType]
 * @param title Title.
 * @param modifier [Modifier]
 * @param onNavigationPressed Action for navigation button.
 * @param badgeCount Count if should show a badge, null otherwise.
 * @param titleIcons Icons to show at the end of the title if any, null otherwise.
 * @param subtitle Subtitle.
 * @param actions Available options.
 * @param onActionPressed Action for each available option.
 * @param elevation Elevation.
 */
@Composable
fun MegaAppBar(
    appBarType: AppBarType,
    title: String,
    modifier: Modifier = Modifier,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    subtitle: String? = null,
    actions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) = TopAppBar(
    title = {
        Column {
            Row {
                Text(
                    text = title,
                    maxLines = 1,
                    color = MegaTheme.colors.text.primary,
                    style = MaterialTheme.typography.subtitle1
                )
                titleIcons?.let { it() }
            }
            subtitle?.let {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    color = MegaTheme.colors.text.secondary,
                    style = MaterialTheme.typography.body4
                )
            }
        }
    },
    modifier = modifier,
    navigationIcon = appBarType.takeIf { it != AppBarType.NONE }?.composeLet {
        Box {
            NavigationIcon(
                onNavigationPressed = onNavigationPressed ?: {},
                iconId = when (appBarType) {
                    AppBarType.BACK_NAVIGATION -> R.drawable.ic_back
                    AppBarType.MENU -> R.drawable.ic_menu
                    else -> return@Box
                }
            )
            badgeCount?.let {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(24.dp)
                        .padding(3.dp)
                        .background(color = MegaTheme.colors.indicator.pink, shape = CircleShape)
                        .border(
                            2.dp,
                            MegaTheme.colors.background.pageBackground,
                            shape = CircleShape
                        )
                        .align(Alignment.TopEnd),
                ) {
                    Text(
                        text = it.toString(),
                        modifier = Modifier.align(Alignment.Center),
                        color = MegaTheme.colors.text.inverse,
                        style = MaterialTheme.typography.badge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    },
    actions = {
        actions?.let {
            MenuActions(
                actions = actions,
                maxActionsToShow = 4,
                onActionClick = { action -> onActionPressed?.invoke(action) }
            )
        }
    },
    elevation = elevation
)

@Composable
private fun NavigationIcon(
    onNavigationPressed: (() -> Unit),
    @DrawableRes iconId: Int,
) = IconButton(onClick = onNavigationPressed) {
    Icon(
        imageVector = ImageVector.vectorResource(id = iconId),
        contentDescription = "Navigation button",
        tint = MegaTheme.colors.icon.primary
    )
}

@CombinedThemePreviews
@Composable
private fun MegaAppBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaAppBar(
            appBarType = AppBarType.MENU,
            title = "App bar title",
            badgeCount = 3,
            subtitle = "Subtitle"
        )
    }
}

/**
 * Enum class defining the app bar types.
 */
enum class AppBarType {
    /**
     * Back Navigation
     */
    BACK_NAVIGATION,

    /**
     * Menu
     */
    MENU,

    /**
     * None
     */
    NONE
}