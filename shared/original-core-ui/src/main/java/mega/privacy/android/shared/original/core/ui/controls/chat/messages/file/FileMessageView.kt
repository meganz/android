package mega.privacy.android.shared.original.core.ui.controls.chat.messages.file

import mega.privacy.android.icon.pack.R as iconPackR
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * File message with or without preview
 *
 * @param isMe whether message is sent from me
 * @param fileTypeResId resource id of file type icon
 * @param previewUri uri of the file to be loaded, usually something like ["file://xxx.xxx".toUri()]
 * @param duration String representation of the duration of the file in case it's playable, null otherwise
 * @param modifier
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-1.
 * @param fileName name of file
 * @param fileSize size string of file. It can be "Uploading" when message is loading.
 * @param showPausedTransfersWarning whether the message needs to show a warning for paused transfers.
 */

@Composable
fun FileMessageView(
    isMe: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    duration: String?,
    modifier: Modifier = Modifier,
    loadProgress: Float? = null,
    fileName: String = "",
    fileSize: String = "",
    showPausedTransfersWarning: Boolean = false,
) {
    val noPreviewContent: @Composable () -> Unit = {
        FileNoPreviewMessageView(
            isMe,
            fileTypeResId,
            Modifier,
            fileName,
            fileSize,
        )
    }
    var startPadding = 0.dp

    Column {
        FileContainerMessageView(
            modifier = modifier,
            loadProgress = loadProgress,
        ) {
            if (previewUri != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .crossfade(true)
                        .data(previewUri)
                        .build(),
                    contentDescription = fileName,
                    contentScale = ContentScale.Inside,
                    loading = { noPreviewContent() },
                    error = { noPreviewContent() },
                    success = {
                        startPadding = 12.dp
                        val intrinsicSize = it.painter.intrinsicSize
                        val previewModifier = Modifier
                            .sizeIn(
                                maxWidth = MAX_SIZE.dp.coerceAtMostPixels(intrinsicSize.width),
                                maxHeight = MAX_SIZE.dp.coerceAtMostPixels(intrinsicSize.height),
                            )
                            .aspectRatio(intrinsicSize.aspectRatio())
                        Image(
                            painter = it.painter,
                            contentDescription = "Image",
                            contentScale = ContentScale.Inside,
                            modifier = previewModifier
                                .testTag(FILE_PREVIEW_MESSAGE_VIEW_IMAGE_TEST_TAG)
                        )
                        duration?.let {
                            PlayPreviewOverlay(
                                duration = duration,
                                modifier = previewModifier,
                            )
                        }
                    },
                )
            } else {
                noPreviewContent()
            }
        }

        if (showPausedTransfersWarning) {
            MegaText(
                modifier = Modifier.padding(
                    start = startPadding,
                    top = 2.dp
                ),
                text = stringResource(id = R.string.manual_resume_alert),
                style = MaterialTheme.typography.body4,
                textColor = TextColor.Primary
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FileNoPreviewMessageNotMineViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val context = LocalContext.current
        val resourceId = R.drawable.ic_check_circle
        //this uri will be loaded if the preview is running on the device
        val resourceUri = remember(resourceId) {
            with(context.resources) {
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(getResourcePackageName(resourceId))
                    .appendPath(getResourceTypeName(resourceId))
                    .appendPath(getResourceEntryName(resourceId))
                    .build()
            }
        }
        FileMessageView(
            isMe = false,
            fileTypeResId = iconPackR.drawable.ic_alert_circle_regular_medium_outline,
            previewUri = resourceUri,
            duration = null,
            fileName = "Hello.pdf",
            fileSize = "30 MB",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileNoPreviewMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isUploadPaused: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val context = LocalContext.current
        val resourceId = R.drawable.ic_check_circle
        //this uri will be loaded if the preview is running on the device
        val resourceUri = remember(resourceId) {
            with(context.resources) {
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(getResourcePackageName(resourceId))
                    .appendPath(getResourceTypeName(resourceId))
                    .appendPath(getResourceEntryName(resourceId))
                    .build()
            }
        }
        FileMessageView(
            isMe = true,
            fileTypeResId = iconPackR.drawable.ic_alert_circle_regular_medium_outline,
            previewUri = resourceUri,
            duration = null,
            loadProgress = 0.6f,
            fileName = "Hello.pdf",
            fileSize = "30 MB",
            showPausedTransfersWarning = isUploadPaused,
        )
    }
}

internal const val FILE_PREVIEW_MESSAGE_VIEW_IMAGE_TEST_TAG = "chat_file_preview_message_view:image"
private const val MAX_SIZE = 212

@Composable
private fun Dp.coerceAtMostPixels(pixels: Float): Dp {
    val density = LocalDensity.current
    return if (pixels.isFinite()) {
        this.coerceAtMost(with(density) { pixels.toDp() })
    } else {
        this
    }
}

private fun Size.aspectRatio() =
    if (this.height == 0f) 1f else this.width / this.height
