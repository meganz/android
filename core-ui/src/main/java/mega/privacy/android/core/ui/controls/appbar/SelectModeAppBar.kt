package mega.privacy.android.core.ui.controls.appbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Select mode app bar
 *
 * @param title
 * @param modifier [Modifier]
 * @param onNavigationPressed Action for navigation button.
 * @param actions Available options.
 * @param onActionClick Action for each available option.
 * @param elevation Elevation.
 */
@Composable
fun SelectModeAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: List<MenuAction> = emptyList(),
    onNavigationPressed: (() -> Unit)? = null,
    onActionClick: ((MenuAction) -> Unit)? = null,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) = TopAppBar(
    modifier = modifier,
    title = {
        Text(
            text = title,
            maxLines = 1,
            color = MegaTheme.colors.text.accent,
            style = MaterialTheme.typography.subtitle1
        )
    },
    navigationIcon = {
        NavigationIcon(
            onNavigationPressed = { onNavigationPressed?.invoke() },
            iconId = R.drawable.ic_back,
            modifier = Modifier.testTag(SELECT_MODE_APP_BAR_BACK_BUTTON_TAG)
        )
    },
    actions = {
        MenuActions(
            actions = actions,
            maxActionsToShow = 4,
            onActionClick = { action -> onActionClick?.invoke(action) },
            selectMode = true,
        )
    },
    elevation = elevation,
)

internal const val SELECT_MODE_APP_BAR_BACK_BUTTON_TAG = "select_mode_appbar:button_back"

@Composable
private fun NavigationIcon(
    onNavigationPressed: (() -> Unit),
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
) = IconButton(onClick = onNavigationPressed) {
    Icon(
        imageVector = ImageVector.vectorResource(id = iconId),
        contentDescription = "Navigation button",
        tint = MegaTheme.colors.icon.accent,
        modifier = modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun SelectModeAppBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SelectModeAppBar(
            title = "App bar title",
            actions = getSampleToolbarActions()
        )
    }
}

private fun getSampleToolbarActions(): List<MenuAction> {
    val item1 = object : MenuActionString(R.drawable.ic_down, R.string.cancel) {}
    val item2 = object : MenuActionString(R.drawable.ic_menu, R.string.action_long) {}
    val item3 = object : MenuActionString(R.drawable.ic_chevron_up, R.string.action_long) {}
    val item4 = object : MenuActionString(R.drawable.ic_alert_circle, R.string.action_long) {}
    val item5 = object : MenuActionWithoutIcon(R.string.password_text) {}
    return listOf(item1, item2, item3, item4, item5)
}
