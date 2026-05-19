package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.model.ChatInfoAction
import mega.privacy.android.app.presentation.meeting.model.ChatInfoUiState
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054

/**
 * Control and show the available buttons
 *
 * @param state                 [mega.privacy.android.app.presentation.meeting.model.ChatInfoUiState]
 * @param noteToSelfChatState   [mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState]
 * @param action                [mega.privacy.android.app.presentation.meeting.model.ChatInfoAction]
 * @param onButtonClicked
 */
@Composable
internal fun ChatActionButton(
    state: ChatInfoUiState,
    noteToSelfChatState: NoteToSelfChatUIState,
    enabledMeetingLinkOption: Boolean,
    isCallInProgress: Boolean,
    action: ChatInfoAction,
    onButtonClicked: (ChatInfoAction) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .testTag(ACTION_BUTTON_OPTION_TAG)
            .fillMaxWidth()
            .clickable {
                if (action != ChatInfoAction.EnabledEncryptedKeyRotation && (action != ChatInfoAction.WaitingRoom || !isCallInProgress)) {
                    onButtonClicked(action)
                }
            }) {
        when (action) {
            ChatInfoAction.ShareMeetingLink,
            ChatInfoAction.ShareMeetingLinkNonHosts,
                -> {
                if (state.isPublic && enabledMeetingLinkOption && !state.isNoteToSelf) {
                    if (action == ChatInfoAction.ShareMeetingLink && state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 72.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                                style = MaterialTheme.typography.button,
                                text = stringResource(id = action.title),
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        CustomDivider(withStartPadding = true)
                    } else if (action == ChatInfoAction.ShareMeetingLinkNonHosts && !state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ActionOption(
                                state = state,
                                action = action,
                                isChecked = true,
                                hasSwitch = false
                            )
                        }
                        CustomDivider(withStartPadding = false)
                    }
                }
            }

            ChatInfoAction.EnableEncryptedKeyRotation ->
                if (state.isHost && state.isPublic && !state.isNoteToSelf) {
                    Text(
                        modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 18.dp
                        ),
                        style = MaterialTheme.typography.button,
                        text = stringResource(id = action.title),
                        color = MaterialTheme.colors.secondary
                    )

                    action.description?.let { description ->
                        Text(
                            modifier = Modifier.padding(
                                start = 14.dp,
                                end = 16.dp,
                                top = 10.dp,
                                bottom = 8.dp
                            ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                                ?: white_alpha_054)
                    }

                    CustomDivider(withStartPadding = false)
                }

            ChatInfoAction.EnabledEncryptedKeyRotation,
                -> if (state.isHost && !state.isPublic && !state.isNoteToSelf) {
                Text(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        end = 16.dp,
                        top = 18.dp
                    ),
                    style = MaterialTheme.typography.subtitle1,
                    text = stringResource(id = action.title),
                    color = black.takeIf { MaterialTheme.colors.isLight } ?: white)

                action.description?.let { description ->
                    Text(
                        modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                        style = MaterialTheme.typography.subtitle2,
                        text = stringResource(id = description),
                        color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                            ?: white_alpha_054)
                }

                CustomDivider(withStartPadding = false)
            }

            ChatInfoAction.MeetingLink,
                -> if (state.isHost && state.isPublic && !state.isNoteToSelf) {
                ActionOption(
                    state = state,
                    action = action,
                    isChecked = enabledMeetingLinkOption,
                    hasSwitch = true
                )
                CustomDivider(withStartPadding = true)
            }

            ChatInfoAction.AllowNonHostAddParticipants ->
                if (state.isHost && !state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = state.enabledAllowNonHostAddParticipantsOption,
                        hasSwitch = true
                    )
                    CustomDivider(withStartPadding = true)
                }

            ChatInfoAction.WaitingRoom -> {
                if (state.isHost && !state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isEnabled = !isCallInProgress,
                        isChecked = state.enabledWaitingRoomOption,
                        hasSwitch = true
                    )

                    action.description?.let { description ->
                        Text(
                            modifier = Modifier.padding(
                                start = 72.dp,
                                end = 16.dp,
                                top = 2.dp,
                                bottom = 18.dp
                            ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight }
                                ?: white_alpha_054)
                    }
                    CustomDivider(withStartPadding = true)
                }
            }

            ChatInfoAction.ManageChatHistory ->
                if (state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }

            ChatInfoAction.ManageMeetingHistory ->
                if (state.isHost && !state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }

            ChatInfoAction.ChatNotifications ->
                if (!state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = state.dndSeconds == null,
                        hasSwitch = true
                    )
                    CustomDivider(withStartPadding = true)
                }

            ChatInfoAction.ShareFiles -> {
                if (!state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }
            }

            ChatInfoAction.Files -> {
                if (state.isNoteToSelf) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }
            }

            ChatInfoAction.Archive ->
                if (state.isNoteToSelf && !state.isArchived) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }

            ChatInfoAction.Unarchive ->
                if (state.isNoteToSelf && state.isArchived) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)
                }
        }
    }
}

