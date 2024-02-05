package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
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
import kotlin.time.Duration.Companion.milliseconds

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
        duration = 3000.milliseconds,
        userHandle = 1L,
        shouldShowAvatar = true,
        shouldShowTime = true,
        shouldShowDate = true,
        reactions = emptyList(),
    )

    private val chatFileNode: ChatDefaultFile = mock()
    private val getChatFileUseCase: GetChatFileUseCase = mock()
    private val voiceClipPlayer: VoiceClipPlayer = mock()
    private val cacheFile: File = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val downloadNodeResultFlow: MutableSharedFlow<MultiTransferEvent> = MutableSharedFlow()
    private val downloadNodesUseCase: DownloadNodesUseCase = mock()
    private val voiceClipPlayResultFlow: MutableSharedFlow<VoiceClipPlayState> = MutableSharedFlow()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()

    private val cacheFileParentPath = "parent path"
    private val mockTimestamp = "00:03"
    private val chatId = 1L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
            voiceClipPlayer,
            durationInSecondsTextMapper
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
        whenever(durationInSecondsTextMapper(any())).thenReturn(mockTimestamp)
    }

    @BeforeEach
    fun setupUnderTest() {
        underTest = VoiceClipMessageViewModel(
            downloadNodesUseCase = downloadNodesUseCase,
            getChatFileUseCase = getChatFileUseCase,
            getCacheFileUseCase = getCacheFileUseCase,
            voiceClipPlayer = voiceClipPlayer,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
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
            MultiTransferEvent.TransferNotStarted(
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
        downloadNodeResultFlow.emit(MultiTransferEvent.InsufficientSpace)
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().isError).isTrue()
        }
    }

    @Test
    fun `test that loading state finishes properly when download completes`() =
        runTest {
            setCacheFileNotExists()
            val endEvent = TransferEvent.TransferFinishEvent(mock(), null)
            whenever(
                downloadNodesUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(
                flowOf(
                    MultiTransferEvent.SingleTransferEvent(endEvent, 1L, 1L)
                )
            )
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage, chatId)
            testScheduler.advanceUntilIdle()
            underTest.getUiStateFlow(voiceClipMessage.msgId).test {
                val state = (this.cancelAndConsumeRemainingEvents().last() as Event.Item).value
                assertThat(state.loadProgress).isNull()
                assertThat(state.timestamp).isEqualTo(mockTimestamp)
            }
        }

    @Test
    fun `test that voice clip is paused if it is playing and play button is clicked`() = runTest {
        val msgId = 100L
        whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(true)
        initUiStateFlow()
        testScheduler.advanceUntilIdle()
        underTest.onPlayOrPauseClicked(msgId)
        underTest.getUiStateFlow(msgId).test {
            assertThat(awaitItem().isPlaying).isFalse()
        }
    }

    @Test
    fun `test that voice clip message is played properly when play button is clicked`() = runTest {
        val msgId = voiceClipMessage.msgId
        whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(false)
        whenever(getCacheFileUseCase(any(), any())).thenReturn(cacheFile)
        initUiStateFlow()
        setCacheFileExists()
        whenever(
            voiceClipPlayer.play(any(), any(), any())
        ).thenReturn(voiceClipPlayResultFlow)
        whenever(voiceClipPlayer.getCurrentPosition(msgId)).thenReturn(0)
        underTest.addVoiceClip(voiceClipMessage, chatId)
        testScheduler.advanceUntilIdle()

        underTest.onPlayOrPauseClicked(msgId)

        voiceClipPlayResultFlow.emit(VoiceClipPlayState.Prepared)
        voiceClipPlayResultFlow.emit(VoiceClipPlayState.Playing(1))
        underTest.getUiStateFlow(msgId).test {
            assertThat(awaitItem().isPlaying).isTrue()
        }

        voiceClipPlayResultFlow.emit(VoiceClipPlayState.Playing(2))
        underTest.getUiStateFlow(msgId).test {
            assertThat(awaitItem().isPlaying).isTrue()
        }

        voiceClipPlayResultFlow.emit(VoiceClipPlayState.Completed)
        underTest.getUiStateFlow(msgId).test {
            assertThat(awaitItem().isPlaying).isFalse()
        }
    }

    @Test
    fun `test that ui is updated to error state when voice clip cache file does not exist and play button is clicked`() =
        runTest {
            val msgId = voiceClipMessage.msgId
            whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(false)
            whenever(getCacheFileUseCase(any(), any())).thenReturn(null)
            initUiStateFlow()
            whenever(voiceClipPlayer.getCurrentPosition(msgId)).thenReturn(0)
            underTest.addVoiceClip(voiceClipMessage, chatId)
            testScheduler.advanceUntilIdle()

            // action
            underTest.onPlayOrPauseClicked(msgId)

            // verify
            underTest.getUiStateFlow(msgId).test {
                assertThat(awaitItem().isError).isTrue()
            }
        }

    @Test
    fun `test that ui is updated to error state when play throws exception and play button is clicked`() =
        runTest {
            val msgId = voiceClipMessage.msgId
            whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(false)
            whenever(getCacheFileUseCase(any(), any())).thenReturn(cacheFile)
            initUiStateFlow()
            setCacheFileExists()
            whenever(
                voiceClipPlayer.play(any(), any(), any())
            ).thenAnswer { throw RuntimeException("voice clip play exception") }
            whenever(voiceClipPlayer.getCurrentPosition(msgId)).thenReturn(0)
            underTest.addVoiceClip(voiceClipMessage, chatId)
            testScheduler.advanceUntilIdle()

            // action
            underTest.onPlayOrPauseClicked(msgId)

            // verify
            underTest.getUiStateFlow(msgId).test {
                assertThat(awaitItem().isError).isTrue()
            }
        }

    private fun setCacheFileNotExists() {
        cacheFile.stub {
            on { exists() }.thenReturn(false)
            on { parent }.thenReturn(cacheFileParentPath)
        }
    }

    private fun setCacheFileExists() {
        cacheFile.stub {
            on { exists() }.thenReturn(true)
            on { parent }.thenReturn(cacheFileParentPath)
            on { absolutePath }.thenReturn("absolute path")
        }
    }

    /**
     * Call the [VoiceClipMessageViewModel.getUiStateFlow] method to initialise the flow
     */
    private fun initUiStateFlow() {
        underTest.getUiStateFlow(voiceClipMessage.msgId)
    }
}