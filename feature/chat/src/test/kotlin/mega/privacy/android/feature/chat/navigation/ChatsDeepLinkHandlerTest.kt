package mega.privacy.android.feature.chat.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CHAT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.LegacyOpenLinkAfterFetchNodes
import mega.privacy.android.navigation.destination.LegacyWaitingRoomNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.WaitingRoomNavKeyInfo
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatsDeepLinkHandlerTest {
    private lateinit var underTest: ChatsDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val getChatLinkContentUseCase = mock<GetChatLinkContentUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getChatCallUseCase = mock<GetChatCallUseCase>()
    private val getScheduledMeetingByChatUseCase = mock<GetScheduledMeetingByChatUseCase>()
    private val startMeetingInWaitingRoomChatUseCase = mock<StartMeetingInWaitingRoomChatUseCase>()
    private val answerChatCallUseCase = mock<AnswerChatCallUseCase>()
    private val isEphemeralPlusPlusUseCase = mock<IsEphemeralPlusPlusUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()

    @BeforeAll
    fun setup() {
        underTest = ChatsDeepLinkHandler(
            getChatLinkContentUseCase = getChatLinkContentUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getChatCallUseCase = getChatCallUseCase,
            getScheduledMeetingByChatUseCase = getScheduledMeetingByChatUseCase,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            isEphemeralPlusPlusUseCase = isEphemeralPlusPlusUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            snackbarEventQueue,
            getChatLinkContentUseCase,
            getChatRoomUseCase,
            getChatCallUseCase,
            getScheduledMeetingByChatUseCase,
            startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase,
            isEphemeralPlusPlusUseCase,
            rootNodeExistsUseCase,
        )

        wheneverBlocking { (rootNodeExistsUseCase()) } doReturn true
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches NEW_MESSAGE_CHAT_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/fm/chat"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, NEW_MESSAGE_CHAT_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(ChatListNavKey())
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match NEW_MESSAGE_CHAT_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }

    @Nested
    inner class ChatLink {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that correct NavKey is returned when regex pattern type is CHAT_LINK and root node does not exist`(
            isLoggedIn: Boolean,
        ) = runTest {
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            whenever(rootNodeExistsUseCase()) doReturn false
            whenever(getChatLinkContentUseCase(uriString)) doThrow RuntimeException()

            val actual =
                underTest.getNavKeysInternal(uri, CHAT_LINK, isLoggedIn)

            if (isLoggedIn) {
                assertThat(actual).isEqualTo(listOf(LegacyOpenLinkAfterFetchNodes(uriString)))
                verifyNoInteractions(snackbarEventQueue)
            } else {
                assertThat(actual).isEmpty()
                verify(snackbarEventQueue).queueMessage(sharedR.string.general_invalid_link)
            }
        }

        @Test
        fun `test that error is shown when MeetingLink has invalid chatHandle`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val meetingLink = createMeetingLink(chatHandle = -1L)

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_invalid_link)
        }

        @Test
        fun `test that error is shown when MeetingLink has empty link`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val meetingLink = createMeetingLink(link = "")

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_invalid_link)
        }

        @Test
        fun `test that error is shown when getChatRoomUseCase returns null for MeetingLink`() =
            runTest {

                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val meetingLink = createMeetingLink(chatHandle = chatId)

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
                whenever(getChatRoomUseCase(chatId)).thenReturn(null)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).isEmpty()
                verify(snackbarEventQueue).queueMessage(sharedR.string.invalid_chat_link_error_message)
            }

        @Test
        fun `test that waiting room nav key is returned when joining waiting room meeting`() =
            runTest {

                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val meetingLink = createMeetingLink(
                    chatHandle = chatId,
                    isWaitingRoom = true,
                )
                val chatRoom = createChatRoom(chatId = chatId)
                val expectedNavKey = LegacyWaitingRoomNavKey(
                    chatId = chatId,
                    waitingRoomInfo = WaitingRoomNavKeyInfo.JoinWaitingRoom(uriString),
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
                whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
                whenever(getChatCallUseCase(chatId)).thenReturn(null)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that meeting nav key is returned when joining in progress room meeting`() =
            runTest {

                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val meetingLink = createMeetingLink(chatHandle = chatId)
                val chatRoom = createChatRoom(chatId = chatId)
                val expectedNavKey = LegacyMeetingNavKey(
                    chatId = chatId,
                    meetingInfo = MeetingNavKeyInfo.JoinInProgressCall(
                        meetingName = "Test Meeting",
                        link = uriString,
                    ),
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
                whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
                whenever(getChatCallUseCase(chatId)).thenReturn(null)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that rejoin meeting nav key is returned when exist is true`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val chatId = 123L
            val meetingLink = createMeetingLink(
                chatHandle = chatId,
                exist = true,
            )
            val chatRoom = createChatRoom(chatId = chatId)
            val expectedNavKey = LegacyMeetingNavKey(
                chatId = chatId,
                meetingInfo = MeetingNavKeyInfo.RejoinInProgressCall(
                    meetingName = "Test Meeting",
                    publicChatHandle = 456L,
                    link = uriString,
                ),
            )

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getChatCallUseCase(chatId)).thenReturn(null)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).containsExactly(expectedNavKey)
        }

        @Test
        fun `test that return to call nav key is returned when call is in progress`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val chatId = 123L
            val meetingLink = createMeetingLink(chatHandle = chatId)
            val chatRoom = createChatRoom(chatId = chatId)
            val call = createChatCall(chatId = chatId)
            val expectedNavKey = LegacyMeetingNavKey(
                chatId = chatId,
                meetingInfo = MeetingNavKeyInfo.ReturnToInProgressCall(isGuest = false),
            )

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getChatCallUseCase(chatId)).thenReturn(call)
            whenever(isEphemeralPlusPlusUseCase()).thenReturn(false)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).containsExactly(expectedNavKey)
        }

        @Test
        fun `test that waiting room host opens the call when call is in progress`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val chatId = 123L
            val callId = 789L
            val meetingLink = createMeetingLink(
                chatHandle = chatId,
                isWaitingRoom = true,
            )
            val chatRoom = createChatRoom(
                chatId = chatId,
                ownPrivilege = ChatRoomPermission.Moderator,
                isWaitingRoom = true,
            )
            val call = createChatCall(
                chatId = chatId,
                callId = callId,
            )
            val answeredCall = createChatCall(
                chatId = chatId,
                callId = callId,
                isOutgoing = false,
            )
            val expectedNavKey = LegacyMeetingNavKey(
                chatId = chatId,
                meetingInfo = MeetingNavKeyInfo.OpenCall(
                    callId = callId,
                    isGuest = false,
                    hasLocalVideo = false,
                    isOutgoing = false,
                    answer = false,
                ),
            )
            val schedIdWr = 1234L
            val scheduledMeeting = mock<ChatScheduledMeeting> {
                on { schedId } doReturn schedIdWr
            }

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getChatCallUseCase(chatId)).thenReturn(call)
            whenever(getScheduledMeetingByChatUseCase(chatId))
                .thenReturn(listOf(scheduledMeeting))
            whenever(
                startMeetingInWaitingRoomChatUseCase(
                    chatId = chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            ).thenReturn(answeredCall)
            whenever(isEphemeralPlusPlusUseCase()).thenReturn(false)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).containsExactly(expectedNavKey)
        }

        @Test
        fun `test that waiting room host starts the call when call is null`() = runTest {

            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val chatId = 123L
            val callId = 789L
            val meetingLink = createMeetingLink(
                chatHandle = chatId,
                isWaitingRoom = true,
            )
            val chatRoom = createChatRoom(
                chatId = chatId,
                ownPrivilege = ChatRoomPermission.Moderator,
                isWaitingRoom = true,
            )
            val startedCall = createChatCall(
                chatId = chatId,
                callId = callId,
                isOutgoing = false,
            )
            val expectedNavKey = LegacyMeetingNavKey(
                chatId = chatId,
                meetingInfo = MeetingNavKeyInfo.OpenCall(
                    callId = callId,
                    isGuest = false,
                    hasLocalVideo = false,
                    isOutgoing = false,
                    answer = false,
                ),
            )
            val schedIdWr = 1234L
            val scheduledMeeting = mock<ChatScheduledMeeting> {
                on { schedId } doReturn schedIdWr
            }

            whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
            whenever(getChatCallUseCase(chatId)).thenReturn(null)
            whenever(getScheduledMeetingByChatUseCase(chatId))
                .thenReturn(listOf(scheduledMeeting))
            whenever(
                startMeetingInWaitingRoomChatUseCase(
                    chatId = chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            ).thenReturn(startedCall)
            whenever(isEphemeralPlusPlusUseCase()).thenReturn(false)

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).containsExactly(expectedNavKey)
        }

        @Test
        fun `test that waiting room guest nav key is returned when joining as guest with waiting room`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val meetingLink = createMeetingLink(
                    chatHandle = chatId,
                    isWaitingRoom = true,
                )
                val expectedNavKey = LegacyWaitingRoomNavKey(
                    chatId = chatId,
                    waitingRoomInfo = WaitingRoomNavKeyInfo.JoinAsGuest(uriString),
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, false)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that meeting guest nav key is returned when joining as guest without waiting room`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val meetingLink = createMeetingLink(chatHandle = chatId)
                val expectedNavKey = LegacyMeetingNavKey(
                    chatId = chatId,
                    meetingInfo = MeetingNavKeyInfo.JoinAsGuest(
                        meetingName = "Test Meeting",
                        link = uriString,
                    ),
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(meetingLink)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, false)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that chat nav key is returned when ChatLink is returned and logged in`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val chatLink = createChatLink(chatHandle = chatId)
                val expectedNavKey = ChatNavKey(
                    chatId = chatId,
                    action = ACTION_OPEN_CHAT_LINK,
                    link = uriString,
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(chatLink)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that chat nav key is returned when ChatLink is returned and not logged in`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val chatLink = createChatLink(chatHandle = chatId)
                val expectedNavKey = ChatNavKey(
                    chatId = chatId,
                    action = ACTION_OPEN_CHAT_LINK,
                    link = uriString,
                )

                whenever(getChatLinkContentUseCase(uriString)).thenReturn(chatLink)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, false)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that error is shown when IAmOnAnotherCallException is thrown`() = runTest {
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            whenever(getChatLinkContentUseCase(uriString)).thenThrow(IAmOnAnotherCallException())

            val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.can_only_join_one_call_error_message)
        }

        @Test
        fun `test that meeting ended dialog nav key is returned when MeetingEndedException is thrown and chat does not exist`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val expectedNavKey = MeetingHasEndedDialogNavKey(null)

                whenever(getChatLinkContentUseCase(uriString)).thenThrow(
                    MeetingEndedException(
                        link = uriString,
                        chatId = chatId,
                    )
                )
                whenever(getChatRoomUseCase(chatId)) doReturn null

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).containsExactly(expectedNavKey)
            }

        @Test
        fun `test that meeting ended dialog nav key is returned when MeetingEndedException is thrown and chat exist`() =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val chatId = 123L
                val chat = mock<ChatRoom>()
                val expectedNavKey = MeetingHasEndedDialogNavKey(chatId)

                whenever(getChatLinkContentUseCase(uriString)).thenThrow(
                    MeetingEndedException(
                        link = uriString,
                        chatId = chatId,
                    )
                )
                whenever(getChatRoomUseCase(chatId)) doReturn chat

                val actual = underTest.getNavKeys(uri, CHAT_LINK, true)

                assertThat(actual).containsExactly(expectedNavKey)
            }


        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `test that error is shown when generic exception is thrown`(isLoggedIn: Boolean) =
            runTest {
                val uri = mock<Uri> {
                    on { this.toString() } doReturn uriString
                }
                val exception = RuntimeException("Generic error")

                whenever(getChatLinkContentUseCase(uriString)).thenThrow(exception)

                val actual = underTest.getNavKeys(uri, CHAT_LINK, isLoggedIn)

                assertThat(actual).isEmpty()
                verify(snackbarEventQueue).queueMessage(
                    if (isLoggedIn) {
                        sharedR.string.invalid_chat_link_error_message
                    } else {
                        sharedR.string.general_invalid_link
                    }
                )
            }

        private fun createMeetingLink(
            link: String = uriString,
            chatHandle: Long = 123L,
            text: String = "Test Meeting",
            userHandle: Long = 456L,
            exist: Boolean = false,
            isWaitingRoom: Boolean = false,
        ): ChatLinkContent.MeetingLink = mock {
            on { this.link } doReturn link
            on { this.chatHandle } doReturn chatHandle
            on { this.text } doReturn text
            on { this.userHandle } doReturn userHandle
            on { this.exist } doReturn exist
            on { this.isWaitingRoom } doReturn isWaitingRoom
        }

        private fun createChatRoom(
            chatId: Long = 123L,
            ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
            isWaitingRoom: Boolean = false,
        ): ChatRoom = mock {
            on { this.chatId } doReturn chatId
            on { this.ownPrivilege } doReturn ownPrivilege
            on { this.isWaitingRoom } doReturn isWaitingRoom
        }

        private fun createChatCall(
            chatId: Long = 123L,
            callId: Long = 789L,
            status: ChatCallStatus? = ChatCallStatus.InProgress,
            hasLocalVideo: Boolean = false,
            isOutgoing: Boolean = false,
        ): ChatCall = mock {
            on { this.chatId } doReturn chatId
            on { this.callId } doReturn callId
            on { this.status } doReturn status
            on { this.hasLocalVideo } doReturn hasLocalVideo
            on { this.isOutgoing } doReturn isOutgoing
        }

        private fun createChatLink(
            link: String = uriString,
            chatHandle: Long = 123L,
        ): ChatLinkContent.ChatLink = mock {
            on { this.link } doReturn link
            on { this.chatHandle } doReturn chatHandle
        }

        private val uriString = "https://mega.nz/meeting/test"
    }
}

