package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R


@Composable
internal fun TransferImage(
    fileTypeResId: Int?,
    previewUri: Uri?,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.size(32.dp),
    contentAlignment = Alignment.Center,
) {
    val noThumbnailContent: @Composable () -> Unit = {
        TransferFileType(fileTypeResId = fileTypeResId)
    }
    previewUri?.let {
        SubcomposeAsyncImage(
            modifier = Modifier.testTag(TEST_TAG_FILE_THUMBNAIL),
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(it)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            loading = { noThumbnailContent() },
            error = { noThumbnailContent() },
            success = { result ->
                Image(
                    painter = result.painter,
                    contentDescription = "Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(LocalSpacing.current.x4)),
                )
            }
        )
    } ?: noThumbnailContent()
}

@Composable
private fun TransferFileType(
    fileTypeResId: Int?,
    modifier: Modifier = Modifier,
) = Box(modifier = modifier.testTag(TEST_TAG_FILE_TYPE_ICON)) {
    fileTypeResId?.let {
        Image(
            painter = painterResource(id = it),
            contentDescription = null,
        )
    }
}

@Composable
internal fun LeadingIndicator(
    isDownload: Boolean,
    modifier: Modifier = Modifier,
    isOverQuota: Boolean = false,
) = Box(
    modifier = modifier
        .size(16.dp)
        .clip(CircleShape)
        .background(color = DSTokens.colors.background.pageBackground)
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
            id = if (isDownload) R.drawable.ic_arrow_down_circle_small_regular_outline
            else R.drawable.ic_arrow_up_circle_small_regular_outline
        ),
        contentDescription = null,
        tint = when {
            isOverQuota -> DSTokens.colors.indicator.yellow
            isDownload -> DSTokens.colors.indicator.green
            else -> DSTokens.colors.indicator.blue
        },
    )
}

@CombinedThemePreviews
@Composable
private fun TransferFileTypePreview(
    @PreviewParameter(BooleanProvider::class) isDownload: Boolean,
) {
    AndroidThemeForPreviews {
        TransferFileType(
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LeadingIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) isDownload: Boolean,
) {
    AndroidThemeForPreviews {
        LeadingIndicator(
            isDownload = isDownload,
        )
    }
}

internal const val TEST_TAG_FILE_THUMBNAIL = "transfers_view:file_thumbnail"
internal const val TEST_TAG_FILE_TYPE_ICON = "transfers_view:file_type_icon"
internal const val TEST_TAG_DOWNLOAD_LEADING_INDICATOR = "transfers_view:download_leading_indicator"
internal const val TEST_TAG_UPLOAD_LEADING_INDICATOR = "transfers_view:upload_leading_indicator"