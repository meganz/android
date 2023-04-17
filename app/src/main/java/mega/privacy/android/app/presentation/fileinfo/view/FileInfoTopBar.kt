package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState

/**
 * Top app bar for file info screen
 * @param tintColor tint for title, back button and actions
 * @param opacityTransitionDelta determines if the bar background is fully visible (1.0) or transparent (0f, to show the header below) or the transition in between
 */
@Composable
internal fun FileInfoTopBar(
    viewState: FileInfoViewState,
    tintColor: Color,
    opacityTransitionDelta: Float,
    onBackPressed: () -> Unit,
    onActionClick: (FileInfoMenuAction) -> Unit,
) {
    val backgroundAlpha = (opacityTransitionDelta * 10).coerceIn(0f, 1f)
    val elevationFactor = ((opacityTransitionDelta - 0.1f) * 2).coerceIn(0f, 1f)
    TopAppBar(
        title = {
            Text(
                text = viewState.title,
                style = MaterialTheme.typography.subtitle1.copy(color = tintColor),
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = tintColor,
                )
            }
        },
        actions = {
            FileInfoMenuActions(
                viewState = viewState,
                tint = tintColor,
                onActionClick = onActionClick
            )
        },
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = backgroundAlpha),
        elevation = (AppBarDefaults.TopAppBarElevation.value * elevationFactor).dp,
        modifier = Modifier.fillMaxWidth()
    )
}