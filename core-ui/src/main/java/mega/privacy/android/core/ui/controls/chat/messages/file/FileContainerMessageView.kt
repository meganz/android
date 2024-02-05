package mega.privacy.android.core.ui.controls.chat.messages.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * File message container, utility composable to animate the content size and apply load overly to file previews
 * @param loadProgress if set, a linear progress indicator and an overly will be shown
 * @param modifier
 * @param onClick handle click when file message is clicked
 * @param content composable function to draw the content: [FileNoPreviewMessageView] or an [Image] with the preview
 *
 */
@Composable
internal fun FileContainerMessageView(
    loadProgress: Float?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {

    // the size of loading overlay is calculated later
    var width by remember { mutableStateOf(0.dp) }
    var height by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MegaTheme.colors.background.surface2)
            .testTag(FILE_MESSAGE_VIEW_ROOT_TEST_TAG)
            .onGloballyPositioned {
                width = with(density) { it.size.width.toDp() }
                height = with(density) { it.size.height.toDp() }
            }
    ) {
        content()
        if ((loadProgress ?: 1f) < 1f) {
            Box(
                modifier = Modifier
                    .size(width, height)
            ) {
                LoadOverlay(modifier = Modifier.testTag(FILE_MESSAGE_VIEW_OVERLAY_TEST_TAG))
                MegaLinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .testTag(FILE_MESSAGE_VIEW_PROGRESS_TEST_TAG),
                    progress = loadProgress
                )
            }
        }
    }
}

@Composable
private fun LoadOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.5f),
            ),
    )
}

/**
 * This preview are useful to check the behaviour of the component depending on intrinsic size of the content
 */
@CombinedThemePreviews
@Composable
private fun FileMessageViewLoadingPreview(
    @PreviewParameter(PreviewContainerProvider::class) params: PreviewContainerParams,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val painter = rememberVectorPainter(image = image(params.width, params.height))
        FileContainerMessageView(
            modifier = Modifier.padding(12.dp),
            content = {
                Image(
                    painter = painter,
                    contentDescription = "Image",
                    contentScale = ContentScale.Inside,
                )
            },
            loadProgress = params.progress,
        )
    }
}

private class PreviewContainerProvider : PreviewParameterProvider<PreviewContainerParams> {
    override val values = listOf(
        1f, 0.6f
    ).flatMap {
        listOf(
            PreviewContainerParams(60.dp, 50.dp, it),
            PreviewContainerParams(212.dp, 212.dp, it),
            PreviewContainerParams(412.dp, 212.dp, it),
            PreviewContainerParams(212.dp, 412.dp, it),
        )
    }.asSequence()
}

private data class PreviewContainerParams(val width: Dp, val height: Dp, val progress: Float = 1f)

private fun image(with: Dp, height: Dp) = ImageVector.Builder(
    defaultWidth = with,
    defaultHeight = height,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    this.addPath(createPath(), fill = SolidColor(Color.Red))
}.build()

private fun createPath(): List<PathNode> {
    return listOf(
        PathNode.MoveTo(0f, 12f),
        PathNode.LineTo(12f, 0f),
        PathNode.LineTo(24f, 12f),
        PathNode.LineTo(12f, 24f),
        PathNode.Close
    )
}

internal const val FILE_MESSAGE_VIEW_ROOT_TEST_TAG = "chat_file_message_view:root_view"
internal const val FILE_MESSAGE_VIEW_OVERLAY_TEST_TAG =
    "chat_file_preview_message_view:overlay"
internal const val FILE_MESSAGE_VIEW_PROGRESS_TEST_TAG =
    "chat_file_preview_message_view:preview"