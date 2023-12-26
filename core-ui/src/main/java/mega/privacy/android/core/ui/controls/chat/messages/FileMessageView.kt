package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor
import kotlin.math.roundToInt

internal const val FILE_MESSAGE_VIEW_ROOT_TEST_TAG = "chat_file_message_view:root_view"
internal const val FILE_MESSAGE_VIEW_FILE_NAME_TEST_TAG =
    "chat_file_message_view:file_name_text"
internal const val FILE_MESSAGE_VIEW_FILE_SIZE_TEST_TAG =
    "chat_file_message_view:file_size_text"
internal const val FILE_MESSAGE_VIEW_FILE_TYPE_ICON_TEST_TAG =
    "chat_file_message_view:file_type_icon"
internal const val FILE_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG =
    "chat_file_message_view:load_progress_indicator"

/**
 * File message without preview
 *
 * @param isMe whether message is sent from me
 * @param fileTypeResId resource id of file type icon
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-100.
 * @param modifier
 * @param fileName name of file
 * @param fileSize size string of file. It can be "Uploading" when message is loading.
 * @param onClick handle click when file message is clicked
 */
@Composable
fun FileMessageView(
    isMe: Boolean,
    fileTypeResId: Int,
    modifier: Modifier = Modifier,
    loadProgress: Int? = null,
    fileName: String = "",
    fileSize: String = "",
    onClick: () -> Unit = {},
) {
    // the width of loading overlay is calculated later
    var width by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() }
            .onGloballyPositioned { coordinates ->
                width = (coordinates.size.width.toFloat() / density.density).roundToInt() + 1
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isMe) MegaTheme.colors.button.primary else MegaTheme.colors.background.surface2,
            )
            .testTag(FILE_MESSAGE_VIEW_ROOT_TEST_TAG),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileIcon(fileTypeResId, modifier)
            Column {
                FileNameText(fileName, isMe)
                FileSizeText(fileSize, isMe)
            }
        }

        LoadOverlay(loadProgress = loadProgress, width = width)
        LoadProgress(loadProgress = loadProgress, width = width)
    }
}

@Composable
private fun FileSizeText(fileSize: String, isMe: Boolean) {
    MegaText(
        text = fileSize,
        style = MaterialTheme.typography.caption,
        textColor = if (isMe) TextColor.Inverse else TextColor.Primary,
        modifier = Modifier.testTag(FILE_MESSAGE_VIEW_FILE_SIZE_TEST_TAG),
    )
}

@Composable
private fun FileNameText(fileName: String, isMe: Boolean) {
    MegaText(
        text = fileName,
        style = MaterialTheme.typography.subtitle1,
        textColor = if (isMe) TextColor.Inverse else TextColor.Primary,
        overflow = LongTextBehaviour.MiddleEllipsis,
        modifier = Modifier
            .widthIn(max = 184.dp)
            .testTag(FILE_MESSAGE_VIEW_FILE_NAME_TEST_TAG),
    )
}

@Composable
private fun FileIcon(fileTypeResId: Int, modifier: Modifier) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(fileTypeResId),
            contentDescription = null,
            modifier = modifier.testTag(FILE_MESSAGE_VIEW_FILE_TYPE_ICON_TEST_TAG),
        )
    }
}

/**
 * Show an overlay when file message is loading.
 *
 * @param loadProgress loading progress of the message. null if already loaded. The value can be 0-100.
 */
@Composable
private fun LoadOverlay(loadProgress: Int?, width: Int) {
    loadProgress?.let {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(56.dp)
                .background(
                    color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f)
                    else Color.White.copy(alpha = 0.5f),
                )
        )
    }
}

@Composable
private fun LoadProgress(loadProgress: Int?, width: Int) {
    loadProgress?.let {
        MegaLinearProgressIndicator(
            progress = loadProgress / 100f,
            modifier = Modifier
                .width(width.dp)
                .height(4.dp)
                .border(width = 0.dp, color = Color.Transparent)
                .testTag(FILE_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileMessageViewShortFileNamePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileMessageView(
            isMe = isMe,
            loadProgress = null,
            fileName = "this is a very very very long file name.pdf",
            fileSize = "30 MB",
            fileTypeResId = R.drawable.ic_check_circle,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileMessageView(
            isMe = isMe,
            loadProgress = null,
            fileName = "hello.pdf",
            fileSize = "30 MB",
            fileTypeResId = R.drawable.ic_check_circle,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileMessageViewLoadingPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileMessageView(
            isMe = isMe,
            fileTypeResId = R.drawable.ic_alert_circle,
            loadProgress = 30,
            fileName = "long long long file name.pdf",
            fileSize = "30 MB",
        )
    }
}




