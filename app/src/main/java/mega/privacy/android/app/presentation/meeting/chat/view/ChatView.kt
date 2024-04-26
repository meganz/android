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
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.camera.CameraArg
import mega.privacy.android.app.camera.InAppCameraLauncher
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.meeting.chat.extension.getOpenChatId
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.extension.isStarted
import mega.privacy.android.app.presentation.meeting.chat.model.ActionToManage
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageBottomSheetAction
import mega.privacy.android.app.presentation.meeting.chat.saver.ChatSavers
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBar
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBarParameter
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.AllContactsAddedDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.EndCallForAllDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.JoinAnswerCallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.NoContactToAddDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ParticipatingInACallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAddContactActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAttachContactActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatPicker
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startLoginActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageNotSentBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageOptionsBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ReactionsInfoBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet
import mega.privacy.android.app.presentation.meeting.view.dialog.CallRecordingConsentDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.controls.chat.ChatObserverIndicator
import mega.privacy.android.core.ui.controls.chat.ScrollToBottomFab
import mega.privacy.android.core.ui.controls.chat.VoiceClipRecordEvent
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.ChatConversationAddAttachmentButtonPressedEvent
import mega.privacy.mobile.analytics.event.ChatMessageLongPressedEvent

@Composable
internal fun ChatView(
    actionsFactories: Set<(ChatViewModel) -> MessageAction>,
    savers: ChatSavers,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val actions by remember {
        mutableStateOf(actionsFactories.map { it(viewModel) }.toSet())
    }

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
        onSendClick = viewModel::sendTextMessage,
        onHoldAndAnswerCall = viewModel::onHoldAndAnswerCall,
        onEndAndAnswerCall = viewModel::onEndAndAnswerCall,
        onJoinChat = viewModel::onJoinChat,
        onSetPendingJoinLink = viewModel::onSetPendingJoinLink,
        createNewImage = viewModel::createNewImageUri,
        onSendLocationMessage = viewModel::sendLocationMessage,
        onAttachFiles = viewModel::onAttachFiles,
        onAttachNodes = viewModel::onAttachNodes,
        onCloseEditing = viewModel::onCloseEditing,
        onAddReaction = viewModel::onAddReaction,
        onDeleteReaction = viewModel::onDeleteReaction,
        onSendGiphyMessage = viewModel::onSendGiphyMessage,
        onAttachContacts = viewModel::onAttachContacts,
        getUserInfoIntoReactionList = viewModel::getUserInfoIntoReactionList,
        getUser = viewModel::getUser,
        onForwardMessages = viewModel::onForwardMessages,
        actions = actions,
        messageSetSaver = savers.messageSetSaver,
        consumeDownloadEvent = viewModel::consumeDownloadEvent,
        onActionToManageEventConsumed = viewModel::onActionToManageEventConsumed,
        onVoiceClipRecordEvent = viewModel::onVoiceClipRecordEvent,
        onConfirmFreePlanParticipantsLimitDialogEvent = viewModel::consumeShowFreePlanParticipantsLimitDialogEvent,
        onConsumeShouldUpgradeToProPlan = viewModel::onConsumeShouldUpgradeToProPlan,
    )
}

