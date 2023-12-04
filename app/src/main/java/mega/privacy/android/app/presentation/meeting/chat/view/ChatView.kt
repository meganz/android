package mega.privacy.android.app.presentation.meeting.chat.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.main.megachat.MapsActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.extension.isStarted
import mega.privacy.android.app.presentation.meeting.chat.extension.toInfoText
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.AllContactsAddedDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.EnableGeolocationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.EndCallForAllDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.NoContactToAddDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ParticipatingInACallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import mega.privacy.android.app.presentation.meeting.chat.view.message.MessageRow
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.controls.chat.ChatInputTextToolbar
import mega.privacy.android.core.ui.controls.chat.ChatMeetingButton
import mega.privacy.android.core.ui.controls.chat.ReturnToCallBanner
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber

@Composable
internal fun ChatView(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    ChatView(
        uiState = uiState,
        onBackPressed = { onBackPressedDispatcher?.onBackPressed() },
        onMenuActionPressed = viewModel::handleActionPress,
        enablePasscodeCheck = viewModel::enablePasscodeCheck,
        inviteContactsToChat = viewModel::inviteContactsToChat,
        onClearChatHistory = viewModel::clearChatHistory,
        onInfoToShowConsumed = viewModel::onInfoToShowEventConsumed,
        archiveChat = viewModel::archiveChat,
        unarchiveChat = viewModel::unarchiveChat,
        endCallForAll = viewModel::endCall,
        startCall = viewModel::startCall,
        onCallStarted = viewModel::onCallStarted,
        onWaitingRoomOpened = viewModel::onWaitingRoomOpened,
        onRequestMoreMessages = viewModel::requestMessages,
        onMutePushNotificationSelected = viewModel::mutePushNotification,
        onShowMutePushNotificationDialog = viewModel::showMutePushNotificationDialog,
        onShowMutePushNotificationDialogConsumed = viewModel::onShowMutePushNotificationDialogConsumed,
        onStartOrJoinMeeting = viewModel::onStartOrJoinMeeting,
        onAnswerCall = viewModel::onAnswerCall,
        onEnableGeolocation = viewModel::onEnableGeolocation,
    )
}

