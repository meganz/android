package mega.privacy.android.app.presentation.meeting.chat.view.appbar

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserChatStatus

internal const val TEST_TAG_USER_CHAT_STATE = "chat_app_bar:icon_user_chat_status"
internal const val TEST_TAG_NOTIFICATION_MUTE = "chat_app_bar:icon_chat_notification_mute"
internal const val TEST_TAG_PRIVATE_ICON = "chat_app_bar:icon_chat_room_private"
internal const val MENU_ACTIONS_TO_SHOW = 2

@Composable
internal fun ChatAppBar(
    uiState: ChatUiState = ChatUiState(),
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    showParticipatingInACallDialog: () -> Unit = {},
    showNoContactToAddDialog: () -> Unit = {},
    showAllContactsParticipateInChat: () -> Unit = {},
    showGroupOrContactInfoActivity: () -> Unit = {},
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit = {},
    onStartCall: (Boolean) -> Unit = {},
    openAddContactActivity: () -> Unit = {},
    showClearChatConfirmationDialog: () -> Unit = {},
    showMutePushNotificationDialog: () -> Unit = {},
    archiveChat: () -> Unit = {},
    showEndCallForAllDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isVideoCall by rememberSaveable { mutableStateOf(false) }
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.RECORD_AUDIO] == true) {
            onStartCall(isVideoCall)
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
    MegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = uiState.title.orEmpty(),
        subtitle = getSubtitle(uiState = uiState),
        modifier = Modifier.clickable {
            with(uiState) {
                if (!isJoiningOrLeaving && !isPreviewMode && isConnected
                    && (isGroup || myPermission != ChatRoomPermission.ReadOnly)
                ) {
                    showGroupOrContactInfoActivity()
                }
            }
        },
        onNavigationPressed = onBackPressed,
        titleIcons = { TitleIcons(uiState) },
        marqueeSubtitle = uiState.userLastGreen != null,
        actions = getChatRoomActions(uiState),
        onActionPressed = onActionPressed@{
            when (it) {
                is ChatRoomMenuAction.AudioCall, is ChatRoomMenuAction.VideoCall -> {
                    isVideoCall = it is ChatRoomMenuAction.VideoCall
                    if (uiState.currentCall != null) {
                        showParticipatingInACallDialog()
                        return@onActionPressed
                    }
                    if (checkStorageState(context, uiState.storageState)) {
                        callPermissionsLauncher.launch(PermissionUtils.getCallPermissionListByVersion())
                    }
                }

                ChatRoomMenuAction.Info -> {
                    showGroupOrContactInfoActivity()
                }

                ChatRoomMenuAction.AddParticipants -> {
                    when {
                        !uiState.hasAnyContact -> showNoContactToAddDialog()
                        uiState.allContactsParticipateInChat -> showAllContactsParticipateInChat()
                        else -> openAddContactActivity()
                    }
                }

                ChatRoomMenuAction.Clear -> {
                    showClearChatConfirmationDialog()
                }

                ChatRoomMenuAction.Mute -> showMutePushNotificationDialog()

                ChatRoomMenuAction.Archive -> archiveChat()

                ChatRoomMenuAction.EndCallForAll -> showEndCallForAllDialog()

                else -> (it as ChatRoomMenuAction).let(onMenuActionPressed)
            }
        },
        maxActionsToShow = MENU_ACTIONS_TO_SHOW,
        elevation = 0.dp,
    )
}

private fun getChatRoomActions(uiState: ChatUiState): List<ChatRoomMenuAction> = buildList {
    with(uiState) {
        if (isJoiningOrLeaving) return@buildList

        val hasModeratorPermission = myPermission == ChatRoomPermission.Moderator

        if ((hasModeratorPermission || myPermission == ChatRoomPermission.Standard) && !isPreviewMode) {
            add(ChatRoomMenuAction.AudioCall(!hasACallInThisChat() && (!isWaitingRoom || hasModeratorPermission)))

            if (!isGroup) {
                add(ChatRoomMenuAction.VideoCall(!hasACallInThisChat()))
            }
        }

        if (isGroup && (hasModeratorPermission || isActive && isOpenInvite)) {
            add(ChatRoomMenuAction.AddParticipants)
        }

        if (isPreviewMode.not() && isConnected && (isGroup || myPermission != ChatRoomPermission.ReadOnly)
        ) {
            add(ChatRoomMenuAction.Info)
        }

        if (!isPreviewMode && isConnected
            && ((isGroup && hasModeratorPermission) || (!isGroup && myPermission != ChatRoomPermission.ReadOnly))
        ) {
            add(ChatRoomMenuAction.Clear)
        }

        if (!isPreviewMode && isConnected && !isArchived) {
            add(ChatRoomMenuAction.Archive)
        }

        if (hasModeratorPermission && (uiState.isGroup || uiState.isMeeting) && uiState.hasACallInThisChat()) {
            add(ChatRoomMenuAction.EndCallForAll)
        }

        if (!isPreviewMode && isConnected &&
            ((isGroup && isActive) || (!isGroup && hasModeratorPermission))
        ) {
            if (isChatNotificationMute) {
                add(ChatRoomMenuAction.Unmute)
            } else {
                add(ChatRoomMenuAction.Mute)
            }
        }
    }
}

private fun checkStorageState(
    context: Context,
    storageState: StorageState,
): Boolean {
    if (storageState == StorageState.PayWall) {
        context.startActivity(
            Intent(
                context,
                OverDiskQuotaPaywallActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
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
                    pluralStringResource(
                        id = R.plurals.subtitle_of_group_chat,
                        0,
                        0
                    )
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

@CombinedThemePreviews
@Composable
private fun ChatAppBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatAppBar(
            uiState = ChatUiState(
                title = "My Name",
                userChatStatus = UserChatStatus.Away,
                isChatNotificationMute = true,
                isPrivateChat = true,
                myPermission = ChatRoomPermission.Standard,
            )
        )
    }
}