package mega.privacy.android.app.main.dialog.link

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpenLinkViewModelTest {
    private lateinit var underTest: OpenLinkViewModel
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase = mock()
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase = mock()
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val chatManagement: ChatManagement = mock()

    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testCoroutineScope = TestScope(testCoroutineDispatcher)

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getUrlRegexPatternTypeUseCase,
            savedStateHandle,
            getHandleFromContactLinkUseCase,
            getChatLinkContentUseCase
        )
    }

    private fun initTestClass() {
        underTest = OpenLinkViewModel(
            getUrlRegexPatternTypeUseCase,
            savedStateHandle,
            getHandleFromContactLinkUseCase,
            getChatLinkContentUseCase,
            getScheduledMeetingByChatUseCase,
            getChatCallUseCase,
            startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase,
            setChatVideoInDeviceUseCase,
            rtcAudioManagerGateway,
            chatManagement,
            testCoroutineScope,
        )
    }

    @Test
    fun `test that uiState update correctly when call onLinkChanged`() = runTest {
        val link = "https://mega.app/C!86YkxIDC"
        underTest.onLinkChanged(link)
        verify(savedStateHandle)[OpenLinkViewModel.CURRENT_INPUT_LINK] = link
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.linkType).isNull()
            Truth.assertThat(state.checkLinkResult).isNull()
            Truth.assertThat(state.submittedLink).isNull()
        }
    }

    @Test
    fun `test that inputLink returns correctly when call inputLink`() {
        val link = "https://mega.app/C!86YkxIDC"
        whenever(savedStateHandle.get<String>(OpenLinkViewModel.CURRENT_INPUT_LINK)).thenReturn(link)
        Truth.assertThat(underTest.inputLink).isEqualTo(link)
    }

    @Test
    fun `test that uiState update correctly when call openLink with link`() = runTest {
        val link = "link"
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.submittedLink).isEqualTo(link)
        }
    }

    @Test
    fun `test that linkType update correctly when link is not chat link`() = runTest {
        val link = "https://mega.app/C!86YkxIDC"
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(false)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(false)
        whenever(getUrlRegexPatternTypeUseCase(link)).thenReturn(RegexPatternType.CONTACT_LINK)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.linkType).isEqualTo(RegexPatternType.CONTACT_LINK)
        }
    }

    @Test
    fun `test that openContactLinkHandle update correctly when link is not chat link`() = runTest {
        val link = "https://mega.app/C!86YkxIDC"
        whenever(getHandleFromContactLinkUseCase(link)).thenReturn(1L)
        underTest.openContactLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.openContactLinkHandle).isEqualTo(1L)
        }
    }

    @Test
    fun `test that checkLinkResult update correctly when open link to join meeting`() = runTest {
        val link = "https://mega.app/C!86YkxIDC"
        val chatLinkContent = mock<ChatLinkContent.ChatLink>()
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(true)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(false)
        whenever(getChatLinkContentUseCase(link)).thenReturn(chatLinkContent)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.checkLinkResult).isEqualTo(Result.success(chatLinkContent))
            Truth.assertThat(state.linkType).isNull()
        }
    }

    @Test
    fun `test that checkLinkResult update correctly when open link from chat`() = runTest {
        val link = "https://mega.app/C!86YkxIDC"
        val chatLinkContent = mock<ChatLinkContent.ChatLink>()
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(false)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(true)
        whenever(getChatLinkContentUseCase(link)).thenReturn(chatLinkContent)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.checkLinkResult).isEqualTo(Result.success(chatLinkContent))
            Truth.assertThat(state.linkType).isNull()
        }
    }
}