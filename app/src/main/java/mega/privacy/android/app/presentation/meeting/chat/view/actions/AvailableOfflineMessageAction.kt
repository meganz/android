package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithClick
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ChatConversationAvailableOfflineActionMenuItemEvent
import timber.log.Timber

internal const val OFFLINE_SWITCH_TEST_TAG = "available_offline_message_option:offline_switch"

internal class AvailableOfflineMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.file_properties_available_offline,
    icon = mega.privacy.android.icon.pack.R.drawable.ic_arrow_down_circle_medium_regular_outline,
    testTag = "available_offline",
    group = MessageActionGroup.Transfer,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) =
        messages.size == 1 && messages.first().let { it is NodeAttachmentMessage && it.exists }

    override fun bottomSheetItem(
        message: TypedMessage,
        onClick: () -> Unit,
    ): @Composable () -> Unit = {
        val viewModel = hiltViewModel<NodeAttachmentMessageViewModel>()
        var isAvailableOffline by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isAvailableOffline =
                viewModel.isAvailableOffline((message as NodeAttachmentMessage).fileNode)
        }
        MenuActionListTile(
            text = stringResource(id = text),
            icon = painterResource(id = icon),
            modifier = Modifier
                .testTag(bottomSheetItemTestTag)
                .clickable(onClick = onClick),
            dividerType = null,
            trailingItem = {
                MegaSwitch(
                    checked = isAvailableOffline,
                    modifier = Modifier
                        .testTag(OFFLINE_SWITCH_TEST_TAG)
                        .padding(end = 16.dp),
                ) { onClick() }
            }
        )
    }

    override fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? = null

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationAvailableOfflineActionMenuItemEvent)
        val snackbarHostState = LocalSnackBarHostState.current
        val removeMessage = stringResource(id = R.string.file_removed_offline)
        val viewModel = hiltViewModel<NodeAttachmentMessageViewModel>()
        LaunchedEffect(messages.size) {
            val fileNode = (messages.first() as NodeAttachmentMessage).fileNode
            runCatching {
                val isAvailableOffline = viewModel.isAvailableOffline(fileNode)
                if (isAvailableOffline) {
                    viewModel.removeOfflineNode(fileNode)
                    snackbarHostState?.showAutoDurationSnackbar(removeMessage)
                } else {
                    chatViewModel.onDownloadForOfflineChatNode(fileNode)
                }
            }.onFailure {
                Timber.e(it)
            }
            onHandled()
        }
    }
}