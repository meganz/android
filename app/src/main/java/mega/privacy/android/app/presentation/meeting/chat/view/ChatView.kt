package mega.privacy.android.app.presentation.meeting.chat.view

import mega.privacy.android.core.R as CoreR
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.extensions.vectorRes
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.view.getRecurringMeetingDateTime
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderParagraph
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderSubtitleWithIcon
import mega.privacy.android.core.ui.controls.chat.FirstMessageHeaderTitle
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import timber.log.Timber

@Composable
internal fun ChatView(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    EventEffect(
        event = uiState.openMeetingEvent,
        onConsumed = viewModel::consumeOpenMeetingEvent,
    ) { chatId ->
        startMeetingActivity(context, chatId)
    }

    ChatView(
        uiState = uiState,
        onBackPressed = { onBackPressedDispatcher?.onBackPressed() },
        onMenuActionPressed = viewModel::handleActionPress,
        getAnotherCallParticipating = viewModel::getAnotherCallParticipating,
    )
}

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
    getAnotherCallParticipating: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.RECORD_AUDIO] == true) {
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
    var showNoContactToAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAllContactsParticipateInChat by rememberSaveable { mutableStateOf(false) }

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

                        is ChatRoomMenuAction.AddParticipants -> {
                            if (!uiState.hasAnyContact) {
                                showNoContactToAddDialog = true

                            }
                            if (uiState.allContactsParticipateInChat) {
                                showAllContactsParticipateInChat = true
                            }
                        }

                        else -> (it as ChatRoomMenuAction).let(onMenuActionPressed)
                    }
                },
                maxActionsToShow = MENU_ACTIONS_TO_SHOW,
                subtitle = getSubtitle(uiState = uiState),
                marqueeSubtitle = uiState.userLastGreen != null,
                elevation = 0.dp
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    )
    { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item("first_message_header") { FirstMessageHeader(uiState, context) }
        }

        if (showParticipatingInACallDialog) {
            ParticipatingInACallDialog(
                onDismiss = { showParticipatingInACallDialog = false },
                onConfirm = {
                    showParticipatingInACallDialog = false
                    // return to active call
                    getAnotherCallParticipating()
                }
            )
        }

        if (showNoContactToAddDialog) {
            ConfirmationDialog(
                title = stringResource(id = R.string.chat_add_participants_no_contacts_title),
                text = stringResource(id = R.string.chat_add_participants_no_contacts_message),
                cancelButtonText = stringResource(id = R.string.button_cancel),
                confirmButtonText = stringResource(id = R.string.contact_invite),
                onDismiss = { showNoContactToAddDialog = false },
                onConfirm = {
                    showNoContactToAddDialog = false
                    context.startActivity(Intent(context, InviteContactActivity::class.java))
                }
            )
        }

        if (showAllContactsParticipateInChat) {
            AllContactsAddedDialog {
                showAllContactsParticipateInChat = false
            }
        }
    }
}

@Composable
private fun FirstMessageHeader(uiState: ChatUiState, context: Context) {
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }
    Column(
        modifier = Modifier.padding(start = 72.dp, top = 40.dp, end = 24.dp),
    ) {
        uiState.title?.let { title ->
            val subtitle = uiState.scheduledMeeting?.let { scheduledMeeting ->
                getRecurringMeetingDateTime(
                    scheduledMeeting = scheduledMeeting,
                    is24HourFormat = is24HourFormat,
                ).text
            }
            FirstMessageHeaderTitle(
                title = title,
                subtitle = subtitle,
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
    if (uiState.scheduledMeeting != null && uiState.schedIsPending && uiState.isMeeting && uiState.isActive) {
        Timber.d("show scheduled meeting info")
        Intent(context, ScheduledMeetingInfoActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, uiState.scheduledMeeting.chatId)
            putExtra(Constants.SCHEDULED_MEETING_ID, uiState.scheduledMeeting.schedId)
        }.also {
            context.startActivity(it)
        }
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
        !isConnected -> {
            stringResource(id = R.string.invalid_connection_state)
        }

        isArchived -> {
            stringResource(id = R.string.archived_chat)
        }

        myPermission == ChatRoomPermission.ReadOnly -> {
            stringResource(id = R.string.observer_permission_label_participants_panel)
        }

        myPermission == ChatRoomPermission.Removed -> {
            stringResource(id = R.string.inactive_chat)
        }

        userLastGreen != null -> {
            getLastSeenString(userLastGreen) ?: ""
        }

        !isGroup && userChatStatus?.isValid() == true -> {
            stringResource(id = userChatStatus.text)
        }

        customSubtitleList != null -> {
            getCustomSubtitle(this)
        }

        participantsCount != null -> {
            pluralStringResource(
                id = R.plurals.subtitle_of_group_chat,
                participantsCount.toInt(),
                participantsCount.toInt()
            )
        }

        else -> {
            ""
        }
    }
}

@Composable
private fun getCustomSubtitle(uiState: ChatUiState): String = with(uiState) {
    customSubtitleList?.let {
        val me = stringResource(id = R.string.bucket_word_me)
        when {
            customSubtitleList.isEmpty() -> {
                if (isPreviewMode) {
                    pluralStringResource(id = R.plurals.subtitle_of_group_chat, 0, 0)
                } else {
                    me
                }
            }

            customSubtitleList.size == 1 -> {
                if (isPreviewMode) {
                    customSubtitleList[0]
                } else {
                    "${customSubtitleList[0]}, $me"
                }
            }

            customSubtitleList.size == 2 -> {
                if (isPreviewMode) {
                    "${customSubtitleList[0]}, ${customSubtitleList[1]}"
                } else {
                    "${customSubtitleList[0]}, ${customSubtitleList[1]}, $me"
                }
            }

            customSubtitleList.size == 3 -> {
                if (isPreviewMode) {
                    "${customSubtitleList[0]}, ${customSubtitleList[1]}, ${customSubtitleList[2]}"
                } else {
                    "${customSubtitleList[0]}, ${customSubtitleList[1]}, ${customSubtitleList[2]}, $me"
                }
            }

            else -> {
                stringResource(
                    id = R.string.custom_subtitle_of_group_chat,
                    "${customSubtitleList[0]}, ${customSubtitleList[1]}, ${customSubtitleList[2]}",
                    customSubtitleList[3].toInt()
                )
            }
        }
    } ?: ""
}

private fun startMeetingActivity(context: Context, chatId: Long) {
    context.startActivity(Intent(context, MeetingActivity::class.java).apply {
        action = MeetingActivity.MEETING_ACTION_IN
        putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    })
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