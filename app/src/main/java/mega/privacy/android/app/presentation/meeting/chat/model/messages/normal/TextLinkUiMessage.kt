package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.onUserClick
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinksMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinksMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.LinkContent
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Contact link ui message
 * @property message Contact link message
 *
 */
data class TextLinkUiMessage(
    override val message: TextLinkMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val viewModel: ChatLinksMessageViewModel = hiltViewModel()
        val contactViewModel: ContactMessageViewModel = hiltViewModel()
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
                        val linkContent =
                            viewModel.loadContactInfo(it.link) { handle, email, name, isContact ->
                                onUserClick(
                                    handle,
                                    email.orEmpty(),
                                    name.orEmpty(),
                                    isContact,
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
        ChatLinksMessageView(
            message = message,
            contentLinks = contentLinks,
            linkViews = {
                contentLinks.forEachIndexed { index, linkContent ->
                    key(linkContent.link) {
                        linkContent.SubContentComposable(
                            modifier = initialiseModifier {
                                linkContent.onClick(
                                    context,
                                    navHostController
                                )
                            },
                        )
                    }

                    if (index != contentLinks.lastIndex) {
                        MegaDivider(dividerType = DividerType.FullSize)
                    }
                }
            },
            interactionEnabled = interactionEnabled,
            modifier = initialiseModifier {},
            onLongClick = onLongClick,
            navHostController = navHostController,
        )
    }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}