/**
 * Show action buttons options
 *
 * @param state         [ChatInfoUiState]
 * @param action        [ChatInfoAction]
 * @param isChecked     True, if the option is checked. False if not
 * @param hasSwitch     True, if the option has a switch. False if not
 * @param isEnabled     True, if the option must be enabled. False if not
 */
@Composable
private fun ActionOption(
    state: ChatInfoUiState,
    action: ChatInfoAction,
    isChecked: Boolean,
    hasSwitch: Boolean,
    isEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp
        )
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .wrapContentSize(Alignment.Center)

            ) {
                action.icon?.let { icon ->
                    MegaIcon(
                        painter = rememberVectorPainter(icon),
                        contentDescription = "${action.name} icon",
                        tint = IconColor.Secondary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ActionText(actionText = action.title)

                state.retentionTimeSeconds?.let { time ->
                    if (action == ChatInfoAction.ManageChatHistory) {
                        ManageChatHistorySubtitle(seconds = time)
                    }
                }

                state.dndSeconds?.let { time ->
                    if (action == ChatInfoAction.ChatNotifications) {
                        ChatNotificationSubtitle(seconds = time)
                    }
                }
            }
        }

        if (hasSwitch) {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
                    .height(40.dp)
            ) {
                MegaSwitch(
                    modifier = Modifier.align(Alignment.Center),
                    checked = isChecked,
                    enabled = isEnabled,
                    onCheckedChange = null,
                )
            }

        }
    }
}

/**
 * Subtitle text of the available options
 *
 * @param text subtitle text
 */
@Composable
private fun ActionSubtitleText(text: String) {
    MegaText(
        modifier = Modifier
            .padding(start = 32.dp, end = 23.dp),
        text = text,
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.subtitle2,
    )
}

/**
 * Text of the available options
 *
 * @param actionText Title of the option
 */
@Composable
private fun ActionText(actionText: Int) {
    MegaText(
        modifier = Modifier
            .padding(start = 32.dp, end = 23.dp),
        text = stringResource(id = actionText),
        textColor = TextColor.Primary,
        style = MaterialTheme.typography.subtitle1,
    )
}

/**
 * Manage chat history subtitle
 *
 * @param seconds  Retention time seconds
 */
@Composable
private fun ManageChatHistorySubtitle(seconds: Long) {
    val text = getRetentionTimeString(LocalContext.current, seconds)?.let {
        "${stringResource(R.string.subtitle_properties_manage_chat)} $it"
    } ?: ""

    ActionSubtitleText(text)
}

/**
 * Chat notification subtitle
 *
 * @param seconds  Dnd seconds
 */
@Composable
private fun ChatNotificationSubtitle(seconds: Long) {
    val text = if (seconds == 0L) {
        stringResource(R.string.mute_chatroom_notification_option_off)
    } else {
        getStringForDndTime(seconds)
    }

    ActionSubtitleText(text)
}

internal const val ACTION_BUTTON_OPTION_TAG = "scheduled_meeting_info:action_button_option"

/**
 * Meeting link action button View Preview
 */
@CombinedThemePreviews
@Composable
fun PreviewActionButton(@PreviewParameter(ChatInfoActionPreviewParameterProvider::class) action: ChatInfoAction) {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.height(56.dp)) {
            ChatActionButton(
                state = ChatInfoUiState(
                    scheduledMeeting = ChatScheduledMeeting(
                        chatId = -1,
                        schedId = -1,
                        parentSchedId = null,
                        organizerUserId = null,
                        timezone = null,
                        startDateTime = -1,
                        endDateTime = -1,
                        title = "Scheduled title",
                        description = "Scheduled description",
                        attributes = null,
                        overrides = null,
                        flags = null,
                        rules = null,
                        changes = null
                    )
                ),
                action = action,
                enabledMeetingLinkOption = true,
                isCallInProgress = false,
                noteToSelfChatState = NoteToSelfChatUIState(),
                onButtonClicked = {},
            )
        }
    }
}

private class ChatInfoActionPreviewParameterProvider : PreviewParameterProvider<ChatInfoAction> {
    override val values: Sequence<ChatInfoAction>
        get() = ChatInfoAction.entries.asSequence()

    override fun getDisplayName(index: Int) = ChatInfoAction.entries[index].name
}