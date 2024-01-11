@file:OptIn(ExperimentalComposeUiApi::class)

package mega.privacy.android.app.presentation.meeting.chat.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.JoinAnswerCallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.NoContactToAddDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ParticipatingInACallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAddContactActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAttachContactActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openLocationPicker
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startLoginActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.chat.ChatInputTextToolbar
import mega.privacy.android.core.ui.controls.chat.ChatObserverIndicator
import mega.privacy.android.core.ui.controls.chat.ScrollToBottomFab
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.theme.MegaAppTheme

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
        onMutePushNotificationSelected = viewModel::mutePushNotification,
        onShowMutePushNotificationDialog = viewModel::showMutePushNotificationDialog,
        onShowMutePushNotificationDialogConsumed = viewModel::onShowMutePushNotificationDialogConsumed,
        onStartOrJoinMeeting = viewModel::onStartOrJoinMeeting,
        onAnswerCall = viewModel::onAnswerCall,
        onEnableGeolocation = viewModel::onEnableGeolocation,
        onSendClick = viewModel::sendMessage,
        onHoldAndAnswerCall = viewModel::onHoldAndAnswerCall,
        onEndAndAnswerCall = viewModel::onEndAndAnswerCall,
        onUserUpdateHandled = viewModel::onUserUpdateHandled,
        onJoinChat = viewModel::onJoinChat,
        onSetPendingJoinLink = viewModel::onSetPendingJoinLink,
        createNewImage = viewModel::createNewImageUri,
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
    onMutePushNotificationSelected: (ChatPushNotificationMuteOption) -> Unit = {},
    onShowMutePushNotificationDialog: () -> Unit = {},
    onShowMutePushNotificationDialogConsumed: () -> Unit = {},
    onStartOrJoinMeeting: (isStarted: Boolean) -> Unit = {},
    onAnswerCall: () -> Unit = {},
    onEnableGeolocation: () -> Unit = {},
    onUserUpdateHandled: () -> Unit = {},
    messageListView: @Composable (ChatUiState, LazyListState, Dp) -> Unit = { state, listState, bottomPadding ->
        MessageListView(
            uiState = state,
            scrollState = listState,
            bottomPadding = bottomPadding,
            onUserUpdateHandled = onUserUpdateHandled,
        )
    },
    onSendClick: (String) -> Unit = {},
    onHoldAndAnswerCall: () -> Unit = {},
    onEndAndAnswerCall: () -> Unit = {},
    onJoinChat: () -> Unit = {},
    onSetPendingJoinLink: () -> Unit = {},
    createNewImage: suspend () -> Uri? = { null },
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
    var showJoinAnswerCallDialog by rememberSaveable { mutableStateOf(false) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val keyboardController = LocalSoftwareKeyboardController.current
    val toolbarModalSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden,
            confirmValueChange = {
                if (it != ModalBottomSheetValue.Hidden) {
                    keyboardController?.hide()
                }
                true
            }
        )
    val fileModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it != ModalBottomSheetValue.Hidden) {
                keyboardController?.hide()
            }
            true
        }
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
    val scrollState = rememberLazyListState()
    val showScrollToBottomFab by remember {
        derivedStateOf {
            scrollState.layoutInfo.totalItemsCount > 0
                    && scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != scrollState.layoutInfo.totalItemsCount - 1
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

        val takePictureLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) {
                if (it) {

                }
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
                        onTakePicture = {
                            coroutineScope.launch {
                                createNewImage()?.let(takePictureLauncher::launch)
                            }
                        },
                        isLoadingGalleryFiles = isLoadingGalleryFiles,
                        sheetState = toolbarModalSheetState
                    )
                }
            },
        ) {
            MegaScaffold(
                topBar = {
                    if (isSelectMode) {
                        SelectModeAppBar(
                            title = "",
                            onNavigationPressed = {
                                isSelectMode = false
                            },
                        )
                    } else {
                        Column {
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
                            ReturnToCallBanner(
                                uiState = uiState,
                                isAudioPermissionGranted = audioPermissionState.status.isGranted,
                                onAnswerCall = onAnswerCall
                            )
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState) { data ->
                        MegaSnackbar(snackbarData = data)
                    }
                },
                bottomBar = {
                    if (myPermission == ChatRoomPermission.Standard || myPermission == ChatRoomPermission.Moderator) {
                        Column {
                            UserTypingView(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                usersTyping = uiState.usersTyping,
                            )
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
                                onSendClick = onSendClick
                            )
                        }
                    }
                    if (isPreviewMode) {
                        RaisedDefaultMegaButton(
                            textId = R.string.action_join,
                            onClick = {
                                if (isAnonymousMode) {
                                    onSetPendingJoinLink()
                                    startLoginActivity(context, chatLink)
                                } else {
                                    onJoinChat()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = showScrollToBottomFab,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut(),
                    ) {
                        ScrollToBottomFab {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
                            }
                        }
                    }
                }
            )
            { paddingValues ->

                if (uiState.chatId != -1L) {
                    ChatContentView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        topViews = {
                            TopCallButton(this@with, onStartOrJoinMeeting = {
                                callPermissionsLauncher.launch(PermissionUtils.getCallPermissionListByVersion())
                            })
                            if (numPreviewers > 0) {
                                ChatObserverIndicator(numObservers = numPreviewers.toString())
                            }
                        },
                        bottomViews = {
                            BottomCallButton(
                                uiState = this@with,
                                enablePasscodeCheck = enablePasscodeCheck,
                                onJoinAnswerCallClick = { showJoinAnswerCallDialog = true }
                            )
                        },
                        listView = { bottomPadding ->
                            messageListView(
                                uiState,
                                scrollState,
                                bottomPadding
                            )
                        },
                    )
                }

                if (showParticipatingInACallDialog) {
                    ParticipatingInACallDialog(
                        onDismiss = { showParticipatingInACallDialog = false },
                        onConfirm = {
                            showParticipatingInACallDialog = false
                            // return to active call
                            callInOtherChat?.let {
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

                if (showJoinAnswerCallDialog) {
                    JoinAnswerCallDialog(
                        isGroup = isGroup,
                        onHoldAndAnswer = {
                            showJoinAnswerCallDialog = false
                            onHoldAndAnswerCall()
                        },
                        onEndAndAnswer = {
                            showJoinAnswerCallDialog = false
                            onEndAndAnswerCall()
                        },
                        onDismiss = { showJoinAnswerCallDialog = false },
                    )
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

private fun getInfoToShow(infoToShow: InfoToShow, context: Context): String? = with(infoToShow) {
    inviteContactToChatResult?.toInfoText(context)
        ?: chatPushNotificationMuteOption?.toInfoText(context)
        ?: if (args.isNotEmpty()) {
            stringId?.let { context.getString(it, *args.toTypedArray()) }
        } else {
            stringId?.let { context.getString(it) }
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