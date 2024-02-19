package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.onUserClick
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.dividers.DividerSpacing
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Chat link message view
 *
 * @param message Chat link message
 * @param modifier Modifier
 * @param viewModel Chat link message view model
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatLinksMessageView(
    message: TextLinkMessage,
    onLongClick: (TypedMessage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatLinksMessageViewModel = hiltViewModel(),
    contactViewModel: ContactMessageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    var contentLinks by remember {
        mutableStateOf(listOf<LinkContent>())
    }
    LaunchedEffect(message.links) {
        message.links.forEach {
            when (it.type) {
                RegexPatternType.CONTACT_LINK -> {
                    val linkContent = viewModel.loadContactInfo(it.link) { handle, email, name ->
                        onUserClick(
                            handle,
                            email.orEmpty(),
                            name.orEmpty(),
                            context,
                            coroutineScope,
                            snackbarHostState,
                            contactViewModel::checkUser,
                            contactViewModel::inviteUser,
                        )
                    }
                    if (linkContent != null) {
                        contentLinks = contentLinks + linkContent
                    }
                }

                RegexPatternType.CHAT_LINK -> {
                    val linkContent = viewModel.loadChatLinkInfo(it.link)
                    contentLinks = contentLinks + linkContent
                }

                RegexPatternType.FOLDER_LINK -> {
                    viewModel.loadFolderLinkInfo(it.link)?.let { linkContent ->
                        contentLinks = contentLinks + linkContent
                    }
                }

                RegexPatternType.FILE_LINK -> {
                    viewModel.loadFileLinkInfo(it.link)?.let { linkContent ->
                        contentLinks = contentLinks + linkContent
                    }
                }
                // other link type here
                else -> Unit
            }
        }
    }
    ChatBubble(
        isMe = message.isMine,
        modifier = modifier
            .combinedClickable(
                onClick = { contentLinks.firstOrNull()?.onClick(context) },
                onLongClick = { onLongClick(message) }
            ),
        subContent = {
            contentLinks.forEachIndexed { index, linkContent ->
                key(linkContent.link) {
                    linkContent.SubContentComposable(
                        modifier = Modifier.combinedClickable(
                            onClick = { linkContent.onClick(context) },
                            onLongClick = { onLongClick(message) }
                        ))
                }

                if (index != contentLinks.lastIndex) {
                    MegaDivider(dividerSpacing = DividerSpacing.Full)
                }
            }
        },
        content = {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = message.content,
            )
        },
    )
}