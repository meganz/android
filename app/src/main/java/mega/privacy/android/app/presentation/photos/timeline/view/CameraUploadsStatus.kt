package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.amber_400
import mega.privacy.android.core.ui.theme.blue_400
import mega.privacy.android.core.ui.theme.green_400

@Composable
fun CameraUploadsStatusSync(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = Color.Transparent,
        onClick = onClick,
        modifier = modifier,
        progress = 0f,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_sync),
                contentDescription = "Camera uploads status sync",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusUploading(
    modifier: Modifier = Modifier,
    progress: Float,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = blue_400,
        onClick = onClick,
        modifier = modifier,
        progress = progress,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_uploading),
                contentDescription = "Camera uploads status uploading",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusCompleted(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = green_400,
        onClick = onClick,
        modifier = modifier,
        progress = 1f,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_fab_status_completed),
                contentDescription = "Camera uploads status completed",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusWarning(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    progress: Float,
) {
    CameraUploadsStatus(
        loadingIndicatorColor = amber_400,
        onClick = onClick,
        modifier = modifier,
        progress = progress,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_warning),
                contentDescription = "Camera uploads status warning",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
private fun CameraUploadsStatus(
    modifier: Modifier = Modifier,
    loadingIndicatorColor: Color,
    onClick: () -> Unit = {},
    progress: Float = 0f,
    statusIcon: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        backgroundColor = MaterialTheme.colors.surface,
        content = {
            CircularProgressIndicator(
                progress = progress,
                color = loadingIndicatorColor,
                strokeWidth = 2.dp,
            )

            statusIcon()
        },
    )
}

@Preview
@Composable
fun PreviewCameraUploadsStatusSync() {
    CameraUploadsStatusSync(onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusUploading() {
    CameraUploadsStatusUploading(progress = 0.6f, onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusCompleted() {
    CameraUploadsStatusCompleted(onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusWarning() {
    CameraUploadsStatusWarning(progress = 0.4f, onClick = { /*TODO*/ })
}
