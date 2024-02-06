package mega.privacy.android.app.presentation.meeting.dialog.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.chat.list.view.ChatDivider
import mega.privacy.android.app.presentation.chat.list.view.ChatUserStatusView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.view.BottomSheetMenuItemView
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Recurring Meeting Occurrence bottom sheet view
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CallParticipantBottomSheetView(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    state: MeetingState,
    onAddContactClick: () -> Unit = {},
    onContactInfoClick: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onSendMessageClick: () -> Unit = {},
    onMakeHostClick: () -> Unit = {},
    onRemoveAsHostClick: () -> Unit = {},
    onDisplayInMainViewClick: () -> Unit = {},
    onMuteParticipantClick: () -> Unit = {},
    onRemoveParticipantClick: () -> Unit = {},
) {

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        scrimColor = black.copy(alpha = 0.32f),
        sheetContent = {
            BottomSheetContent(
                modalSheetState = modalSheetState,
                coroutineScope = coroutineScope,
                state = state,
                onAddContactClick = onAddContactClick,
                onContactInfoClick = onContactInfoClick,
                onEditProfileClick = onEditProfileClick,
                onSendMessageClick = onSendMessageClick,
                onMakeHostClick = onMakeHostClick,
                onRemoveAsHostClick = onRemoveAsHostClick,
                onDisplayInMainViewClick = onDisplayInMainViewClick,
                onMuteParticipantClick = onMuteParticipantClick,
                onRemoveParticipantClick = onRemoveParticipantClick,
            )
        }
    ) {}
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    state: MeetingState,
    onAddContactClick: () -> Unit = {},
    onContactInfoClick: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onSendMessageClick: () -> Unit = {},
    onMakeHostClick: () -> Unit = {},
    onRemoveAsHostClick: () -> Unit = {},
    onDisplayInMainViewClick: () -> Unit = {},
    onRemoveParticipantClick: () -> Unit = {},
    onMuteParticipantClick: () -> Unit = {},
) {

    state.chatParticipantSelected?.let { participant ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(71.dp)
            ) {

                val (avatarImage, titleText, subtitleText, statusIcon) = createRefs()
                Box(modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
                    .constrainAs(avatarImage) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }) {
                    ChatAvatarView(
                        avatarUri = participant.data.avatarUri,
                        avatarPlaceholder = participant.getAvatarFirstLetter(),
                        avatarColor = participant.defaultAvatarColor,
                        avatarTimestamp = participant.avatarUpdateTimestamp,
                        modifier = Modifier.border(1.dp, Color.White, CircleShape),
                    )
                }

                val name = participant.data.fullName ?: participant.email ?: ""

                Text(
                    text = if (participant.isMe) stringResource(
                        R.string.chat_me_text_bracket,
                        name
                    ) else name,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.textColorPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.constrainAs(titleText) {
                        linkTo(avatarImage.end, parent.end, 16.dp, 32.dp, 0.dp, 0.dp, 0f)
                        top.linkTo(parent.top)
                        bottom.linkTo(subtitleText.top)
                        width = Dimension.preferredWrapContent
                    }
                )

                if (!participant.isMe && participant.callParticipantData.isContact && !participant.callParticipantData.isGuest) {
                    ChatUserStatusView(
                        userChatStatus = participant.status,
                        modifier = Modifier.constrainAs(statusIcon) {
                            start.linkTo(titleText.end, 4.dp)
                            top.linkTo(titleText.top)
                            bottom.linkTo(titleText.bottom)
                        },
                    )
                }

                Text(
                    text = participant.email ?: "",
                    color = MaterialTheme.colors.textColorSecondary,
                    style = MaterialTheme.typography.subtitle2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.constrainAs(subtitleText) {
                        linkTo(avatarImage.end, parent.end, 16.dp, 16.dp, 0.dp, 0.dp, 0f)
                        top.linkTo(titleText.bottom)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.preferredWrapContent
                    }
                )

                createVerticalChain(
                    titleText,
                    subtitleText,
                    chainStyle = ChainStyle.Packed
                )
            }

            ChatDivider(startPadding = 16.dp)
            Column(modifier = Modifier.verticalScroll(rememberScrollState()))
            {
                if (!participant.callParticipantData.isContact && !participant.callParticipantData.isGuest && !participant.isMe) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_g_add_contact,
                        text = R.string.menu_add_contact,
                        description = "Add contact",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onAddContactClick()
                        }
                    )
                    ChatDivider()
                }

                if (participant.callParticipantData.isContact && !participant.isMe && !participant.callParticipantData.isGuest) {
                    BottomSheetMenuItemView(modifier = Modifier,
                        res = R.drawable.info_ic,
                        text = R.string.contact_properties_activity,
                        description = "Contact info",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onContactInfoClick(participant.email ?: "")
                        })
                    ChatDivider()
                }

                if (participant.isMe) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.info_ic,
                        text = R.string.group_chat_edit_profile_label,
                        description = "Edit profile",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onEditProfileClick()
                        }
                    )
                    ChatDivider()
                }

                if (!participant.callParticipantData.isGuest && participant.callParticipantData.isContact && !participant.isMe) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_chat,
                        text = R.string.context_send_message,
                        description = "Send message",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onSendMessageClick()
                        }
                    )
                    ChatDivider()
                }

                val shouldShowMuteOption =
                    state.isMuteFeatureFlagEnabled && state.hasHostPermission() && !participant.isMe && !participant.isMuted

                if (shouldShowMuteOption) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.mute_participant_icon,
                        text = R.string.general_mute,
                        description = "Mute participant",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onMuteParticipantClick()
                        }
                    )
                    ChatDivider()
                }

                if (state.hasHostPermission() && !participant.isMe && participant.privilege != ChatRoomPermission.Moderator) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_moderator,
                        text = R.string.make_moderator,
                        description = "Make host",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onMakeHostClick()
                        }
                    )
                    ChatDivider()
                }

                if (state.hasHostPermission() && !participant.isMe && participant.privilege == ChatRoomPermission.Moderator) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_remove_moderator,
                        text = R.string.remove_moderator,
                        description = "Remove as host",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRemoveAsHostClick()
                        }
                    )
                    ChatDivider()
                }

                if (!participant.isMe && state.isSpeakerMode) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_speaker_view,
                        text = R.string.pin_to_speaker,
                        description = "Display in main view",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onDisplayInMainViewClick()
                        }
                    )
                    ChatDivider()
                }

                if (state.hasHostPermission() && !participant.isMe) {
                    BottomSheetMenuItemView(
                        modifier = Modifier,
                        res = R.drawable.ic_remove,
                        text = R.string.remove_participant_menu_item,
                        description = "Remove participant",
                        tintRed = true,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRemoveParticipantClick()
                        }
                    )
                    ChatDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun PreviewCallParticipantBottomSheetView() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    CallParticipantBottomSheetView(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        state = MeetingState(),
    )
}