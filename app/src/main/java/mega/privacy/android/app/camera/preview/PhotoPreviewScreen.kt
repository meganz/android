package mega.privacy.android.app.camera.preview

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import mega.privacy.android.app.camera.CameraPreviewScreen
import mega.privacy.android.app.camera.PreviewViewModel

@Composable
internal fun PhotoPreviewScreen(
    uri: Uri,
    onBackPressed: () -> Unit,
    onSendPhoto: (Uri) -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    CameraPreviewScreen(
        uri = uri,
        onBackPressed = onBackPressed,
        onSend = onSendPhoto,
        viewModel = viewModel,
    ) { modifier ->
        PreviewPhotoSection(
            modifier = modifier.fillMaxSize(),
            uri = uri,
        )
    }
}

@Composable
private fun PreviewPhotoSection(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = uri,
        contentDescription = "Preview Photo",
        modifier = modifier.testTag(TEST_TAG_PHOTO_PREVIEW_IMAGE)
    )
}

internal const val TEST_TAG_PHOTO_PREVIEW_IMAGE = "preview_photo_section:image"