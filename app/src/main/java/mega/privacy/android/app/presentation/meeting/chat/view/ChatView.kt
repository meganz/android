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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.meeting.chat.extension.getOpenChatId
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.extension.isStarted
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.saver.ChatSavers
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBar
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
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatPicker
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openLocationPicker
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startLoginActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageOptionsBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ReactionsInfoBottomSheet
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.controls.chat.ChatObserverIndicator
import mega.privacy.android.core.ui.controls.chat.ScrollToBottomFab
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun ChatView(
    viewModel: ChatViewModel = hiltViewModel(),
    actionsFactories: Set<(ChatViewModel) -> MessageAction>,
    savers: ChatSavers,
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
        onSendClick = viewModel::sendTextMessage,
        onHoldAndAnswerCall = viewModel::onHoldAndAnswerCall,
        onEndAndAnswerCall = viewModel::onEndAndAnswerCall,
        onUserUpdateHandled = viewModel::onUserUpdateHandled,
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
        actions = actionsFactories.map { it(viewModel) }.toSet(),
        messageListSaver = savers.messageListSaver,
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
 * @param onUserUpdateHandled
 * @param messageListView
 * @param bottomBar
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
    ExperimentalLayoutApi::class
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
    onUserUpdateHandled: () -> Unit = {},
    messageListView: @Composable (
        ChatUiState,
        LazyListState,
        Dp,
        (TypedMessage) -> Unit,
        (Long) -> Unit,
        (Long, String, List<UIReaction>) -> Unit,
        (String, List<UIReaction>) -> Unit,
        (TypedMessage) -> Unit,
        (Boolean) -> Unit,
    ) -> Unit =
        { state, listState, bottomPadding, onMessageLongClick, onMoreReactionsClick, onReactionClick, onReactionLongClick, onForwardClick, onCanSelectChanged ->
            MessageListView(
                uiState = state,
                scrollState = listState,
                bottomPadding = bottomPadding,
                onUserUpdateHandled = onUserUpdateHandled,
                onMessageLongClick = onMessageLongClick,
                onMoreReactionsClicked = onMoreReactionsClick,
                onReactionClicked = onReactionClick,
                onReactionLongClick = onReactionLongClick,
                onForwardClicked = onForwardClick,
                onCanSelectChanged = onCanSelectChanged,
            )
        },
    bottomBar: @Composable (
        uiState: ChatUiState,
        showEmojiPicker: Boolean,
        onSendClick: (String) -> Unit,
        onAttachmentClick: () -> Unit,
        onEmojiClick: () -> Unit,
        onCloseEditing: () -> Unit,
        interactionSourceTextInput: MutableInteractionSource,
    ) -> Unit = { state, showEmojiPicker, onSendClicked, onAttachmentClick, onEmojiClick, onCloseEditingClick, interactionSourceTextInput ->
        ChatBottomBar(
            uiState = state,
            showEmojiPicker = showEmojiPicker,
            onSendClick = onSendClicked,
            onAttachmentClick = onAttachmentClick,
            onEmojiClick = onEmojiClick,
            interactionSourceTextInput = interactionSourceTextInput,
            onCloseEditing = onCloseEditingClick
        )
    },
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
    onForwardMessages: (List<TypedMessage>, List<Long>?, List<Long>?) -> Unit = { _, _, _ -> },
    actions: Set<MessageAction> = setOf(),
    messageListSaver: Saver<List<TypedMessage>, String> = Saver(
        save = { "" },
        restore = { emptyList() }),
) {
    val context = LocalContext.current
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
    var showJoinAnswerCallDialog by rememberSaveable { mutableStateOf(false) }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var showReactionPicker by rememberSaveable { mutableStateOf(false) }
    var addingReactionTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedReaction by rememberSaveable { mutableStateOf("") }
    var reactionList by rememberSaveable { mutableStateOf(emptyList<UIReaction>()) }
    var selectedMessages by rememberSaveable(stateSaver = messageListSaver) {
        mutableStateOf(
            emptyList()
        )
    }
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
                selectedMessages = emptyList()
                addingReactionTo = null
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
    var showEnableGeolocationDialog by rememberSaveable { mutableStateOf(false) }
    var waitingForPickLocation by rememberSaveable { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            onSendLocationMessage(it.data)
            coroutineScope.launch { toolbarModalSheetState.hide() }
        }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openLocationPicker(context, locationPickerLauncher)
        } else {
            coroutineScope.launch { toolbarModalSheetState.hide() }
            showPermissionNotAllowedSnackbar(
                context,
                coroutineScope,
                scaffoldState.snackbarHostState,
                R.string.chat_attach_location_deny_permission
            )
        }
    }
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
    BackHandler(enabled = WindowInsets.isImeVisible) {
        keyboardController?.hide()
    }
    BackHandler(enabled = showEmojiPicker) {
        showEmojiPicker = false
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

            selectedMessages = emptyList()
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
                result.data?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                    ?.let { contactList ->
                        onAttachContacts(contactList.toList())
                    }
            }
        var takePictureUri by remember { mutableStateOf(Uri.EMPTY) }
        val takePictureLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { resultOk ->
                if (resultOk) {
                    onAttachFiles(listOf(takePictureUri))
                }
                takePictureUri = Uri.EMPTY
            }

        val isFileModalShown by derivedStateOf {
            fileModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isMessageOptionsModalShown by derivedStateOf {
            messageOptionsModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isReactionInfoModalShown by derivedStateOf {
            reactionInfoBottomSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val isToolbarModalShown by derivedStateOf {
            toolbarModalSheetState.currentValue != ModalBottomSheetValue.Hidden
        }
        val noBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

        if (!isMessageOptionsModalShown && addingReactionTo == null) {
            showReactionPicker = false
        }

        BottomSheet(
            modalSheetState = when {
                isFileModalShown -> fileModalSheetState
                isMessageOptionsModalShown -> messageOptionsModalSheetState
                isReactionInfoModalShown -> reactionInfoBottomSheetState
                isToolbarModalShown -> toolbarModalSheetState
                else -> noBottomSheetState
            },
            sheetBody = {
                when {
                    isFileModalShown -> {
                        ChatAttachFileBottomSheet(
                            onAttachFiles = onAttachFiles,
                            onAttachNodes = onAttachNodes,
                            sheetState = fileModalSheetState,
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
                            actions = actions.filter { action ->
                                action.appliesTo(selectedMessages)
                            }.map {
                                it.bottomSheetMenuItem(
                                    messages = selectedMessages,
                                    chatId = chatId,
                                    context = context
                                )
                            },
                            sheetState = messageOptionsModalSheetState,
                        )
                    }

                    isReactionInfoModalShown -> {
                        ReactionsInfoBottomSheet(
                            selectedReaction = selectedReaction,
                            reactions = reactionList,
                            sheetState = reactionInfoBottomSheetState,
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
                            onSendGiphyMessage = onSendGiphyMessage,
                            onTakePicture = {
                                coroutineScope.launch {
                                    createNewImage()?.let {
                                        takePictureUri = it
                                        takePictureLauncher.launch(it)
                                    }
                                    toolbarModalSheetState.hide()
                                }
                            },
                            sheetState = toolbarModalSheetState,
                            onCameraPermissionDenied = {
                                showPermissionNotAllowedSnackbar(
                                    context,
                                    coroutineScope,
                                    scaffoldState.snackbarHostState,
                                    R.string.chat_attach_pick_from_camera_deny_permission
                                )
                            },
                            onAttachFiles = onAttachFiles
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
                            title = "",
                            onNavigationPressed = {
                                isSelectMode = false
                            },
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
                        bottomBar(
                            uiState,
                            showEmojiPicker,
                            onSendClick,
                            {
                                coroutineScope.launch {
                                    toolbarModalSheetState.show()
                                }
                            },
                            {
                                showEmojiPicker = !showEmojiPicker

                                if (showEmojiPicker) {
                                    keyboardController?.hide()
                                } else {
                                    keyboardController?.show()
                                }
                            },
                            onCloseEditing,
                            interactionSourceTextInput,
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
                        ScrollToBottomFab {
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
                            messageListView(
                                uiState,
                                scrollState,
                                bottomPadding,
                                { message ->
                                    selectedMessages = listOf(message)
                                    // Use message for showing correct available options
                                    coroutineScope.launch {
                                        messageOptionsModalSheetState.show()
                                    }
                                },
                                { msgId ->
                                    addingReactionTo = msgId
                                    showReactionPicker = true
                                    coroutineScope.launch {
                                        messageOptionsModalSheetState.show()
                                    }
                                },
                                { msgId, clickedReaction, reactions ->
                                    reactions.find { reaction -> reaction.reaction == clickedReaction }
                                        ?.let {
                                            if (it.hasMe) {
                                                onDeleteReaction(msgId, clickedReaction)
                                            } else {
                                                onAddReaction(msgId, clickedReaction)
                                            }
                                        }
                                },
                                { clickedReaction, reactions ->
                                    if (clickedReaction.isNotEmpty() && reactions.isNotEmpty()) {
                                        selectedReaction = clickedReaction
                                        reactionList = reactions
                                        coroutineScope.launch {
                                            reactionInfoBottomSheetState.show()
                                        }
                                    }
                                },
                                { message ->
                                    selectedMessages = listOf(message)
                                    openChatPicker(context, chatId, chatPickerLauncher)
                                },
                                { hasSelectableMessage -> canSelect = hasSelectableMessage },
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
            }

            EventEffect(
                event = infoToShowEvent,
                onConsumed = onInfoToShowConsumed
            ) { info ->
                info?.let {
                    info.getInfo(context).let { text ->
                        if (info is InfoToShow.ForwardMessagesResult) {
                            info.result.getOpenChatId(chatId)?.let { openChatId ->
                                val result = scaffoldState.snackbarHostState.showSnackbar(
                                    text,
                                    context.getString(R.string.general_confirmation_open)
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    openChatFragment(context, openChatId, null)
                                }
                            } ?: scaffoldState.snackbarHostState.showSnackbar(text)
                        } else {
                            scaffoldState.snackbarHostState.showSnackbar(text)
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

private fun showPermissionNotAllowedSnackbar(
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