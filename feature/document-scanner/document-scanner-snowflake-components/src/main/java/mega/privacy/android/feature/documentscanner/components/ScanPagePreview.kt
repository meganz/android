package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Full-page preview of a scanned page, fit within the available space.
 *
 * @param imageUri URI of the full-resolution page image.
 * @param modifier Modifier for the preview.
 */
@Composable
fun ScanPagePreview(
    imageUri: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageUri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    )
}
