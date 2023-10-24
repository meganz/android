package mega.privacy.android.core.ui.controls.appbar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mega.privacy.android.core.R
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
 * @param onActionPressed Action for each available option.
 * @param elevation Elevation.
 */
@Composable
fun SelectModeAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: List<MenuAction> = emptyList(),
    onNavigationPressed: (() -> Unit)? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) = CompositionLocalProvider(
    LocalMegaAppBarTint provides
            MegaAppBarTint(MegaTheme.colors.icon.accent, MegaTheme.colors.text.accent)
) {
    BaseMegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = title,
        modifier = modifier,
        onNavigationPressed = onNavigationPressed,
        actions = actions,
        onActionPressed = onActionPressed,
        elevation = elevation
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
    val item1 = object : MenuActionString(R.drawable.ic_down, R.string.cancel, "cancel") {}
    val item2 = object : MenuActionString(R.drawable.ic_menu, R.string.action_long, "menu") {}
    val item3 =
        object : MenuActionString(R.drawable.ic_chevron_up, R.string.action_long, "chevron up") {}
    val item4 =
        object : MenuActionString(R.drawable.ic_alert_circle, R.string.action_long, "circle") {}
    val item5 = object : MenuActionWithoutIcon(R.string.password_text, "password") {}
    return listOf(item1, item2, item3, item4, item5)
}
