package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium

/**
 * Top app bar for file info screen
 * @param tintColor tint for title, back button and actions
 * @param opacityTransitionDelta determines if the bar background is fully visible (1.0) or transparent (0f, to show the header below) or the transition in between
 */
@Composable
internal fun FileInfoTopBar(
    title: String,
    actions: List<FileInfoMenuAction>,
    tintColor: Color,
    opacityTransitionDelta: Float,
    modifier: Modifier = Modifier,
    titleDisplacement: Dp = 0.dp,
    titleAlpha: Float = 1f,
    onBackPressed: () -> Unit,
    onActionClick: (FileInfoMenuAction) -> Unit,
    enabled: Boolean = true,
) {
    val backgroundAlpha = (opacityTransitionDelta * 10).coerceIn(0f, 1f)
    val elevationFactor = ((opacityTransitionDelta - 0.1f) * 2).coerceIn(0f, 1f)
    val elevation = (AppBarDefaults.TopAppBarElevation.value * elevationFactor).dp
    val systemUiController = rememberSystemUiController()
    val statusColor = MaterialTheme.colors.surface
        .surfaceColorAtElevation(absoluteElevation = elevation)
        .copy(alpha = backgroundAlpha)
    systemUiController.setStatusBarColor(
        color = statusColor,
        darkIcons = systemUiController.statusBarDarkContentEnabled
    )
    TopAppBar(
        title = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(titleAlpha)
                    .padding(top = titleDisplacement * 2), //double because it's centered
                text = title,
                style = MaterialTheme.typography.subtitle1medium.copy(color = tintColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed, enabled = enabled) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = tintColor,
                )
            }
        },
        actions = {
            FileInfoMenuActions(
                actions = actions,
                onActionClick = onActionClick,
                enabled = enabled,
                tint = tintColor
            )
        },
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = backgroundAlpha),
        elevation = elevation,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun Color.surfaceColorAtElevation(
    absoluteElevation: Dp,
): Color = LocalElevationOverlay.current?.apply(this, absoluteElevation) ?: this

@CombinedTextAndThemePreviews
@Composable
private fun FileInfoTopBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoTopBar(
            title = "Title",
            actions = listOf(FileInfoMenuAction.Move),
            tintColor = MaterialTheme.colors.onSurface,
            opacityTransitionDelta = 1f,
            onBackPressed = {},
            onActionClick = {},
        )
    }
}