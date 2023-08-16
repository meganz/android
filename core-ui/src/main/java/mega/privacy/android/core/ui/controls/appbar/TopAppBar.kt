package mega.privacy.android.core.ui.controls.appbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top app bar with title and optional subtitle.
 */
@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    elevation: Boolean,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    if (subtitle == null) {
        SimpleTopAppBar(
            modifier,
            title,
            elevation,
            isEnabled,
            onBackPressed
        )
    } else {
        SimpleTopAppBarWithSubtitle(
            modifier,
            title,
            subtitle,
            elevation,
            isEnabled,
            onBackPressed
        )
    }
}