/**
 * Chat view
 *
 * @param uiState [ChatUiState]
 * @param onBackPressed Action to perform for back button.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@Composable
internal fun ChatView(
    uiState: ChatUiState,
    onBackPressed: () -> Unit,
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit,
    inviteContactsToChat: (Long, List<String>) -> Unit = { _, _ -> },
    onClearChatHistory: () -> Unit = {},
    onInfoToShowConsumed: () -> Unit = {},
    enablePasscodeCheck: () -> Unit = {},
    archiveChat: () -> Unit = {},
    unarchiveChat: () -> Unit = {},
    endCallForAll: () -> Unit = {},
    startCall: (Boolean) -> Unit = {},
    onCallStarted: () -> Unit = {},
    onWaitingRoomOpened: () -> Unit = {},
    onRequestMoreMessages: () -> Unit = {},
    onMutePushNotificationSelected: (ChatPushNotificationMuteOption) -> Unit = {},
    onShowMutePushNotificationDialog: () -> Unit = {},
    onShowMutePushNotificationDialogConsumed: () -> Unit = {},
    onStartOrJoinMeeting: (isStarted: Boolean) -> Unit = {},
    onAnswerCall: () -> Unit = {},
    onEnableGeolocation: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    var showParticipatingInACallDialog by rememberSaveable { mutableStateOf(false) }
    var showNoContactToAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAllContactsParticipateInChat by rememberSaveable { mutableStateOf(false) }
    var showClearChat by rememberSaveable { mutableStateOf(false) }
    var showEndCallForAllDialog by rememberSaveable { mutableStateOf(false) }
    var showMutePushNotificationDialog by rememberSaveable { mutableStateOf(false) }
    var muteNotificationDialogOptions by rememberSaveable { mutableStateOf(emptyList<ChatPushNotificationMuteOption>()) }
    var isSelectMode by rememberSaveable { mutableStateOf(false) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val toolbarModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val fileModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    var showEnableGeolocationDialog by rememberSaveable { mutableStateOf(false) }
    var waitingForPickLocation by rememberSaveable { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // Manage picked location here
            coroutineScope.launch { toolbarModalSheetState.hide() }
        }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionsResult ->
        if (permissionsResult) {
            openLocationPicker(context, locationPickerLauncher)
        } else {
            //TODO show snackbar when string is approved by content team
        }
    }
    BackHandler(enabled = toolbarModalSheetState.isVisible) {
        coroutineScope.launch {
            toolbarModalSheetState.hide()
        }
    }
    BackHandler(enabled = fileModalSheetState.isVisible) {
        coroutineScope.launch {
            fileModalSheetState.hide()
        }
    }

    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.RECORD_AUDIO] == true) {
            val isStarted = uiState.callInThisChat?.status?.isStarted == true
            onStartOrJoinMeeting(isStarted)
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

    with(uiState) {
        val addContactLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.let { intent ->
                    intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                        ?.let { contactList ->
                            inviteContactsToChat(
                                chatId,
                                contactList
                            )
                        }
                }
            }

        val attachContactLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                // TODO attach contact to chat room
            }

        val isFileModalShown = fileModalSheetState.currentValue != ModalBottomSheetValue.Hidden

        BottomSheet(
            modalSheetState = if (isFileModalShown) fileModalSheetState else toolbarModalSheetState,
            sheetBody = {
                if (isFileModalShown) {
                    ChatAttachFileBottomSheet(
                        sheetState = fileModalSheetState
                    )
                } else {
                    ChatToolbarBottomSheet(
                        onAttachFileClicked = {
                            onBackPressed()
                            coroutineScope.launch {
                                fileModalSheetState.show()
                            }
                        },
                        onAttachContactClicked = {
                            if (uiState.hasAnyContact) {
                                openAttachContactActivity(context, attachContactLauncher)
                            } else {
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar(context.getString(R.string.no_contacts_invite))
                                }
                            }
                        },
                        onPickLocation = {
                            checkLocationPicker(
                                isGeolocationEnabled = isGeolocationEnabled,
                                isPermissionGranted = locationPermissionState.status.isGranted,
                                onShowEnableGeolocationDialog = {
                                    showEnableGeolocationDialog = true
                                },
                                onAskForLocationPermission = {
                                    locationPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                onPickLocation = {
                                    openLocationPicker(context, locationPickerLauncher)
                                }
                            )
                        },
                        isLoadingGalleryFiles = isLoadingGalleryFiles,
                        sheetState = toolbarModalSheetState
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    if (isSelectMode) {
                        SelectModeAppBar(
                            title = "",
                            onNavigationPressed = {
                                isSelectMode = false
                            },
                        )
                    } else {
                        ChatAppBar(
                            uiState = uiState,
                            snackBarHostState = snackBarHostState,
                            onBackPressed = onBackPressed,
                            onMenuActionPressed = onMenuActionPressed,
                            showParticipatingInACallDialog = {
                                showParticipatingInACallDialog = true
                            },
                            showAllContactsParticipateInChat = {
                                showAllContactsParticipateInChat = true
                            },
                            showGroupOrContactInfoActivity = {
                                showGroupOrContactInfoActivity(context, uiState)
                            },
                            showNoContactToAddDialog = {
                                showNoContactToAddDialog = true
                            },
                            onStartCall = { isVideoCall ->
                                startCall(isVideoCall)
                            },
                            openAddContactActivity = {
                                openAddContactActivity(
                                    context = context,
                                    chatId = chatId,
                                    addContactLauncher = addContactLauncher
                                )
                            },
                            showClearChatConfirmationDialog = {
                                showClearChat = true
                            },
                            archiveChat = archiveChat,
                            unarchiveChat = unarchiveChat,
                            showEndCallForAllDialog = {
                                showEndCallForAllDialog = true
                            },
                            showMutePushNotificationDialog = { onShowMutePushNotificationDialog() },
                            enableSelectMode = {
                                isSelectMode = true
                            },
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState) { data ->
                        MegaSnackbar(snackbarData = data)
                    }
                },
                bottomBar = {
                    ChatInputTextToolbar(
                        onAttachmentClick = {
                            coroutineScope.launch {
                                toolbarModalSheetState.show()
                            }
                        },
                        text = uiState.sendingText,
                        placeholder = stringResource(
                            R.string.type_message_hint_with_customized_title,
                            uiState.title.orEmpty()
                        ),
                    )
                }
            )
            { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item("first_message_header") { FirstMessageHeader(uiState) }
                        items(messages) { uiChatMessage ->
                            MessageRow(
                                uiChatMessage = uiChatMessage,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (isMeeting && isActive && !isArchived) {
                        StartOrJoinMeeting(this@with, onStartOrJoinMeeting = {
                            callPermissionsLauncher.launch(PermissionUtils.getCallPermissionListByVersion())
                        })
                    }
                    ReturnToCallBanner(
                        uiState = uiState,
                        context = context,
                        isAudioPermissionGranted = audioPermissionState.status.isGranted,
                        onAnswerCall = onAnswerCall
                    )
                }

                if (showParticipatingInACallDialog) {
                    ParticipatingInACallDialog(
                        onDismiss = { showParticipatingInACallDialog = false },
                        onConfirm = {
                            showParticipatingInACallDialog = false
                            // return to active call
                            currentCall?.let {
                                enablePasscodeCheck()
                                startMeetingActivity(context, it.chatId)
                            }
                        }
                    )
                }

                if (showMutePushNotificationDialog) {
                    MutePushNotificationDialog(
                        options = muteNotificationDialogOptions,
                        isMeeting = uiState.isMeeting,
                        onCancel = { showMutePushNotificationDialog = false },
                        onConfirm = { muteOption ->
                            showMutePushNotificationDialog = false
                            onMutePushNotificationSelected(muteOption)
                        }
                    )
                }

                if (showNoContactToAddDialog) {
                    NoContactToAddDialog(
                        onDismiss = { showNoContactToAddDialog = false },
                        onConfirm = {
                            showNoContactToAddDialog = false
                            context.startActivity(
                                Intent(context, InviteContactActivity::class.java)
                            )
                        }
                    )
                }

                if (showAllContactsParticipateInChat) {
                    AllContactsAddedDialog {
                        showAllContactsParticipateInChat = false
                    }
                }

                if (showClearChat) {
                    ClearChatConfirmationDialog(
                        isMeeting = isMeeting,
                        onDismiss = { showClearChat = false },
                        onConfirm = {
                            showClearChat = false
                            onClearChatHistory()
                        })
                }

                if (showEndCallForAllDialog) {
                    EndCallForAllDialog(
                        onDismiss = { showEndCallForAllDialog = false },
                        onConfirm = {
                            showEndCallForAllDialog = false
                            endCallForAll()
                        }
                    )
                }

                if (showEnableGeolocationDialog) {
                    EnableGeolocationDialog(
                        onConfirm = {
                            waitingForPickLocation = true
                            onEnableGeolocation()
                        },
                        onDismiss = { showEnableGeolocationDialog = false },
                    )
                }

                if (waitingForPickLocation && isGeolocationEnabled) {
                    waitingForPickLocation = false
                    openLocationPicker(context, locationPickerLauncher)
                }
            }

            EventEffect(
                event = infoToShowEvent,
                onConsumed = onInfoToShowConsumed
            ) { info ->
                info?.let {
                    getInfoToShow(info, context)?.let { snackBarHostState.showSnackbar(it) }
                } ?: context.findActivity()?.finish()
            }

            EventEffect(
                event = mutePushNotificationDialogEvent,
                onConsumed = onShowMutePushNotificationDialogConsumed,
            ) { options ->
                muteNotificationDialogOptions = options
                showMutePushNotificationDialog = true
            }
        }

        if (isStartingCall && callInThisChat != null) {
            onCallStarted()
            MegaApplication.getInstance().openCallService(chatId)
            CallUtil.clearIncomingCallNotification(callInThisChat.callId)
            startMeetingActivity(
                context,
                chatId,
                enableAudio = callInThisChat.hasLocalAudio,
                enableVideo = callInThisChat.hasLocalVideo
            )
        }

        if (openWaitingRoomScreen) {
            onWaitingRoomOpened()
            startWaitingRoom(context, chatId)
        }
    }
}

@Composable
private fun BoxScope.StartOrJoinMeeting(
    uiState: ChatUiState,
    onStartOrJoinMeeting: () -> Unit = {},
) {
    val modifier = Modifier
        .padding(top = 16.dp)
        .align(Alignment.TopCenter)
    if (uiState.callInThisChat?.status?.isStarted != true) {
        ChatMeetingButton(
            modifier = modifier,
            text = stringResource(id = R.string.meetings_chat_room_start_scheduled_meeting_option),
            onClick = onStartOrJoinMeeting,
        )
    } else if (uiState.callInThisChat.status?.isJoined != true) {
        ChatMeetingButton(
            modifier = modifier,
            text = stringResource(id = R.string.meetings_chat_room_join_scheduled_meeting_option),
            onClick = onStartOrJoinMeeting,
        )
    }
}

private fun openAddContactActivity(
    context: Context,
    chatId: Long,
    addContactLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    val intent =
        Intent(context, AddContactActivity::class.java).apply {
            putExtra(
                AddContactActivity.EXTRA_CONTACT_TYPE,
                CONTACT_TYPE_MEGA
            )
            putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
            putExtra(
                Constants.INTENT_EXTRA_KEY_CHAT_ID,
                chatId
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                context.getString(R.string.add_participants_menu_item)
            )
        }

    addContactLauncher.launch(intent)
}

private fun showGroupOrContactInfoActivity(context: Context, uiState: ChatUiState) {
    with(uiState) {
        if (scheduledMeeting != null && schedIsPending && isMeeting && isActive) {
            Timber.d("show scheduled meeting info")
            Intent(context, ScheduledMeetingInfoActivity::class.java).apply {
                putExtra(Constants.CHAT_ID, scheduledMeeting.chatId)
                putExtra(Constants.SCHEDULED_MEETING_ID, scheduledMeeting.schedId)
            }.also {
                context.startActivity(it)
            }
        } else {
            val targetActivity =
                if (isGroup) GroupChatInfoActivity::class.java else ContactInfoActivity::class.java
            Intent(context, targetActivity).apply {
                putExtra(Constants.HANDLE, chatId)
                putExtra(Constants.ACTION_CHAT_OPEN, true)
            }.also {
                context.startActivity(it)
            }
        }
    }
}

private fun startMeetingActivity(
    context: Context,
    chatId: Long,
    enableAudio: Boolean? = null,
    enableVideo: Boolean? = null,
) {
    context.startActivity(Intent(context, MeetingActivity::class.java).apply {
        action =
            if (enableAudio != null && !enableAudio) MeetingActivity.MEETING_ACTION_RINGING
            else MeetingActivity.MEETING_ACTION_IN

        putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        enableAudio?.let { putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, it) }
        enableVideo?.let { putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, it) }
        addFlags(if (enableAudio != null) Intent.FLAG_ACTIVITY_NEW_TASK else Intent.FLAG_ACTIVITY_CLEAR_TOP)
    })
}

private fun openAttachContactActivity(
    context: Context,
    attachContactLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, AddContactActivity::class.java).apply {
        putExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
        putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
        putExtra(
            Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
            context.getString(R.string.send_contacts)
        )
    }.also {
        attachContactLauncher.launch(it)
    }
}

private fun startWaitingRoom(context: Context, chatId: Long) {
    context.startActivity(
        Intent(
            context,
            WaitingRoomActivity::class.java
        ).apply {
            putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
        },
    )
}

private fun getInfoToShow(infoToShow: InfoToShow, context: Context): String? = with(infoToShow) {
    inviteContactToChatResult?.toInfoText(context)
        ?: chatPushNotificationMuteOption?.toInfoText(context)
        ?: if (args.isNotEmpty()) {
            stringId?.let { context.getString(it, *args.toTypedArray()) }
        } else {
            stringId?.let { context.getString(it) }
        }
}

@Composable
private fun ReturnToCallBanner(
    uiState: ChatUiState,
    context: Context,
    isAudioPermissionGranted: Boolean,
    onAnswerCall: () -> Unit,
) = with(uiState) {
    if (!isConnected) return@with null

    val callInThisChatNotAnswered =
        callInThisChat?.status == ChatCallStatus.TerminatingUserParticipation
                || callInThisChat?.status == ChatCallStatus.UserNoPresent

    when {
        !isGroup && hasACallInThisChat && callInThisChatNotAnswered ->
            ReturnToCallBanner(
                text = stringResource(id = R.string.join_call_layout),
                onBannerClicked = {
                    if (isAudioPermissionGranted) {
                        onAnswerCall()
                    } else {
                        startMeetingActivity(context, chatId, enableAudio = false)
                    }
                })

        !isMeeting && currentCall != null ->
            ReturnToCallBanner(
                text = stringResource(id = R.string.call_in_progress_layout),
                onBannerClicked = { startMeetingActivity(context, currentCall.chatId) },
                duration = currentCall.duration
            )

        else -> null
    }
}

/**
 * Checks if location picker can be shown depending on permissions.
 */
private fun checkLocationPicker(
    isGeolocationEnabled: Boolean,
    isPermissionGranted: Boolean,
    onShowEnableGeolocationDialog: () -> Unit,
    onAskForLocationPermission: () -> Unit,
    onPickLocation: () -> Unit,
) {
    when {
        !isGeolocationEnabled -> {
            onShowEnableGeolocationDialog()
        }

        !isPermissionGranted -> {
            onAskForLocationPermission()
        }

        else -> {
            onPickLocation()
        }
    }
}

private fun openLocationPicker(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, MapsActivity::class.java).also {
        locationLauncher.launch(it)
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
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
            onMenuActionPressed = {},
            inviteContactsToChat = { _, _ -> },
        )
    }
}