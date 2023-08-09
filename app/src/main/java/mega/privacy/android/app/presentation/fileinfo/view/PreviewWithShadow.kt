package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.grey_alpha_026

@Composable
internal fun PreviewWithShadow(
    previewUri: String,
    alpha: Float,
    modifier: Modifier = Modifier,
) {

    Image(
        modifier = modifier
            .testTag(TEST_TAG_PREVIEW)
            .fillMaxSize()
            .alpha(alpha),
        painter = rememberAsyncImagePainter(model = previewUri),
        contentDescription = "Preview",
        contentScale = ContentScale.FillWidth,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        //shadow top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .fillMaxHeight(0.5f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            grey_alpha_026,
                            Color.Transparent,
                        )
                    )
                ),
        )
        //shadow bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            grey_alpha_026,
                        )
                    )
                )
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewWithShadowPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.height(120.dp)) {
            PreviewWithShadow(previewUri = "a", alpha = 1f)
        }
    }
}

internal const val TEST_TAG_PREVIEW = "preview_with_shadow:image_preview"