package mega.privacy.android.core.ui.controls.appbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A [Composable] inheriting from [SimpleTopAppBar] with an optional Toolbar Subtitle
 *
 * @param title The Toolbar Title
 * @param elevation if true, Toolbar elevation is added. Otherwise, no elevation is added
 * @param modifier The [Modifier] class
 * @param isEnabled if true, enables the Back Button. Otherwise, it is disabled
 * @param onBackPressed Lambda that performs a specific action when a Back Press event is detected
 */
@Deprecated(
    message = "This component doesn't follow our design system correctly",
    replaceWith = ReplaceWith("MegaAppBar")
)
@Composable
fun LegacyTopAppBar(
    title: String,
    elevation: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    if (subtitle == null) {
        SimpleTopAppBar(
            title = title,
            elevation = elevation,
            modifier = modifier,
            isEnabled = isEnabled,
            onBackPressed = onBackPressed,
        )
    } else {
        SimpleTopAppBarWithSubtitle(
            modifier = modifier,
            title = title,
            subtitle = subtitle,
            elevation = elevation,
            isEnabled = isEnabled,
            onBackPressed = onBackPressed,
        )
    }
}