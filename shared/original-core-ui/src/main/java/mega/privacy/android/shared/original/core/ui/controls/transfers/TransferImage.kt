package mega.privacy.android.shared.original.core.ui.controls.transfers

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun TransferImage(
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.size(48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    val noThumbnailContent: @Composable () -> Unit = {
        TransferFileType(isDownload = isDownload, fileTypeResId = fileTypeResId)
    }
    previewUri?.let {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(it)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            loading = { noThumbnailContent() },
            error = { noThumbnailContent() },
            success = { result ->
                Box(
                    modifier = modifier
                        .testTag(TEST_TAG_FILE_TYPE_ICON)
                        .size(42.dp)
                ) {
                    Image(
                        painter = result.painter,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .testTag(TEST_TAG_FILE_THUMBNAIL)
                            .size(40.dp)
                            .padding(start = 2.dp, top = 2.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    LeadingIndicator(
                        isDownload = isDownload,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        )
    } ?: noThumbnailContent()
}

@Composable
private fun TransferFileType(
    isDownload: Boolean,
    fileTypeResId: Int?,
    modifier: Modifier = Modifier,
) = Box(modifier = modifier.testTag(TEST_TAG_FILE_TYPE_ICON)) {
    fileTypeResId?.let {
        Image(
            painter = painterResource(id = it),
            contentDescription = null,
        )
    }
    LeadingIndicator(
        isDownload = isDownload,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 2.5.dp)
    )
}

@Composable
private fun LeadingIndicator(
    isDownload: Boolean,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .size(19.dp)
        .clip(CircleShape)
        .background(color = MegaOriginalTheme.colors.background.pageBackground)
        .testTag(
            if (isDownload) TEST_TAG_DOWNLOAD_LEADING_INDICATOR
            else TEST_TAG_UPLOAD_LEADING_INDICATOR
        )
) {
    Icon(
        modifier = Modifier
            .size(14.dp)
            .align(Alignment.Center),
        painter = painterResource(
            id = if (isDownload) R.drawable.ic_arrow_down_circle_small_thin_solid
            else R.drawable.ic_arrow_up_circle_small_thin_solid
        ),
        contentDescription = null,
        tint = if (isDownload) MegaOriginalTheme.colors.indicator.green
        else MegaOriginalTheme.colors.indicator.blue,
    )
}

@CombinedThemePreviews
@Composable
private fun TransferFileTypePreview(
    @PreviewParameter(BooleanProvider::class) isDownload: Boolean,
) {
    val isDark = isSystemInDarkTheme()
    OriginalTempTheme(isDark = isDark) {
        TransferFileType(
            isDownload = isDownload,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LeadingIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) isDownload: Boolean,
) {
    val isDark = isSystemInDarkTheme()
    OriginalTempTheme(isDark = isDark) {
        LeadingIndicator(
            isDownload = isDownload,
            modifier = Modifier.background(if (isDark) Color.White else Color.Black)
        )
    }
}

internal const val TEST_TAG_FILE_THUMBNAIL = "transfers_view:file_thumbnail"
internal const val TEST_TAG_FILE_TYPE_ICON = "transfers_view:file_type_icon"
internal const val TEST_TAG_DOWNLOAD_LEADING_INDICATOR = "transfers_view:download_leading_indicator"
internal const val TEST_TAG_UPLOAD_LEADING_INDICATOR = "transfers_view:upload_leading_indicator"