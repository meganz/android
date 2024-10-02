package mega.privacy.android.shared.original.core.ui.controls.appbar

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionString
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithClick
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
fun SelectModeAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: List<MenuActionWithClick>? = emptyList(),
    onNavigationPressed: (() -> Unit)? = null,
    elevation: Dp = LocalMegaAppBarElevation.current,
) = CompositionLocalProvider(
    LocalMegaAppBarColors provides
            MegaAppBarColors(
                MegaOriginalTheme.colors.icon.accent,
                MegaOriginalTheme.colors.text.accent
            )
) {
    BaseMegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = { MegaAppBarTitle(title) },
        modifier = modifier,
        onNavigationPressed = onNavigationPressed,
        actions = actions,
        elevation = elevation
    )
}

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
    elevation: Dp = LocalMegaAppBarElevation.current,
) = SelectModeAppBar(
    title = title,
    modifier = modifier,
    actions = actions.addClick(onActionPressed),
    onNavigationPressed = onNavigationPressed,
    elevation = elevation
    )

@CombinedThemePreviews
@Composable
private fun SelectModeAppBarPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SelectModeAppBar(
            title = "App bar title",
            actions = getSampleToolbarActions()
        )
    }
}

private fun getSampleToolbarActions(): List<MenuAction> {
    val item1 = object : MenuActionString(R.drawable.ic_down, R.string.password_text, "cancel") {}
    val item2 = object : MenuActionString(R.drawable.ic_menu, R.string.password_text, "menu") {}
    val item3 =
        object : MenuActionString(R.drawable.ic_chevron_up, R.string.password_text, "chevron up") {}
    val item4 =
        object : MenuActionString(
            iconPackR.drawable.ic_alert_circle_regular_medium_outline,
            R.string.password_text,
            "circle"
        ) {}
    val item5 = object : MenuActionWithoutIcon(R.string.password_text, "password") {}
    return listOf(item1, item2, item3, item4, item5)
}