/**
 * Chat view
 *
 * @param uiState
 * @param onBackPressed
 * @param onMenuActionPressed
 * @param inviteContactsToChat
 * @param onClearChatHistory
 * @param onInfoToShowConsumed
 * @param enablePasscodeCheck
 * @param archiveChat
 * @param unarchiveChat
 * @param endCallForAll
 * @param startCall
 * @param onCallStarted
 * @param onWaitingRoomOpened
 * @param onMutePushNotificationSelected
 * @param onShowMutePushNotificationDialog
 * @param onShowMutePushNotificationDialogConsumed
 * @param onStartOrJoinMeeting
 * @param onAnswerCall
 * @param onEnableGeolocation
 * @param onSendClick
 * @param onHoldAndAnswerCall
 * @param onEndAndAnswerCall
 * @param onJoinChat
 * @param onSetPendingJoinLink
 * @param createNewImage
 * @param onSendLocationMessage
 * @param onAttachFiles
 * @param onCloseEditing
 * @param onAddReaction
 * @param onDeleteReaction
 * @param onSendGiphyMessage
 * @param onAttachContacts
 * @param getUserInfoIntoReactionList
 * @param onForwardMessages
 * @param actions
 */
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
)
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
    onSendClick: (String) -> Unit = {},
    onHoldAndAnswerCall: () -> Unit = {},
    onEndAndAnswerCall: () -> Unit = {},
    onJoinChat: () -> Unit = {},
    onSetPendingJoinLink: () -> Unit = {},
    createNewImage: suspend () -> Uri? = { null },
    onSendLocationMessage: (Intent?) -> Unit = { _ -> },
    onAttachFiles: (List<Uri>) -> Unit = {},
    onAttachNodes: (List<NodeId>) -> Unit = {},
    onCloseEditing: () -> Unit = {},
    onAddReaction: (Long, String) -> Unit = { _, _ -> },
    onDeleteReaction: (Long, String) -> Unit = { _, _ -> },
    onSendGiphyMessage: (GifData?) -> Unit = { _ -> },
    onAttachContacts: (List<String>) -> Unit = { _ -> },
    getUserInfoIntoReactionList: suspend (List<UIReaction>) -> List<UIReaction> = { emptyList() },
    getUser: suspend (UserId) -> User? = { null },
    onForwardMessages: (Set<TypedMessage>, List<Long>?, List<Long>?) -> Unit = { _, _, _ -> },
    actions: Set<MessageAction> = setOf(),
    messageSetSaver: Saver<Set<TypedMessage>, String> = Saver(
        save = { "" },
        restore = { emptySet() }),
    consumeDownloadEvent: () -> Unit = {},
    onActionToManageEventConsumed: () -> Unit = {},
    onVoiceClipRecordEvent: (VoiceClipRecordEvent) -> Unit = {},
    onConfirmFreePlanParticipantsLimitDialogEvent: () -> Unit = {},
    onConsumeShouldUpgradeToProPlan: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    var showParticipatingInACallDialog by rememberSaveable { mutableStateOf(false) }
    var showNoContactToAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAllContactsParticipateInChat by rememberSaveable { mutableStateOf(false) }
    var showClearChat by rememberSaveable { mutableStateOf(false) }
    var showEndCallForAllDialog by rememberSaveable { mutableStateOf(false) }
    var showMutePushNotificationDialog by rememberSaveable { mutableStateOf(false) }
    var muteNotificationDialogOptions by rememberSaveable { mutableStateOf(emptyList<ChatPushNotificationMuteOption>()) }
    var isSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedMessages by rememberSaveable(stateSaver = messageSetSaver) {
        mutableStateOf(
            emptySet()
        )
    }
    val exitSelectMode = {
        isSelectMode = false
        selectedMessages = emptySet()
    }

    var showJoinAnswerCallDialog by rememberSaveable { mutableStateOf(false) }
    var showFreePlanLimitParticipantsDialog by rememberSaveable { mutableStateOf(false) }

    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var showReactionPicker by rememberSaveable { mutableStateOf(false) }
    var addingReactionTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedReaction by rememberSaveable { mutableStateOf("") }
    var reactionList by rememberSaveable { mutableStateOf(emptyList<UIReaction>()) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val keyboardController = LocalSoftwareKeyboardController.current
    val toolbarModalSheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            confirmValueChange = {
                if (it != ModalBottomSheetValue.Hidden) {
                    keyboardController?.hide()
                    showEmojiPicker = false
                }
                true
            }
        )
    val fileModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it != ModalBottomSheetValue.Hidden) {
                keyboardController?.hide()
                showEmojiPicker = false
            }
            true
        }
    )
    val messageOptionsModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it != ModalBottomSheetValue.Hidden) {
                keyboardController?.hide()
                showEmojiPicker = false
            } else {
                if (!isSelectMode) {
                    selectedMessages = emptySet()
                }
                addingReactionTo = null
            }
            true
        }
    )
    val messageNotSentBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it != ModalBottomSheetValue.Hidden) {
                keyboardController?.hide()
                showEmojiPicker = false
            }
            true
        }
    )
    val reactionInfoBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it != ModalBottomSheetValue.Hidden) {
                keyboardController?.hide()
                showEmojiPicker = false
            } else {
                selectedReaction = ""
                reactionList = emptyList()
            }
            true
        }
    )

    val upgradeToProPlanBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            true
        }
    )
    var showLocationView by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val showScrollToBottomFab by remember {
        derivedStateOf {
            scrollState.layoutInfo.totalItemsCount > 0 && scrollState.firstVisibleItemIndex != 0
        }
    }
    val interactionSourceTextInput = remember { MutableInteractionSource() }
    val isTextInputPressed by interactionSourceTextInput.collectIsPressedAsState()
    var canSelect by remember {
        mutableStateOf(false)
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
    BackHandler(enabled = messageOptionsModalSheetState.isVisible) {
        coroutineScope.launch {
            messageNotSentBottomSheetState.hide()
        }
    }
    BackHandler(enabled = messageNotSentBottomSheetState.isVisible) {
        coroutineScope.launch {
            messageNotSentBottomSheetState.hide()
        }
    }
    BackHandler(enabled = reactionInfoBottomSheetState.isVisible) {
        coroutineScope.launch { reactionInfoBottomSheetState.hide() }
    }

    BackHandler(enabled = WindowInsets.isImeVisible) {
        keyboardController?.hide()
    }
    BackHandler(enabled = showEmojiPicker) {
        showEmojiPicker = false
    }
    BackHandler(enabled = isSelectMode) {
        isSelectMode = false
        selectedMessages = emptySet()
    }
    LaunchedEffect(isTextInputPressed) {
        if (isTextInputPressed) {
            showEmojiPicker = false
        }
    }

    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.RECORD_AUDIO] == true) {
            val isStarted = uiState.callInThisChat?.status?.isStarted == true
            onStartOrJoinMeeting(isStarted)
        } else {
            showPermissionNotAllowedSnackbar(
                context,
                coroutineScope,
                scaffoldState.snackbarHostState,
                R.string.allow_acces_calls_subtitle_microphone
            )
        }
    }
    val chatPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            selectedMessages.takeUnless { it.isEmpty() }?.let { messages ->
                result.data?.let {
                    val chatHandles = it.getLongArrayExtra(Constants.SELECTED_CHATS)?.toList()
                    val contactHandles = it.getLongArrayExtra(Constants.SELECTED_USERS)?.toList()
                    onForwardMessages(messages, chatHandles, contactHandles)
                }
            }

            selectedMessages = emptySet()
        }
    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = InAppCameraLauncher()
        ) { uri ->
            uri?.let {
                onAttachFiles(listOf(it))
            }
        }
    val capturePhotoOrVideoPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.CAMERA] == true) {
            takePictureLauncher.launch(CameraArg(uiState.title.orEmpty()))
        } else {
            showPermissionNotAllowedSnackbar(
                context,
                coroutineScope,
                scaffoldState.snackbarHostState,
                R.string.chat_attach_pick_from_camera_deny_permission
            )
        }
    }
    var pendingAction: (@Composable () -> Unit)? by remember {
        mutableStateOf(null)
    }

    var unreadMessageCount by remember { mutableIntStateOf(0) }

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
                result.data?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                    ?.let { contactList ->
                        onAttachContacts(contactList.toList())
                    }
            }

        val isFileModalShown by derivedStateOf {
            fileModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isMessageOptionsModalShown by derivedStateOf {
            messageOptionsModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isMessageNotSentModalShown by derivedStateOf {
            messageNotSentBottomSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isReactionInfoModalShown by derivedStateOf {
            reactionInfoBottomSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isToolbarModalShown by derivedStateOf {
            toolbarModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }

        val isUpgradeToProPlanShown by derivedStateOf {
            upgradeToProPlanBottomSheetState.currentValue != ModalBottomSheetValue.Hidden
        }

        val noBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

        if (!isMessageOptionsModalShown && addingReactionTo == null) {
            showReactionPicker = false
        }

        if (callEndedDueToFreePlanLimits && isCallUnlimitedProPlanFeatureFlagEnabled
            && usersCallLimitReminders == UsersCallLimitReminders.Enabled
        ) {
            showFreePlanLimitParticipantsDialog = true
        }

        BottomSheet(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            modalSheetState = when {
                isFileModalShown -> fileModalSheetState
                isMessageOptionsModalShown -> messageOptionsModalSheetState
                isReactionInfoModalShown -> reactionInfoBottomSheetState
                isToolbarModalShown -> toolbarModalSheetState
                isMessageNotSentModalShown -> messageNotSentBottomSheetState
                isUpgradeToProPlanShown -> upgradeToProPlanBottomSheetState
                else -> noBottomSheetState
            },
            sheetBody = {
                when {
                    isFileModalShown -> {
                        ChatAttachFileBottomSheet(
                            onAttachFiles = onAttachFiles,
                            onAttachNodes = onAttachNodes,
                            hideSheet = { coroutineScope.launch { fileModalSheetState.hide() } },
                        )
                    }

                    isMessageOptionsModalShown -> {
                        MessageOptionsBottomSheet(
                            showReactionPicker = showReactionPicker,
                            onReactionClicked = {
                                selectedMessages.firstOrNull()
                                    ?.let { message -> onAddReaction(message.msgId, it) }
                                addingReactionTo?.let { msgId -> onAddReaction(msgId, it) }
                                coroutineScope.launch { messageOptionsModalSheetState.hide() }
                            },
                            onMoreReactionsClicked = { showReactionPicker = true },
                            actions =
                            if (selectedMessages.isEmpty()) emptyList()
                            else actions.filter { action ->
                                action.appliesTo(selectedMessages)
                            }.map { action ->
                                MessageBottomSheetAction(
                                    action.bottomSheetMenuItem(
                                        messages = selectedMessages,
                                        hideBottomSheet = {
                                            coroutineScope.launch {
                                                messageOptionsModalSheetState.hide()
                                            }
                                        },
                                        setAction = { pendingAction = it }
                                    ),
                                    action.group
                                )
                            },
                        )
                    }

                    isMessageNotSentModalShown -> {
                        MessageNotSentBottomSheet(
                            actions = actions.filter { action ->
                                action.appliesTo(selectedMessages)
                            }.map { action ->
                                action.bottomSheetMenuItem(
                                    messages = selectedMessages,
                                    hideBottomSheet = {
                                        coroutineScope.launch {
                                            messageNotSentBottomSheetState.hide()
                                        }
                                    },
                                    setAction = { pendingAction = it }
                                )
                            },
                        )
                    }

                    isReactionInfoModalShown -> {
                        ReactionsInfoBottomSheet(
                            selectedReaction = selectedReaction,
                            reactions = reactionList,
                            getDetailsInReactionList = getUserInfoIntoReactionList,
                            onUserClick = { userHandle ->
                                coroutineScope.launch {
                                    reactionInfoBottomSheetState.hide()
                                    val isMe = uiState.myUserHandle == userHandle
                                    if (isMe) {
                                        scaffoldState.snackbarHostState
                                            .showSnackbar(context.getString(R.string.contact_is_me))
                                    } else {
                                        getUser(UserId(userHandle))?.let { user ->
                                            val isUserMyContact =
                                                user.visibility == UserVisibility.Visible
                                            if (isUserMyContact) {
                                                openContactInfoActivity(context, user.email)
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }

                    isToolbarModalShown -> {
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
                                        scaffoldState.snackbarHostState
                                            .showSnackbar(context.getString(R.string.no_contacts_invite))
                                    }
                                }
                            },
                            onPickLocation = {
                                coroutineScope.launch { toolbarModalSheetState.hide() }
                                showLocationView = true
                            },
                            onSendGiphyMessage = onSendGiphyMessage,
                            onTakePicture = {
                                coroutineScope.launch {
                                    toolbarModalSheetState.hide()
                                }
                                capturePhotoOrVideoPermissionsLauncher.launch(
                                    arrayOf(
                                        PermissionUtils.getCameraPermission(),
                                        PermissionUtils.getRecordAudioPermission()
                                    )
                                )
                            },
                            onCameraPermissionDenied = {
                                showPermissionNotAllowedSnackbar(
                                    context,
                                    coroutineScope,
                                    scaffoldState.snackbarHostState,
                                    R.string.chat_attach_pick_from_camera_deny_permission
                                )
                            },
                            onAttachFiles = onAttachFiles,
                            hideSheet = { coroutineScope.launch { toolbarModalSheetState.hide() } },
                            isVisible = toolbarModalSheetState.isVisible,
                        )
                    }

                    isUpgradeToProPlanShown -> {
                        UpgradeProPlanBottomSheet(
                            onUpgrade = { coroutineScope.launch { upgradeToProPlanBottomSheetState.hide() } },
                        )
                    }
                }
            },
            sheetGesturesEnabled = !showReactionPicker,
        ) {
            MegaScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
                scaffoldState = scaffoldState,
                topBar = {
                    if (isSelectMode) {
                        SelectModeAppBar(
                            title =
                            if (selectedMessages.isEmpty()) stringResource(id = R.string.select_message_title)
                            else selectedMessages.size.toString(),
                            onNavigationPressed = exitSelectMode,
                            actions = actions.filter {
                                it.appliesTo(selectedMessages)
                            }.mapNotNull { action ->
                                action.toolbarMenuItemWithClick(
                                    messages = selectedMessages,
                                    exitSelectMode = exitSelectMode,
                                    setAction = { pendingAction = it }
                                )
                            }
                        )
                    } else {
                        Column {
                            ChatAppBar(
                                uiState = uiState,
                                snackBarHostState = scaffoldState.snackbarHostState,
                                onBackPressed = onBackPressed,
                                showParticipatingInACallDialog = {
                                    showParticipatingInACallDialog = true
                                },
                                showNoContactToAddDialog = {
                                    showNoContactToAddDialog = true
                                },
                                showAllContactsParticipateInChat = {
                                    showAllContactsParticipateInChat = true
                                },
                                showGroupOrContactInfoActivity = {
                                    showGroupOrContactInfoActivity(context, uiState)
                                },
                                onMenuActionPressed = onMenuActionPressed,
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
                                showMutePushNotificationDialog = { onShowMutePushNotificationDialog() },
                                archiveChat = archiveChat,
                                unarchiveChat = unarchiveChat,
                                showEndCallForAllDialog = {
                                    showEndCallForAllDialog = true
                                },
                                enableSelectMode = {
                                    isSelectMode = true
                                },
                                canSelect = canSelect,
                            )
                            ReturnToCallBanner(
                                uiState = uiState,
                                isAudioPermissionGranted = audioPermissionState.status.isGranted,
                                onAnswerCall = onAnswerCall
                            )
                        }
                    }
                },
                bottomBar = {
                    if (haveWritePermission) {
                        val onAttachmentClick: () -> Unit = {
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                Analytics.tracker.trackEvent(
                                    ChatConversationAddAttachmentButtonPressedEvent
                                )
                                toolbarModalSheetState.show()
                            }
                        }
                        val onEmojiClick: () -> Unit = {
                            showEmojiPicker = !showEmojiPicker

                            if (showEmojiPicker) {
                                keyboardController?.hide()
                            } else {
                                keyboardController?.show()
                            }
                        }
                        val onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit =
                            { voiceClipRecordEvent ->
                                onVoiceClipRecordEvent(voiceClipRecordEvent)
                            }
                        ChatBottomBar(
                            ChatBottomBarParameter(
                                uiState = uiState,
                                showEmojiPicker = showEmojiPicker,
                                onSendClick = onSendClick,
                                onAttachmentClick = onAttachmentClick,
                                onEmojiClick = onEmojiClick,
                                interactionSourceTextInput = interactionSourceTextInput,
                                onCloseEditing = onCloseEditing,
                                onVoiceClipEvent = onVoiceClipEvent
                            )
                        )
                    }
                    JoinChatButton(isPreviewMode = isPreviewMode, isJoining = isJoining) {
                        if (isAnonymousMode) {
                            onSetPendingJoinLink()
                            startLoginActivity(context, chatLink)
                        } else {
                            onJoinChat()
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = showScrollToBottomFab,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut(),
                    ) {
                        ScrollToBottomFab(unreadCount = unreadMessageCount) {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(0)
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
                            val onMessageLongClick: (TypedMessage) -> Unit = { message ->
                                selectedMessages = setOf(message)
                                // Use message for showing correct available options
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    Analytics.tracker.trackEvent(ChatMessageLongPressedEvent)
                                    messageOptionsModalSheetState.show()
                                }
                            }
                            val onMoreReactionsClicked: (Long) -> Unit = { msgId ->
                                addingReactionTo = msgId
                                showReactionPicker = true
                                coroutineScope.launch {
                                    messageOptionsModalSheetState.show()
                                }
                            }
                            val onReactionClicked: (Long, String, List<UIReaction>) -> Unit =
                                { msgId, clickedReaction, reactions ->
                                    reactions.find { reaction -> reaction.reaction == clickedReaction }
                                        ?.let {
                                            if (it.hasMe) {
                                                onDeleteReaction(msgId, clickedReaction)
                                            } else {
                                                onAddReaction(msgId, clickedReaction)
                                            }
                                        }
                                }
                            val onReactionLongClick: (String, List<UIReaction>) -> Unit =
                                { clickedReaction, reactions ->
                                    if (clickedReaction.isNotEmpty() && reactions.isNotEmpty()) {
                                        selectedReaction = clickedReaction
                                        reactionList = reactions
                                        coroutineScope.launch {
                                            reactionInfoBottomSheetState.show()
                                        }
                                    }
                                }
                            val onForwardClicked: (TypedMessage) -> Unit = { message ->
                                selectedMessages = setOf(message)
                                openChatPicker(context, chatId, chatPickerLauncher)
                            }
                            val onCanSelectChanged: (Boolean) -> Unit = { hasSelectableMessage ->
                                canSelect = hasSelectableMessage
                            }
                            val selectItem = { message: TypedMessage ->
                                selectedMessages = selectedMessages + message
                            }
                            val deselectItem = { message: TypedMessage ->
                                selectedMessages = selectedMessages - message
                            }

                            val onNotSentClick: (TypedMessage) -> Unit =
                                { message: TypedMessage ->
                                    selectedMessages = setOf(message)
                                    coroutineScope.launch {
                                        messageNotSentBottomSheetState.show()
                                    }
                                }

                            MessageListView(
                                MessageListParameter(
                                    uiState = uiState,
                                    scrollState = scrollState,
                                    bottomPadding = bottomPadding,
                                    onMessageLongClick = onMessageLongClick,
                                    onMoreReactionsClicked = onMoreReactionsClicked,
                                    onReactionClicked = onReactionClicked,
                                    onReactionLongClick = onReactionLongClick,
                                    onForwardClicked = onForwardClicked,
                                    onCanSelectChanged = onCanSelectChanged,
                                    selectedItems = selectedMessages.map { it.msgId }.toSet(),
                                    selectItem = selectItem,
                                    deselectItem = deselectItem,
                                    selectMode = isSelectMode,
                                    onNotSentClick = onNotSentClick,
                                    showUnreadIndicator = {
                                        unreadMessageCount = it
                                    }
                                ),
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
                            callsInOtherChats.find { it.status?.isJoined == true }?.let {
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

                if (showLocationView) {
                    ChatLocationView(
                        isGeolocationEnabled = isGeolocationEnabled,
                        onEnableGeolocation = onEnableGeolocation,
                        onSendLocationMessage = onSendLocationMessage,
                        onDismissView = { showLocationView = false },
                    )
                }

                if (showJoinAnswerCallDialog) {
                    JoinAnswerCallDialog(
                        isGroup = isGroup,
                        numberOfCallsInOtherChats = callsInOtherChats.size,
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

                if (showFreePlanLimitParticipantsDialog) {
                    FreePlanLimitParticipantsDialog(onConfirm = {
                        onConfirmFreePlanParticipantsLimitDialogEvent()
                        showFreePlanLimitParticipantsDialog = false
                    })
                }

                pendingAction?.let { it() }

                UsersInWaitingRoomDialog()
                DenyEntryToCallDialog()
                CallRecordingConsentDialog()
            }

            EventEffect(
                event = infoToShowEvent,
                onConsumed = onInfoToShowConsumed
            ) { info ->
                info?.let {
                    info.getInfo(context).let { text ->
                        when {
                            info is InfoToShow.ForwardMessagesResult -> {
                                info.result.getOpenChatId(chatId)?.let { openChatId ->
                                    val result = scaffoldState.snackbarHostState.showSnackbar(
                                        text,
                                        context.getString(R.string.general_confirmation_open)
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        openChatFragment(context, openChatId)
                                    }
                                } ?: scaffoldState.snackbarHostState.showSnackbar(text)
                            }

                            else -> scaffoldState.snackbarHostState.showSnackbar(text)
                        }
                    }
                } ?: context.findActivity()?.finish()
            }

            EventEffect(
                event = mutePushNotificationDialogEvent,
                onConsumed = onShowMutePushNotificationDialogConsumed,
            ) { options ->
                muteNotificationDialogOptions = options
                showMutePushNotificationDialog = true
            }

            LaunchedEffect(shouldUpgradeToProPlan) {
                if (shouldUpgradeToProPlan) {
                    upgradeToProPlanBottomSheetState.show()
                    onConsumeShouldUpgradeToProPlan()
                }
            }

            EventEffect(
                event = actionToManageEvent,
                onConsumed = onActionToManageEventConsumed,
            ) { action ->
                when (action) {
                    is ActionToManage.OpenChat -> openChatFragment(context, action.chatId)
                    is ActionToManage.EnableSelectMode -> isSelectMode = true
                    is ActionToManage.OpenContactInfo -> openContactInfoActivity(
                        context,
                        action.email
                    )

                    is ActionToManage.CloseChat -> onBackPressed()
                }
            }
        }

        StartTransferComponent(
            event = uiState.downloadEvent,
            onConsumeEvent = consumeDownloadEvent,
            snackBarHostState = scaffoldState.snackbarHostState,
        )

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

/**
 * Shows a permission not allowed warning.
 */
fun showPermissionNotAllowedSnackbar(
    context: Context,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    @StringRes permissionStringId: Int,
) {
    coroutineScope.launch {
        val result = snackBarHostState.showSnackbar(
            context.getString(permissionStringId),
            context.getString(R.string.general_allow),
        )
        if (result == SnackbarResult.ActionPerformed) {
            context.navigateToAppSettings()
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            userChatStatus = UserChatStatus.Away,
            isChatNotificationMute = true,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {},
            onMenuActionPressed = {},
            inviteContactsToChat = { _, _ -> },
        )
    }
}
