package mega.privacy.android.app.presentation.meeting.chat.view

import mega.privacy.android.core.R as CoreR
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.extensions.vectorRes
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderParagraph
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderSubtitleWithIcon
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderTitle
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import timber.log.Timber

/**
 * Chat view
 *
 * @param uiState [ChatUiState]
 * @param onBackPressed Action to perform for back button.
 * @param onMenuActionPressed Action to perform for menu buttons.
 */
@Composable
internal fun ChatView(
    uiState: ChatUiState,
    onBackPressed: () -> Unit,
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) {
            Timber.d("Ready to start call")
            // start call here
        } else {
            coroutineScope.launch {
                val result = snackBarHostState.showSnackbar(
                    context.getString(R.string.allow_acces_calls_subtitle_microphone),
                    context.getString(R.string.general_allow),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.navigateToAppSettings()
                }
            }
        }
    }
    var showParticipatingInACallDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = uiState.title.orEmpty(),
                onNavigationPressed = onBackPressed,
                titleIcons = { TitleIcons(uiState) },
                actions = getChatRoomActions(uiState),
                onActionPressed = onActionPressed@{
                    when (it) {
                        is ChatRoomMenuAction.AudioCall, is ChatRoomMenuAction.VideoCall -> {
                            if (uiState.isParticipatingInACall) {
                                showParticipatingInACallDialog = true
                                return@onActionPressed
                            }
                            if (checkStorageState(context, uiState.storageState)) {
                                callPermissionsLauncher.launch(PermissionUtils.getCallPermissionListByVersion())
                            }
                        }

                        is ChatRoomMenuAction.Info -> {
                            showGroupOrContactInfoActivity(context, uiState)
                        }

                        else -> (it as ChatRoomMenuAction).let(onMenuActionPressed)
                    }
                },
                maxActionsToShow = MENU_ACTIONS_TO_SHOW,
                subtitle = getSubtitle(uiState = uiState),
                elevation = 0.dp
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    )
    { _ ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item("first_message_header") {
                FirstMessageHeader(uiState)
            }
        }
        if (showParticipatingInACallDialog) {
            ParticipatingInACallDialog(
                onDismiss = { showParticipatingInACallDialog = false },
                onConfirm = {
                    showParticipatingInACallDialog = false
                    // return to active call
                }
            )
        }
    }
}

@Composable
private fun FirstMessageHeader(uiState: ChatUiState) {
    Column(
        modifier = Modifier.padding(start = 72.dp, top = 40.dp, end = 24.dp),
    ) {
        uiState.title?.let {
            FirstMessageHeaderTitle(
                title = it,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }

        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.chat_chatroom_first_message_header_mega_info_text),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        FirstMessageHeaderSubtitleWithIcon(
            subtitle = stringResource(id = R.string.title_mega_confidentiality_empty_screen),
            iconRes = R.drawable.ic_lock
        )
        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.mega_confidentiality_empty_screen),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        FirstMessageHeaderSubtitleWithIcon(
            subtitle = stringResource(id = R.string.title_mega_authenticity_empty_screen),
            iconRes = CoreR.drawable.ic_check_circle
        )
        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.chat_chatroom_first_message_header_authenticity_info_text)
        )
    }
}

private fun showGroupOrContactInfoActivity(context: Context, uiState: ChatUiState) {
    val isScheduledMeetingValid: Boolean = uiState.scheduledMeeting != null
    if (isScheduledMeetingValid) {
        Timber.d("show scheduled meeting info")
    } else {
        val targetActivity =
            if (uiState.isGroup) GroupChatInfoActivity::class.java else ContactInfoActivity::class.java
        Intent(context, targetActivity).apply {
            putExtra(Constants.HANDLE, uiState.chatId)
            putExtra(Constants.ACTION_CHAT_OPEN, true)
        }.also {
            context.startActivity(it)
        }
    }
}

@Composable
private fun TitleIcons(uiState: ChatUiState) {
    UserChatStateIcon(uiState.userChatStatus)
    PrivateChatIcon(uiState.isPrivateChat)
    MuteIcon(uiState.isChatNotificationMute)
}

@Composable
private fun UserChatStateIcon(userChatStatus: UserChatStatus?) {
    if (userChatStatus?.isValid() == true) {
        Image(
            painter = painterResource(id = userChatStatus.vectorRes(MaterialTheme.colors.isLight)),
            modifier = Modifier.testTag(TEST_TAG_USER_CHAT_STATE),
            contentDescription = "Status icon"
        )
    }
}

@Composable
private fun MuteIcon(isNotificationMute: Boolean) {
    if (isNotificationMute) {
        Icon(
            modifier = Modifier.testTag(TEST_TAG_NOTIFICATION_MUTE),
            painter = painterResource(id = R.drawable.ic_bell_off_small),
            contentDescription = "Mute icon"
        )
    }
}

@Composable
private fun PrivateChatIcon(isPrivateChat: Boolean?) {
    if (isPrivateChat == true) {
        Icon(
            modifier = Modifier
                .testTag(TEST_TAG_PRIVATE_ICON)
                .size(16.dp),
            painter = painterResource(id = R.drawable.ic_key_02),
            contentDescription = "private room icon",
        )
    }
}

private fun getChatRoomActions(uiState: ChatUiState): List<ChatRoomMenuAction> = buildList {
    with(uiState) {
        val hasModeratorPermission = myPermission == ChatRoomPermission.Moderator

        if ((hasModeratorPermission || myPermission == ChatRoomPermission.Standard)
            && !isJoiningOrLeaving && !isPreviewMode
        ) {
            add(ChatRoomMenuAction.AudioCall(!hasACallInThisChat))

            if (!isGroup) {
                add(ChatRoomMenuAction.VideoCall(!hasACallInThisChat))
            }
        }

        if (isJoiningOrLeaving.not() && isPreviewMode.not() && isConnected
            && (isGroup || myPermission != ChatRoomPermission.ReadOnly)
        ) {
            add(ChatRoomMenuAction.Info(true))
        }

        if (!isJoiningOrLeaving && isGroup && (hasModeratorPermission || isActive && isOpenInvite)) {
            add(ChatRoomMenuAction.AddParticipants)
        }
    }
}

private fun checkStorageState(context: Context, storageState: StorageState): Boolean {
    if (storageState == StorageState.PayWall) {
        context.startActivity(Intent(context, OverDiskQuotaPaywallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return false
    }
    return true
}

@Composable
private fun getSubtitle(uiState: ChatUiState) = with(uiState) {
    when {
        isArchived -> {
            stringResource(id = R.string.archived_chat)
        }

        myPermission == ChatRoomPermission.ReadOnly -> {
            stringResource(id = R.string.observer_permission_label_participants_panel)
        }

        myPermission == ChatRoomPermission.Removed -> {
            stringResource(id = R.string.inactive_chat)
        }

        !isGroup && userChatStatus?.isValid() == true -> {
            stringResource(id = userChatStatus.text)
        }

        else -> {
            ""
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            title = "My Name",
            userChatStatus = UserChatStatus.Away,
            isChatNotificationMute = true,
            isPrivateChat = true,
            myPermission = ChatRoomPermission.Standard,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {},
            onMenuActionPressed = {}
        )
    }
}

internal const val TEST_TAG_USER_CHAT_STATE = "chat_view:icon_user_chat_status"
internal const val TEST_TAG_NOTIFICATION_MUTE = "chat_view:icon_chat_notification_mute"
internal const val TEST_TAG_PRIVATE_ICON = "chat_view:icon_chat_room_private"
internal const val MENU_ACTIONS_TO_SHOW = 2