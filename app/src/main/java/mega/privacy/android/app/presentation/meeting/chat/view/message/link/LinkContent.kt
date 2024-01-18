package mega.privacy.android.app.presentation.meeting.chat.view.message.link

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
}

/**
 * Contact link content
 *
 * @property content Contact link content
 */
data class ContactLinkContent(val content: ContactLink, override val link: String) : LinkContent

/**
 * Chat link content
 *
 * @property numberOfParticipants Number of participants
 * @property name Group name
 */
data class ChatGroupLinkContent(
    val numberOfParticipants: Long = 0L,
    val name: String = "",
    override val link: String,
) : LinkContent

/**
 * Folder link content
 *
 * @property folderInfo Folder information
 */
data class FolderLinkContent(
    val folderInfo: FolderInfo,
    override val link: String,
) : LinkContent

/**
 * File link content
 *
 * @property node
 * @property link
 */
data class FileLinkContent(
    val node: TypedFileNode,
    override val link: String,
) : LinkContent