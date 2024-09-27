package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.PhotoState

@Composable
internal fun ImageContent(
    fullSizePath: String?,
    errorImagePath: String?,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    enableZoom: Boolean,
    isMagnifierMode: Boolean,
    onDragMagnifier: (Boolean) -> Unit,
) {
    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        enableZoom = enableZoom,
        isMagnifierMode = isMagnifierMode,
        onTap = {
            onImageTap()
        },
        onDragMagnifier = onDragMagnifier,
    ) {
        var imagePath by remember(fullSizePath) {
            mutableStateOf(fullSizePath)
        }

        val request = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .listener(
                onError = { _, _ ->
                    // when some image full size picture decoder throw exception, use preview/thumbnail instead
                    // detail see package coil.decode [BitmapFactoryDecoder] 79 line
                    imagePath = errorImagePath
                }
            )
            .crossfade(true)
            .build()
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}