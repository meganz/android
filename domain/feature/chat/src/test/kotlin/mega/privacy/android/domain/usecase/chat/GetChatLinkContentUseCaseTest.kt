package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.chat.ChatPreview
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetChatLinkContentUseCaseTest {
    private lateinit var underTest: GetChatLinkContentUseCase
    private val checkChatLinkUseCase: CheckChatLinkUseCase = mock()
    private val isMeetingEndedUseCase: IsMeetingEndedUseCase = mock()
    private val getAnotherCallParticipatingUseCase: GetAnotherCallParticipatingUseCase = mock()
    private val repository: ChatRepository = mock()
    private val checkIfIAmInThisMeetingUseCase: CheckIfIAmInThisMeetingUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = GetChatLinkContentUseCase(
            checkChatLinkUseCase,
            isMeetingEndedUseCase,
            getAnotherCallParticipatingUseCase,
            repository,
            checkIfIAmInThisMeetingUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkChatLinkUseCase,
            isMeetingEndedUseCase,
            getAnotherCallParticipatingUseCase,
            repository,
            checkIfIAmInThisMeetingUseCase,
        )
    }

    @Test
    fun `test that the chat link returns correctly when CheckChatLinkUseCase returns a chat link`() =
        runTest {
            val link = "link"
            val chatId = 1L
            val request = mock<ChatRequest> {
                on { this.paramType } doReturn null
                on { this.link } doReturn link
                on { this.chatHandle } doReturn chatId
            }
            whenever(checkChatLinkUseCase(link)) doReturn request
            Truth.assertThat(underTest(link)).isEqualTo(ChatLinkContent.ChatLink(link, chatId))
        }

    @Test
    fun `test that the meeting link returns correctly when CheckChatLinkUseCase returns a meeting link`() =
        runTest {
            val link = "link"
            val request = mock<ChatRequest> {
                on { this.paramType } doReturn ChatRequestParamType.MEETING_LINK
                on { this.link } doReturn link
                on { this.chatHandle } doReturn 1L
                on { this.handleList } doReturn listOf(1L)
                on { this.privilege } doReturn 1
                on { this.text } doReturn "text"
                on { this.userHandle } doReturn 1L
            }
            val chatPreview = mock<ChatPreview> {
                on { this.request } doReturn request
                on { this.exist } doReturn true
            }
            val expected = ChatLinkContent.MeetingLink(
                link = chatPreview.request.link.orEmpty(),
                chatHandle = chatPreview.request.chatHandle,
                isInThisMeeting = false,
                handles = chatPreview.request.handleList,
                text = chatPreview.request.text.orEmpty(),
                userHandle = chatPreview.request.userHandle,
                exist = chatPreview.exist,
                isWaitingRoom = false,
            )
            whenever(repository.hasWaitingRoomChatOptions(any())) doReturn false
            whenever(repository.getChatInvalidHandle()) doReturn -1L
            whenever(checkChatLinkUseCase(link)) doReturn request
            whenever(isMeetingEndedUseCase(any(), any())) doReturn false
            whenever(checkIfIAmInThisMeetingUseCase(any())) doReturn false
            whenever(getAnotherCallParticipatingUseCase(any())) doReturn -1L
            whenever(repository.openChatPreview(link)) doReturn chatPreview
            Truth.assertThat(underTest(link)).isEqualTo(expected)
        }

    @Test
    fun `test that throws an MeetingEndedException when is meeting ended`() = runTest {
        val link = "link"
        val request = mock<ChatRequest> {
            on { this.paramType } doReturn ChatRequestParamType.MEETING_LINK
            on { this.link } doReturn link
            on { this.chatHandle } doReturn 1L
            on { this.handleList } doReturn listOf(1L)
            on { this.privilege } doReturn 1
            on { this.text } doReturn "text"
            on { this.userHandle } doReturn 1L
        }
        whenever(repository.hasWaitingRoomChatOptions(any())) doReturn false
        whenever(repository.getChatInvalidHandle()) doReturn -1L
        whenever(checkChatLinkUseCase(link)) doReturn request
        whenever(isMeetingEndedUseCase(any(), any())) doReturn true
        assertThrows<MeetingEndedException> { underTest(link) }
    }

    @Test
    fun `test that throws an IAmOnAnotherCallException when I am participating in another call`() =
        runTest {
            val link = "link"
            val request = mock<ChatRequest> {
                on { this.paramType } doReturn ChatRequestParamType.MEETING_LINK
                on { this.link } doReturn link
                on { this.chatHandle } doReturn 1L
                on { this.handleList } doReturn listOf(1L)
                on { this.privilege } doReturn 1
                on { this.text } doReturn "text"
                on { this.userHandle } doReturn 1L
            }
            whenever(repository.hasWaitingRoomChatOptions(any())) doReturn false
            whenever(repository.getChatInvalidHandle()) doReturn -1L
            whenever(checkChatLinkUseCase(link)) doReturn request
            whenever(isMeetingEndedUseCase(any(), any())) doReturn false
            whenever(checkIfIAmInThisMeetingUseCase(any())) doReturn false
            whenever(getAnotherCallParticipatingUseCase(any())) doReturn 1L
            assertThrows<IAmOnAnotherCallException> { underTest(link) }
        }
}