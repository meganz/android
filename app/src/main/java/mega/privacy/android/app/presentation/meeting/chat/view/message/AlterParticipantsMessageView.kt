package mega.privacy.android.app.presentation.meeting.chat.view.message

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Alter participants message view
 *
 */
@Composable
fun AlterParticipantsMessageView(
    message: AlterParticipantsMessage,
    modifier: Modifier = Modifier,
    viewModel: AlterParticipantsMessageViewModel = hiltViewModel(),
) {
    var ownerActionFullName by remember { mutableStateOf("") }
    var targetActionFullName by remember { mutableStateOf("") }
    val context: Context = LocalContext.current
    LaunchedEffect(Unit) {
        val myUserHandle = viewModel.getMyUserHandle()
        ownerActionFullName = if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        } ?: context.getString(R.string.unknown_name_label)
        targetActionFullName = if (message.userHandle == message.handleOfAction) {
            ownerActionFullName
        } else {
            if (message.handleOfAction == myUserHandle) {
                viewModel.getMyFullName()
            } else {
                viewModel.getParticipantFullName(message.handleOfAction)
            } ?: context.getString(R.string.unknown_name_label)
        }
    }

    AlterParticipantsMessageView(
        message = message,
        ownerActionFullName = ownerActionFullName,
        targetActionFullName = targetActionFullName,
        modifier = modifier,
    )
}

/**
 * Alter participants message view
 *
 * @param message
 * @param ownerActionFullName
 * @param targetActionFullName
 * @param modifier
 */
@Composable
fun AlterParticipantsMessageView(
    message: AlterParticipantsMessage,
    ownerActionFullName: String?,
    targetActionFullName: String?,
    modifier: Modifier = Modifier,
) {
    val participantsMessage = if (message.privilege != ChatRoomPermission.Removed) {
        // case added
        if (message.userHandle == message.handleOfAction) { // by themselves
            stringResource(
                R.string.message_joined_public_chat_autoinvitation,
                ownerActionFullName.orEmpty()
            )
        } else {
            stringResource(
                R.string.message_add_participant,
                targetActionFullName.orEmpty(),
                ownerActionFullName.orEmpty()
            )
        }
    } else {
        // case removed or left
        if (message.userHandle == message.handleOfAction) { // left case by themselves
            stringResource(
                R.string.message_participant_left_group_chat,
                ownerActionFullName.orEmpty()
            )
        } else { // removed case
            stringResource(
                R.string.message_remove_participant,
                targetActionFullName.orEmpty(),
                ownerActionFullName.orEmpty()
            )
        }
    }

    ChatManagementMessageView(
        modifier = modifier,
        text = participantsMessage,
        styles = mapOf(
            SpanIndicator('A') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
            SpanIndicator('B') to MegaSpanStyle(
                SpanStyle(),
                color = TextColor.Secondary
            ),
            SpanIndicator('C') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AlterParticipantsMessageViewSameHandlePreview(
    @PreviewParameter(ChatRoomPermissionProvider::class) permission: ChatRoomPermission,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AlterParticipantsMessageView(
            message = AlterParticipantsMessage(
                userHandle = 1L,
                handleOfAction = 1L,
                privilege = permission,
                isMine = true,
                time = System.currentTimeMillis(),
                msgId = 1L,
            ),
            ownerActionFullName = "Owner",
            targetActionFullName = "Target",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AlterParticipantsMessageViewDifferentHandlePreview(
    @PreviewParameter(ChatRoomPermissionProvider::class) permission: ChatRoomPermission,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AlterParticipantsMessageView(
            message = AlterParticipantsMessage(
                userHandle = 1L,
                handleOfAction = 2L,
                privilege = permission,
                isMine = true,
                time = System.currentTimeMillis(),
                msgId = 1L,
            ),
            ownerActionFullName = "Owner",
            targetActionFullName = "Target",
        )
    }
}

private class ChatRoomPermissionProvider :
    CollectionPreviewParameterProvider<ChatRoomPermission>(ChatRoomPermission.entries)