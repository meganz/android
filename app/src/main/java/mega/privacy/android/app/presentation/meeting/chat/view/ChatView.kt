@file:OptIn(ExperimentalMaterialNavigationApi::class)

package mega.privacy.android.app.presentation.meeting.chat.view

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.meeting.chat.extension.getOpenChatId
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.extension.isStarted
import mega.privacy.android.app.presentation.meeting.chat.model.ActionToManage
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBar
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAddContactActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatPicker
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startLoginActivity
import mega.privacy.android.app.presentation.meeting.view.dialog.CallRecordingConsentDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.shared.original.core.ui.controls.chat.ChatObserverIndicator
import mega.privacy.android.shared.original.core.ui.controls.chat.ScrollToBottomFab
import mega.privacy.android.shared.original.core.ui.controls.chat.VoiceClipRecordEvent
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ChatConversationAddAttachmentButtonPressedEvent
import mega.privacy.mobile.analytics.event.ChatMessageLongPressedEvent

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
 * @param getApplicableActions
 */
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
internal fun ChatView(
    bottomSheetNavigator: BottomSheetNavigator,
    uiState: ChatUiState,
    onBackPressed: () -> Unit,
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit,
    scaffoldState: ScaffoldState,
    setSelectedMessages: (Set<TypedMessage>) -> Unit,
    setSelectMode: (Boolean) -> Unit,
    setSelectedReaction: (String) -> Unit,
    setReactionList: (List<UIReaction>) -> Unit,
    setPendingAction: ((@Composable () -> Unit)?) -> Unit,
    setAddingReactionTo: (Long?) -> Unit,
    getApplicableActions: () -> List<MessageAction>,
    inviteContactsToChat: (Long, List<String>) -> Unit = { _, _ -> },
    onInfoToShowConsumed: () -> Unit = {},
    enablePasscodeCheck: () -> Unit = {},
    archiveChat: () -> Unit = {},
    unarchiveChat: () -> Unit = {},
    startCall: (Boolean) -> Unit = {},
    onCallStarted: () -> Unit = {},
    onWaitingRoomOpened: () -> Unit = {},
    onStartOrJoinMeeting: (isStarted: Boolean) -> Unit = {},
    onAnswerCall: () -> Unit = {},
    onSendClick: (String) -> Unit = {},
    onJoinChat: () -> Unit = {},
    onSetPendingJoinLink: () -> Unit = {},
    onCloseEditing: () -> Unit = {},
    onAddReaction: (Long, String) -> Unit = { _, _ -> },
    onDeleteReaction: (Long, String) -> Unit = { _, _ -> },
    onForwardMessages: (Set<TypedMessage>, List<Long>?, List<Long>?) -> Unit = { _, _, _ -> },
    consumeDownloadEvent: () -> Unit = {},
    onActionToManageEventConsumed: () -> Unit = {},
    onVoiceClipRecordEvent: (VoiceClipRecordEvent) -> Unit = {},
    onConsumeShouldUpgradeToProPlan: () -> Unit = {},
    navigateToFreePlanLimitParticipants: () -> Unit,
    showOptionsModal: (Long) -> Unit,
    showEmojiModal: (Long) -> Unit,
    showNoContactToAddDialog: () -> Unit,
    showParticipatingInACallDialog: (Long) -> Unit,
    showAllContactsParticipateInChat: () -> Unit,
    showGroupOrContactInfoActivity: (ChatUiState) -> Unit,
    showClearChatConfirmationDialog: (Boolean) -> Unit,
    showMutePushNotificationDialog: (Boolean) -> Unit,
    showEndCallForAllDialog: () -> Unit,
    showToolbarModal: () -> Unit,
    showJoinCallDialog: (Boolean, Int) -> Unit,
    showUpgradeToProDialog: () -> Unit,
    navigateToChat: (Long) -> Unit,
    navigateToContactInfo: (String) -> Unit,
    navigateToMeeting: (Long, Boolean, Boolean) -> Unit,
    navigateToWaitingRoom: (Long) -> Unit,
    navigateToReactionInfo: () -> Unit,
    navigateToNotSentModal: () -> Unit,
    navigateToConversation: (Long) -> Unit,
    navHostController: NavHostController,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val keyboardController = LocalSoftwareKeyboardController.current

    val scrollState: LazyListState = rememberLazyListState()
    val showScrollToBottomFab by remember {
        derivedStateOf {
            scrollState.layoutInfo.totalItemsCount > 0 && scrollState.firstVisibleItemIndex != 0
        }
    }

    var canSelect by remember {
        mutableStateOf(false)
    }


    BackHandler(enabled = WindowInsets.isImeVisible) {
        keyboardController?.hide()
    }

    BackHandler(enabled = uiState.isSelectMode) {
        setSelectMode(false)
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
            uiState.selectedMessages.takeUnless { it.isEmpty() }?.let { messages ->
                result.data?.let {
                    val chatHandles = it.getLongArrayExtra(Constants.SELECTED_CHATS)?.toList()
                    val contactHandles = it.getLongArrayExtra(Constants.SELECTED_USERS)?.toList()
                    onForwardMessages(messages, chatHandles, contactHandles)
                }
            }

            setSelectedMessages(emptySet())
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

        if (callEndedDueToFreePlanLimits && isCallUnlimitedProPlanFeatureFlagEnabled
            && usersCallLimitReminders == UsersCallLimitReminders.Enabled
        ) {
            navigateToFreePlanLimitParticipants()
        }


//        ChatContent parameters
        val onStartOrJoinMeetingTopCallButton: () -> Unit = {
            callPermissionsLauncher.launch(PermissionUtils.getCallPermissionListByVersion())
        }


// MessageListViewParameters


        val onMessageLongClick: (TypedMessage) -> Unit = { message ->
            setSelectedMessages(setOf(message))
            // Use message for showing correct available options
            focusManager.clearFocus()
            Analytics.tracker.trackEvent(ChatMessageLongPressedEvent)
            showOptionsModal(message.msgId)
        }
        val onListViewMoreReactionsClicked: (Long) -> Unit = { msgId ->
            setAddingReactionTo(msgId)
            showEmojiModal(msgId)
        }
        val onListViewReactionClicked: (Long, String, List<UIReaction>) -> Unit =
            { msgId, clickedReaction, reactions ->
                reactions.find { reaction -> reaction.reaction == clickedReaction }?.let {
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
                    setSelectedReaction(clickedReaction)
                    setReactionList(reactions)
                    navigateToReactionInfo()
                }
            }
        val onForwardClicked: (TypedMessage) -> Unit = { message ->
            setSelectedMessages(setOf(message))
            openChatPicker(context, chatId, chatPickerLauncher)
        }
        val onCanSelectChanged: (Boolean) -> Unit = { hasSelectableMessage ->
            canSelect = hasSelectableMessage
        }
        val selectItem: (TypedMessage) -> Unit = { message: TypedMessage ->
            setSelectedMessages(uiState.selectedMessages + message)
        }
        val deselectItem: (TypedMessage) -> Unit = { message: TypedMessage ->
            setSelectedMessages(uiState.selectedMessages - message)
        }

        val onNotSentClick: (TypedMessage) -> Unit = { message: TypedMessage ->
            setSelectedMessages(setOf(message))
            navigateToNotSentModal()
        }

        val selectedMessageIdSet = selectedMessages.map { it.msgId }.toSet()
        val showUnreadIndicator: (Int) -> Unit = {
            unreadMessageCount = it
        }

        MegaBottomSheetLayout(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            bottomSheetNavigator = bottomSheetNavigator,
        ) {
            MegaScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
                scaffoldState = scaffoldState,
                topBar = {
                    if (isSelectMode) {
                        val selectModeActions = getApplicableActions().mapNotNull { action ->
                            action.toolbarMenuItemWithClick(
                                messages = uiState.selectedMessages,
                                exitSelectMode = { setSelectMode(false) },
                                setAction = setPendingAction
                            )
                        }
                        SelectModeAppBar(
                            title = if (uiState.selectedMessages.isEmpty()) stringResource(id = R.string.select_message_title) else uiState.selectedMessages.size.toString(),
                            actions = selectModeActions,
                            onNavigationPressed = { setSelectMode(false) },
                        )
                    } else {
                        Column {
                            ChatAppBar(
                                uiState = uiState,
                                snackBarHostState = scaffoldState.snackbarHostState,
                                onBackPressed = onBackPressed,
                                showParticipatingInACallDialog = {
                                    val callChatId =
                                        uiState.callsInOtherChats.find { it.status?.isJoined == true }?.chatId
                                            ?: -1L
                                    showParticipatingInACallDialog(callChatId)
                                },
                                showNoContactToAddDialog = showNoContactToAddDialog,
                                showAllContactsParticipateInChat = showAllContactsParticipateInChat,
                                showGroupOrContactInfoActivity = {
                                    showGroupOrContactInfoActivity(uiState)
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
                                    showClearChatConfirmationDialog(
                                        isMeeting
                                    )
                                },
                                showMutePushNotificationDialog = {
                                    showMutePushNotificationDialog(
                                        isMeeting
                                    )
                                },
                                archiveChat = archiveChat,
                                unarchiveChat = unarchiveChat,
                                showEndCallForAllDialog = showEndCallForAllDialog,
                                enableSelectMode = {
                                    setSelectMode(true)
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
                                showToolbarModal()
                            }
                        }

                        val onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit =
                            { voiceClipRecordEvent ->
                                onVoiceClipRecordEvent(voiceClipRecordEvent)
                            }
                        ChatBottomBar(
                            uiState = uiState,
                            onSendClick = onSendClick,
                            onAttachmentClick = onAttachmentClick,
                            onCloseEditing = onCloseEditing,
                            onVoiceClipEvent = onVoiceClipEvent,
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
                },
                scrollableContentState = scrollState,
                scrollableContentIsReversed = true,
            )
            { paddingValues ->
                if (chatId != -1L) {
                    ChatContentView(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        topViews = {
                            TopCallButton(
                                uiState = uiState,
                                onStartOrJoinMeeting = onStartOrJoinMeetingTopCallButton
                            )
                            if (uiState.numPreviewers > 0) {
                                ChatObserverIndicator(numObservers = uiState.numPreviewers.toString())
                            }
                        },
                        bottomViews = {
                            BottomCallButton(
                                uiState = uiState,
                                enablePasscodeCheck = enablePasscodeCheck,
                                onJoinAnswerCallClick = {
                                    showJoinCallDialog(
                                        isGroup,
                                        callsInOtherChats.size
                                    )
                                }
                            )
                        },
                        listView = { bottomPadding ->
                            MessageListView(
                                uiState = uiState,
                                scrollState = scrollState,
                                bottomPadding = bottomPadding,
                                onMessageLongClick = onMessageLongClick,
                                onMoreReactionsClicked = onListViewMoreReactionsClicked,
                                onReactionClicked = onListViewReactionClicked,
                                onReactionLongClick = onReactionLongClick,
                                onForwardClicked = onForwardClicked,
                                onCanSelectChanged = onCanSelectChanged,
                                selectedItems = selectedMessageIdSet,
                                selectItem = selectItem,
                                deselectItem = deselectItem,
                                selectMode = isSelectMode,
                                onNotSentClick = onNotSentClick,
                                showUnreadIndicator = showUnreadIndicator,
                                navHostController = navHostController,
                            )
                        },
                    )
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
                                    val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                                        text,
                                        context.getString(R.string.general_confirmation_open)
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        navigateToConversation(openChatId)
                                    }
                                } ?: scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                            }

                            else -> scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                        }
                    }
                } ?: context.findActivity()?.finish()
            }

            LaunchedEffect(shouldUpgradeToProPlan) {
                if (shouldUpgradeToProPlan) {
                    showUpgradeToProDialog()
                    onConsumeShouldUpgradeToProPlan()
                }
            }

            EventEffect(
                event = actionToManageEvent,
                onConsumed = onActionToManageEventConsumed,
            ) { action ->
                when (action) {
                    is ActionToManage.OpenChat -> navigateToChat(action.chatId)
                    is ActionToManage.EnableSelectMode -> setSelectMode(true)
                    is ActionToManage.OpenContactInfo -> navigateToContactInfo(action.email)
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
            navigateToMeeting(
                chatId,
                callInThisChat.hasLocalAudio,
                callInThisChat.hasLocalVideo
            )
        }

        if (openWaitingRoomScreen) {
            onWaitingRoomOpened()
            navigateToWaitingRoom(chatId)
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
        val result = snackBarHostState.showAutoDurationSnackbar(
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            userChatStatus = UserChatStatus.Away,
            isChatNotificationMute = true,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {},
            onMenuActionPressed = {},
            inviteContactsToChat = { _, _ -> },
            bottomSheetNavigator = rememberBottomSheetNavigator(),
            scaffoldState = rememberScaffoldState(),
            setSelectedMessages = {},
            setSelectMode = {},
            setSelectedReaction = {},
            setReactionList = {},
            setPendingAction = {},
            setAddingReactionTo = {},
            getApplicableActions = { listOf() },
            navigateToFreePlanLimitParticipants = {},
            showOptionsModal = {},
            showEmojiModal = {},
            showNoContactToAddDialog = {},
            showParticipatingInACallDialog = {},
            showAllContactsParticipateInChat = {},
            showGroupOrContactInfoActivity = {},
            showClearChatConfirmationDialog = {},
            showMutePushNotificationDialog = {},
            showEndCallForAllDialog = {},
            showToolbarModal = {},
            showJoinCallDialog = { _, _ -> },
            showUpgradeToProDialog = {},
            navigateToChat = {},
            navigateToContactInfo = {},
            navigateToMeeting = { _, _, _ -> },
            navigateToWaitingRoom = {},
            navigateToReactionInfo = {},
            navigateToNotSentModal = {},
            navigateToConversation = {},
            navHostController = rememberNavController(),
        )
    }
}
