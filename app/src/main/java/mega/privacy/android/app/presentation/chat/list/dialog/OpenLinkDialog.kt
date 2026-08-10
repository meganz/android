package mega.privacy.android.app.presentation.chat.list.dialog

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.NavigationEventEffect
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.contactlink.ContactLinkDialogFragment
import mega.privacy.android.app.main.dialog.link.OpenLinkViewModel
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialog
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Compose replacement for `OpenLinkDialogFragment`.
 *
 * Wraps [InputDialog] with [OpenLinkViewModel]. Resolves typed-link results to the
 * appropriate destination (file/folder/password activity, chat / meeting navigation,
 * contact-link sub-dialog, meeting-ended sub-dialog).
 *
 * @param isChatScreen Caller is the chat tabs (true) or generic open-link (false).
 * @param isJoinMeeting Caller intends to join a meeting.
 * @param onDismissRequest Called when the dialog should be dismissed.
 */
@Composable
fun OpenLinkDialog(
    isChatScreen: Boolean,
    isJoinMeeting: Boolean,
    onDismissRequest: () -> Unit,
) {
    val viewModel: OpenLinkViewModel =
        hiltViewModel<OpenLinkViewModel, OpenLinkViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(isChatScreen = isChatScreen, isJoinMeeting = isJoinMeeting)
            },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity ?: return
    val megaNavigator = rememberMegaNavigator()
    var meetingEndedException by remember { mutableStateOf<MeetingEndedException?>(null) }

    LaunchedEffect(state.linkType) {
        when (state.linkType) {
            RegexPatternType.FILE_LINK -> {
                context.startActivity(
                    Intent(context, FileLinkComposeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = Constants.ACTION_OPEN_MEGA_LINK
                        data = Uri.parse(viewModel.inputLink)
                    }
                )
                onDismissRequest()
            }

            RegexPatternType.FOLDER_LINK -> {
                context.startActivity(
                    Intent(context, FolderLinkComposeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                        data = Uri.parse(viewModel.inputLink)
                    }
                )
                onDismissRequest()
            }

            RegexPatternType.PASSWORD_LINK -> {
                context.startActivity(
                    Intent(context, OpenPasswordLinkActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        data = Uri.parse(viewModel.inputLink)
                    }
                )
                onDismissRequest()
            }

            else -> Unit
        }
    }

    LaunchedEffect(state.openContactLinkHandle) {
        val handle = state.openContactLinkHandle
        if (handle > 0L) {
            ContactLinkDialogFragment.newInstance(handle)
                .show(activity.supportFragmentManager, ContactLinkDialogFragment.TAG)
            onDismissRequest()
        }
    }

    LaunchedEffect(state.checkLinkResult) {
        val result = state.checkLinkResult ?: return@LaunchedEffect
        result.fold(
            onSuccess = { content -> viewModel.handleChatLinkContent(content) },
            onFailure = { exception ->
                when (exception) {
                    is MeetingEndedException -> meetingEndedException = exception
                    is IAmOnAnotherCallException -> {
                        CallUtil.showConfirmationInACall(
                            activity,
                            activity.getString(sharedR.string.can_only_join_one_call_error_message),
                        )
                        onDismissRequest()
                    }

                    else -> Unit
                }
            },
        )
    }

    NavigationEventEffect(
        event = state.joinMeetingEvent,
        onConsumed = viewModel::onJoinMeetingEventConsumed,
    ) { meetingLink ->
        CallUtil.joinMeetingOrReturnCall(
            activity,
            meetingLink.chatHandle,
            meetingLink.link,
            meetingLink.text,
            meetingLink.exist,
            meetingLink.userHandle,
            meetingLink.isWaitingRoom,
        )
        onDismissRequest()
    }

    NavigationEventEffect(
        event = state.openChatEvent,
        onConsumed = viewModel::onOpenChatEventConsumed,
    ) { chatLink ->
        megaNavigator.openChat(
            context = activity,
            chatId = chatLink.chatHandle,
            link = chatLink.link,
            action = Constants.ACTION_OPEN_CHAT_LINK,
        )
        onDismissRequest()
    }

    EventEffect(
        event = state.dismissEvent,
        onConsumed = viewModel::onDismissEventConsumed,
        action = onDismissRequest
    )

    meetingEndedException?.let { exception ->
        MeetingHasEndedDialog(
            isFromGuest = false,
            onLeave = {},
            onViewMeetingChat = {
                megaNavigator.openChat(
                    context = activity,
                    chatId = exception.chatId,
                    link = exception.link,
                    action = Constants.ACTION_OPEN_CHAT_LINK,
                )
            },
            onDismissRequest = {
                meetingEndedException = null
                onDismissRequest()
            },
        )
    }

    OpenLinkDialogContent(
        isChatScreen = isChatScreen,
        isJoinMeeting = isJoinMeeting,
        inputLink = viewModel.inputLink,
        submittedLink = state.submittedLink,
        linkType = state.linkType,
        checkLinkResult = state.checkLinkResult,
        onLinkChanged = viewModel::onLinkChanged,
        onConfirm = viewModel::openLink,
        onDismissRequest = onDismissRequest,
    )
}

