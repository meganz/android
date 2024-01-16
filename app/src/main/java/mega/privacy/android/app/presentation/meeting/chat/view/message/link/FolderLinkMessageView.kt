package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.RichLinkContentView
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Folder link message view
 *
 * @param linkContent Link content
 * @param modifier
 */
@Composable
fun FolderLinkMessageView(
    linkContent: FolderLinkContent,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    with(linkContent.folderInfo) {
        val folderInfoText = remember(linkContent.folderInfo) {
            TextUtil.getFolderInfo(numFolders, numFiles, context) +
                    "\n${FileSizeStringMapper(context)(currentSize)}"
        }
        RichLinkContentView(
            modifier = modifier,
            image = painterResource(R.drawable.ic_folder_preview),
            contentTitle = folderName,
            contentDescription = folderInfoText,
            icon = painterResource(R.drawable.ic_logo_notifications),
            host = Uri.parse(linkContent.link).authority.orEmpty()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FolderLinkMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe, subContent = {
            FolderLinkMessageView(
                linkContent = FolderLinkContent(
                    link = "https://mega.nz/folder/1234567890",
                    folderInfo = FolderInfo(
                        folderName = "Folder name",
                        numFolders = 3,
                        numFiles = 24,
                        currentSize = 1234567890L,
                        numVersions = 1,
                        versionsSize = 1234567890L,
                    )
                )
            )
        }) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "https://mega.nz/chat/1234567890#1234567890"
            )
        }
    }
}