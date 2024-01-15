package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import org.junit.Rule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoiceClipMessageViewModelTest {

    lateinit var underTest: VoiceClipMessageViewModel

    private val voiceClipMessage = VoiceClipMessage(
        msgId = 1,
        name = "name",
        isMine = true,
        time = 1,
        status = ChatMessageStatus.SERVER_RECEIVED,
        size = 1,
        duration = 3,
        userHandle = 1L,
        shouldShowAvatar = true,
        shouldShowTime = true,
        shouldShowDate = true,
    )

    private val chatFileNode: ChatDefaultFile = mock()
    private val getChatFileUseCase: GetChatFileUseCase = mock()
    private val cacheFile: File = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val downloadNodeResultFlow: MutableSharedFlow<DownloadNodesEvent> = MutableSharedFlow()
    private val downloadNodesUseCase: DownloadNodesUseCase = mock()

    private val cacheFileParentPath = "parent path"
    private val mockTimestamp = "00:03"
    private val chatId = 1L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            downloadNodesUseCase,
            getChatFileUseCase,
            getCacheFileUseCase,
        )

        whenever(getCacheFileUseCase(any(), any())).thenReturn(cacheFile)
        whenever(
            downloadNodesUseCase(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(emptyFlow())
        getChatFileUseCase.stub {
            onBlocking { invoke(any(), any(), any()) }.thenReturn(chatFileNode)
        }
        cacheFile.stub {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(voiceClipMessage.size)
        }
    }

    @BeforeEach
    fun setupUnderTest() {
        underTest = VoiceClipMessageViewModel(
            downloadNodesUseCase,
            getChatFileUseCase,
            getCacheFileUseCase,
        )
    }

    @Test
    fun `test that getUiStateFlow initialise the flow when it is called for the first time`() {
        val msgId = 100L
        underTest.getUiStateFlow(msgId)
        assertThat(underTest.getUiStateFlow(msgId).value.voiceClipMessage).isNull()
    }

    @Test
    fun `test that ui is updated with error when cache file is not obtained`() = runTest {
        whenever(getCacheFileUseCase(any(), any())).thenReturn(null)
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @ParameterizedTest(name = "${0}")
    @MethodSource("provideErrorChatMessageStatus")
    fun `test that ui is updated with error when chat message status is `(
        status: ChatMessageStatus,
    ) = runTest {
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage.copy(status = status), chatId)
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    private fun provideErrorChatMessageStatus() = Stream.of(
        Arguments.of(ChatMessageStatus.SERVER_REJECTED),
        Arguments.of(ChatMessageStatus.SENDING_MANUAL),
        Arguments.of(ChatMessageStatus.SENDING),
    )

    @Test
    fun `test that loading state finishes properly when voice clip has already been downloaded to cache`() =
        runTest {
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage, chatId)
            testScheduler.advanceUntilIdle()
            underTest.getUiStateFlow(voiceClipMessage.msgId).test {
                val state = awaitItem()
                assertThat(state.loadProgress).isNull()
                assertThat(state.timestamp).isEqualTo(mockTimestamp)
            }
        }

    @Test
    fun `test that ui is updated with error if parent path of cache file is null`() = runTest {
        cacheFile.stub {
            on { exists() }.thenReturn(false)
            on { parent }.thenReturn(null)
        }
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @Test
    fun `test that ui is updated with error if it failed to get ChatFile node`() = runTest {
        setCacheFileNotExists()
        getChatFileUseCase.stub {
            onBlocking { invoke(any(), any(), any()) }.thenReturn(null)
        }
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @Test
    fun `test that ui is updated with error when download returns TransferNotStarted`() = runTest {
        setCacheFileNotExists()
        whenever(
            downloadNodesUseCase(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(downloadNodeResultFlow)
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()
        downloadNodeResultFlow.emit(
            DownloadNodesEvent.TransferNotStarted(
                NodeId(1L), Exception()
            )
        )
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @Test
    fun `test that ui is updated with error when download returns NotSufficientSpace`() = runTest {
        setCacheFileNotExists()
        whenever(
            downloadNodesUseCase(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(downloadNodeResultFlow)
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()
        downloadNodeResultFlow.emit(DownloadNodesEvent.NotSufficientSpace)
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @Test
    fun `test that loading state finishes properly when download returns TransferFinishedProcessing`() =
        runTest {
            setCacheFileNotExists()
            whenever(
                downloadNodesUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(downloadNodeResultFlow)
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage, chatId)
            testScheduler.advanceUntilIdle()
            downloadNodeResultFlow.emit(DownloadNodesEvent.TransferFinishedProcessing(NodeId(1L)))
            underTest.getUiStateFlow(voiceClipMessage.msgId).test {
                val state = awaitItem()
                assertThat(state.loadProgress).isNull()
                assertThat(state.timestamp).isEqualTo(mockTimestamp)
            }
        }

    private fun setCacheFileNotExists() {
        cacheFile.stub {
            on { exists() }.thenReturn(false)
            on { parent }.thenReturn(cacheFileParentPath)
        }
    }


    /**
     * Call the [VoiceClipMessageViewModel.getUiStateFlow] method to initialise the flow
     */
    private fun initUiStateFlow() {
        underTest.getUiStateFlow(voiceClipMessage.msgId)
    }
}