package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import mega.privacy.android.app.presentation.meeting.chat.extension.toUiChatStatus
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openFileLinkActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openFolderLinkActivity
import mega.privacy.android.core.ui.controls.chat.messages.ContactMessageContentView
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Link content
 * @property link Link
 *
 */
sealed interface LinkContent {
    val link: String

    /**
     * Sub content composable
     */
    @Composable
    fun SubContentComposable(
        modifier: Modifier,
    )

    /**
     * Action to do when this link is clicked
     */
    fun onClick(context: Context) {}
}

/**
 * Contact link content
 *
 * @property content Contact link content
 * @property onClick On click
 */
data class ContactLinkContent(
    val content: ContactLink,
    override val link: String,
    val onClick: () -> Unit,
) : LinkContent {
    @Composable
    override fun SubContentComposable(modifier: Modifier) {
        ContactMessageContentView(
            modifier = modifier,
            userName = content.fullName.orEmpty(),
            email = content.email.orEmpty(),
            status = content.status.toUiChatStatus(),
            avatar = {
                ChatAvatar(
                    handle = content.contactHandle,
                    modifier = Modifier.size(40.dp)
                )
            },
        )
    }

    override fun onClick(context: Context) {
        onClick()
    }
}

/**
 * Chat link content
 *
 * @property numberOfParticipants Number of participants
 * @property name Group name
 * @property chatId Chat Id
 */
data class ChatGroupLinkContent(
    val numberOfParticipants: Long = 0L,
    val name: String = "",
    val chatId: Long = 0L,
    override val link: String,
) : LinkContent {

    internal val isChatAvailable = numberOfParticipants > 0

    @Composable
    override fun SubContentComposable(modifier: Modifier) {
        ChatLinkMessageView(
            modifier = modifier,
            linkContent = this,
        )
    }

    override fun onClick(context: Context) {
        if (isChatAvailable) {
            openChatFragment(context, chatId, link)
        }
    }
}

/**
 * Folder link content
 *
 * @property folderInfo Folder information
 */
data class FolderLinkContent(
    val folderInfo: FolderInfo,
    override val link: String,
) : LinkContent {
    @Composable
    override fun SubContentComposable(modifier: Modifier) {
        FolderLinkMessageView(
            modifier = modifier,
            linkContent = this,
        )
    }

    override fun onClick(context: Context) {
        openFolderLinkActivity(context, link.toUri())
    }
}

/**
 * File link content
 *
 * @property node
 * @property link
 */
data class FileLinkContent(
    val node: TypedFileNode,
    override val link: String,
) : LinkContent {
    @Composable
    override fun SubContentComposable(modifier: Modifier) {
        FileLinkMessageView(
            modifier = modifier,
            linkContent = this,
        )
    }

    override fun onClick(context: Context) {
        openFileLinkActivity(context, link.toUri())
    }
}