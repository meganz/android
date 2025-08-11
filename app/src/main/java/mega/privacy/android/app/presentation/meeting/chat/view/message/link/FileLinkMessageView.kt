package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.mapper.IconType
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.RichLinkContentView
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Folder link message view
 *
 * @param linkContent
 * @param fileTypeIconMapper [FileTypeIconMapper]
 * @param modifier
 */
@Composable
fun FileLinkMessageView(
    linkContent: FileLinkContent,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
) {
    FileLinkMessageView(
        modifier = modifier,
        fileIcon = painterResource(
            id =
            fileTypeIconMapper(
                fileExtension = linkContent.node.type.extension,
                iconType = IconType.Outlined
            )
        ),
        fileName = linkContent.node.name,
        fileSize = linkContent.node.size,
        link = linkContent.link,
    )
}

/**
 * File link message view
 *
 * @param fileIcon
 * @param fileName
 * @param fileSize
 * @param link
 * @param modifier
 */
@Composable
fun FileLinkMessageView(
    fileIcon: Painter,
    fileName: String,
    fileSize: Long,
    link: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    RichLinkContentView(
        modifier = modifier,
        image = fileIcon,
        contentTitle = fileName,
        contentDescription = FileSizeStringMapper(context)(fileSize),
        icon = painterResource(R.drawable.ic_logo_notifications),
        host = Uri.parse(link).authority.orEmpty(),
        isFullImage = false
    )
}

@CombinedThemePreviews
@Composable
private fun FolderLinkMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe, subContent = {
            FileLinkMessageView(
                fileIcon = painterResource(IconPackR.drawable.ic_3d_thumbnail_outline),
                fileName = "File name",
                fileSize = 1234567890L,
                link = "https://mega.app/file/1234567890"
            )
        }) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "https://mega.app/chat/1234567890#1234567890"
            )
        }
    }
}