/**
 * Stateless content of [OpenLinkDialog]. Renders [InputDialog] driven by raw state
 * so it can be exercised in `@Preview` and unit tests without a ViewModel, activity,
 * or navigator.
 */
@Composable
internal fun OpenLinkDialogContent(
    isChatScreen: Boolean,
    isJoinMeeting: Boolean,
    inputLink: String,
    submittedLink: String? = null,
    linkType: RegexPatternType? = null,
    checkLinkResult: Result<ChatLinkContent>? = null,
    onLinkChanged: (String) -> Unit = {},
    onConfirm: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    InputDialog(
        title = stringResource(openLinkTitleRes(isChatScreen, isJoinMeeting)),
        message = if (isJoinMeeting) {
            stringResource(R.string.paste_meeting_link_guest_instruction)
        } else {
            ""
        },
        hint = stringResource(openLinkHintRes(isChatScreen, isJoinMeeting)),
        text = inputLink,
        confirmButtonText = stringResource(openLinkPositiveTextRes(linkType)),
        cancelButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismissRequest,
        onInputChange = onLinkChanged,
        error = openLinkErrorRes(
            isJoinMeeting = isJoinMeeting,
            isChatScreen = isChatScreen,
            submittedLink = submittedLink,
            linkType = linkType,
            checkLinkResult = checkLinkResult,
        )?.let { stringResource(id = it) },
    )
}

@StringRes
private fun openLinkTitleRes(isChatScreen: Boolean, isJoinMeeting: Boolean): Int = when {
    isJoinMeeting -> R.string.paste_meeting_link_guest_dialog_title
    isChatScreen -> R.string.action_open_chat_link
    else -> R.string.action_open_link
}

@StringRes
private fun openLinkHintRes(isChatScreen: Boolean, isJoinMeeting: Boolean): Int = when {
    isJoinMeeting -> R.string.meeting_link
    isChatScreen -> R.string.hint_enter_chat_link
    else -> R.string.hint_paste_link
}

@StringRes
private fun openLinkPositiveTextRes(linkType: RegexPatternType?): Int = when (linkType) {
    RegexPatternType.CHAT_LINK -> R.string.action_open_chat_link
    RegexPatternType.CONTACT_LINK -> R.string.action_open_contact_link
    else -> R.string.context_open_link
}

@StringRes
private fun openLinkErrorRes(
    isJoinMeeting: Boolean,
    isChatScreen: Boolean,
    submittedLink: String?,
    linkType: RegexPatternType?,
    checkLinkResult: Result<ChatLinkContent>?,
): Int? {
    if (submittedLink != null && submittedLink.isEmpty()) {
        return when {
            isJoinMeeting -> R.string.invalid_meeting_link_empty
            isChatScreen -> R.string.invalid_chat_link_empty
            else -> R.string.invalid_file_folder_link_empty
        }
    }
    if (checkLinkResult != null
        && checkLinkResult.exceptionOrNull() is mega.privacy.android.domain.exception.MegaException
    ) {
        return if (isJoinMeeting) R.string.invalid_meeting_link_args
        else R.string.invalid_chat_link_args
    }
    if (linkType != null) {
        return when (linkType) {
            RegexPatternType.CHAT_LINK -> R.string.valid_chat_link
            RegexPatternType.CONTACT_LINK -> R.string.valid_contact_link
            RegexPatternType.FILE_LINK,
            RegexPatternType.FOLDER_LINK,
            RegexPatternType.PASSWORD_LINK,
                -> null

            else -> R.string.invalid_file_folder_link
        }
    }
    return null
}

@CombinedThemeComponentPreviews
@Composable
private fun OpenLinkDialogContentPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OpenLinkDialogContent(
            isChatScreen = false,
            isJoinMeeting = false,
            inputLink = "",
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun OpenLinkDialogContentChatScreenPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OpenLinkDialogContent(
            isChatScreen = true,
            isJoinMeeting = false,
            inputLink = "https://mega.nz/chat/example#abc",
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun OpenLinkDialogContentJoinMeetingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OpenLinkDialogContent(
            isChatScreen = true,
            isJoinMeeting = true,
            inputLink = "",
        )
    }
}
