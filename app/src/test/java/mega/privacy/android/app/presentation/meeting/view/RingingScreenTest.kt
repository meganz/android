package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.material.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.RingingUIState
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.AUDIO_BUTTON
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.GROUP_AVATAR
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.HANG_UP_BUTTON
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.ONE_TO_ONE_AVATAR
import mega.privacy.android.app.presentation.meeting.view.RingingViewTestTags.VIDEO_BUTTON
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.shared.original.core.ui.controls.appbar.APP_BAR_BACK_BUTTON_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class RingingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the avatar is not shown`() {
        with(composeRule) {
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = true),
                    avatar = null
                )
            )
            onNodeWithTag(ONE_TO_ONE_AVATAR).assertIsNotDisplayed()
            onNodeWithTag(GROUP_AVATAR).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that the group avatar is shown`() {
        with(composeRule) {
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = true),
                    call = createCall(),
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                )
            )

            onNodeWithTag(GROUP_AVATAR).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the one to one avatar is shown`() {
        with(composeRule) {
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = false),
                    call = createCall(),
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                )
            )

            onNodeWithTag(ONE_TO_ONE_AVATAR).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the hang up button is clickable`() {
        with(composeRule) {
            val onHangUpClicked = mock<() -> Unit>()
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = false),
                    isCallAnsweredAndWaitingForCallInfo = false,
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                ),
                onHangUpClicked = onHangUpClicked
            )

            onNodeWithTag(HANG_UP_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }

            verify(onHangUpClicked).invoke()
        }
    }

    @Test
    fun `test that the answer with audio button is clickable`() {
        with(composeRule) {
            val onAnswerWithAudioClicked = mock<() -> Unit>()
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = false),
                    isCallAnsweredAndWaitingForCallInfo = false,
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                ),
                onAudioClicked = onAnswerWithAudioClicked
            )

            onNodeWithTag(AUDIO_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }

            verify(onAnswerWithAudioClicked).invoke()
        }
    }

    @Test
    fun `test that the answer with video button is clickable`() {
        with(composeRule) {
            val onAnswerWithVideoClicked = mock<() -> Unit>()
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = false),
                    isCallAnsweredAndWaitingForCallInfo = false,
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                ),
                onVideoClicked = onAnswerWithVideoClicked
            )

            onNodeWithTag(VIDEO_BUTTON).apply {
                assertIsDisplayed()
                performClick()
            }

            verify(onAnswerWithVideoClicked).invoke()
        }
    }

    @Test
    fun `test that the back button perform an action`() {
        with(composeRule) {
            val onBackPressed = mock<() -> Unit>()
            setScreen(
                uiState = RingingUIState(
                    chat = createChat(isGroup = false),
                    avatar = ChatAvatarItem(
                        placeholderText = "R",
                        color = 0xFFC70000.toInt(),
                        uri = null
                    )
                ),
                onBackPressed = onBackPressed
            )

            onNodeWithTag(APP_BAR_BACK_BUTTON_TAG).apply {
                assertIsDisplayed()
                performClick()
            }

            verify(onBackPressed).invoke()
        }
    }

    private fun createChat(isGroup: Boolean): ChatRoom {
        return ChatRoom(
            chatId = 123L,
            ownPrivilege = ChatRoomPermission.Moderator,
            numPreviewers = 0L,
            peerPrivilegesByHandles = emptyMap(),
            peerCount = 0L,
            peerHandlesList = emptyList(),
            peerPrivilegesList = emptyList(),
            isGroup = isGroup,
            isPublic = false,
            isPreview = false,
            authorizationToken = null,
            title = "chat title",
            hasCustomTitle = false,
            unreadCount = 0,
            userTyping = -1L,
            userHandle = -1L,
            isActive = true,
            isArchived = false,
            retentionTime = -1L,
            creationTime = -1L,
            isMeeting = false,
            isWaitingRoom = false,
            isOpenInvite = false,
            isSpeakRequest = false,
            changes = null
        )
    }

    private fun createCall(): ChatCall {
        return ChatCall(
            chatId = 123L,
            callId = 456L,
        )
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: RingingUIState = RingingUIState(),
        snackbarHostState: SnackbarHostState = SnackbarHostState(),
        onBackPressed: () -> Unit = {},
        onAudioClicked: () -> Unit = {},
        onVideoClicked: () -> Unit = {},
        onHangUpClicked: () -> Unit = {},
    ) {
        setContent {
            RingingView(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onBackPressed = onBackPressed,
                onAudioClicked = onAudioClicked,
                onVideoClicked = onVideoClicked,
                onHangUpClicked = onHangUpClicked,
            )
        }
    }
}