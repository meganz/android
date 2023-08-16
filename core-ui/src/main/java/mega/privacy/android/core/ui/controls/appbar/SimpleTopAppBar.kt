package mega.privacy.android.core.ui.controls.appbar

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
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Top app bar with title.
 */
@Composable
fun SimpleTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleId: Int,
    elevation: Boolean,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    SimpleTopAppBar(
        modifier,
        stringResource(titleId),
        elevation,
        isEnabled,
        onBackPressed
    )
}

@Composable
fun SimpleTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    elevation: Boolean,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
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

@CombinedThemePreviews
@Composable
private fun PreviewSimpleTopAppBar() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SimpleTopAppBar(titleId = 0,
            elevation = false,
            onBackPressed = {}
        )
    }
}
