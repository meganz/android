package mega.privacy.android.legacy.core.ui.controls.appbar

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * A [Composable] that displays a Top App Bar with a Title and Back Button
 *
 * @param titleId A [StringRes] used to display the Toolbar Title
 * @param elevation if true, Toolbar elevation is added. Otherwise, no elevation is added
 * @param modifier The [Modifier] class
 * @param isEnabled if true, enables the Back Button. Otherwise, it is disabled
 * @param onBackPressed Lambda that performs a specific action when a Back Press event is detected
 */
@Composable
fun SimpleTopAppBar(
    @StringRes titleId: Int,
    elevation: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    SimpleTopAppBar(
        title = stringResource(titleId),
        elevation = elevation,
        modifier = modifier,
        isEnabled = isEnabled,
        onBackPressed = onBackPressed,
    )
}

/**
 * Another variation of the [SimpleTopAppBar] Composable that accepts a plain [String] for the
 * Toolbar Title
 *
 * @param title The Toolbar Title
 * @param elevation if true, Toolbar elevation is added. Otherwise, no elevation is added
 * @param modifier The [Modifier] class
 * @param isEnabled if true, enables the Back Button. Otherwise, it is disabled
 * @param onBackPressed Lambda that performs a specific action when a Back Press event is detected
 */
@Composable
@Deprecated(
    message = "This component doesn't follow our design system correctly",
    replaceWith = ReplaceWith("MegaAppBar")
)
fun SimpleTopAppBar(
    title: String,
    elevation: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed, enabled = isEnabled) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

/**
 * A Preview Composable that displays the Top App Bar
 */
@CombinedThemePreviews
@Composable
private fun PreviewSimpleTopAppBar() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SimpleTopAppBar(titleId = 0,
            elevation = false,
            onBackPressed = {}
        )
    }
}

/**
 * A Preview Composable that simulates a very long Toolbar Title
 */
@CombinedThemePreviews
@Composable
private fun PreviewSimpleTopAppBarWithOverflowText() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SimpleTopAppBar(
            title = "This is a very long toolbar title that can overflow",
            elevation = false,
            onBackPressed = {},
        )
    }
}
