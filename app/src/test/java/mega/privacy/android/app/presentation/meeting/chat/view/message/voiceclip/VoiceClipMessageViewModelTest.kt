package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.interfaces.OnProximitySensorListener
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ProximitySensorState
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdateDoesNotExistInMessageUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoiceClipMessageViewModelTest {

    lateinit var underTest: VoiceClipMessageViewModel

    private val voiceClipMessage = VoiceClipMessage(
        chatId = 1,
        msgId = 1,
        name = "name",
        isMine = true,
        time = 1,
        isDeletable = false,
        isEditable = false,
        status = ChatMessageStatus.SERVER_RECEIVED,
        content = null,
        fileNode = mock<ChatDefaultFile>(),
        size = 1,
        duration = 3000.milliseconds,
        userHandle = 1L,
        reactions = emptyList(),
        exists = true,
        rowId = 765L,
    )

    private val voiceClipPlayer: VoiceClipPlayer = mock()
    private val cacheFile: File = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val downloadNodeUseCase: DownloadNodeUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val voiceClipPlayResultFlow: MutableSharedFlow<VoiceClipPlayState> = MutableSharedFlow()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val updateDoesNotExistInMessageUseCase =
        mock<UpdateDoesNotExistInMessageUseCase>()

    private val cacheFileParentPath = "parent path"
    private val mockTimestamp = "00:03"

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @BeforeEach
    fun resetMocks() {
        reset(
            downloadNodeUseCase,
            getCacheFileUseCase,
            voiceClipPlayer,
            durationInSecondsTextMapper,
            updateDoesNotExistInMessageUseCase,
            rtcAudioManagerGateway
        )
        whenever(getCacheFileUseCase(any(), any())).thenReturn(cacheFile)
        whenever(
            downloadNodeUseCase(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(emptyFlow())
        cacheFile.stub {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(voiceClipMessage.size)
        }
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(mockTimestamp)
        whenever(
            rtcAudioManagerGateway.createOrUpdateAudioManager(
                true,
                AUDIO_MANAGER_PLAY_VOICE_CLIP
            )
        ).thenAnswer { }
    }

    @BeforeEach
    fun setupUnderTest() {
        underTest = VoiceClipMessageViewModel(
            downloadNodeUseCase = downloadNodeUseCase,
            getCacheFileUseCase = getCacheFileUseCase,
            voiceClipPlayer = voiceClipPlayer,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            updateDoesNotExistInMessageUseCase = updateDoesNotExistInMessageUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway
        )
    }

    @Test
    fun `test that getUiStateFlow initialise the flow when it is called for the first time`() {
        val msgId = 100L
        underTest.getUiStateFlow(msgId)
        assertThat(underTest.getUiStateFlow(msgId).value.voiceClipMessage).isNull()
    }

    @Test
    fun `test that ui is updated with not available when cache file is not obtained`() = runTest {
        whenever(getCacheFileUseCase(any(), any())).thenReturn(null)
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage)
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            val actual = awaitItem()
            assertThat(actual.timestamp).isNull()
            assertThat(actual.loadProgress).isNull()
        }
    }

    @ParameterizedTest(name = "${0}")
    @MethodSource("provideErrorChatMessageStatus")
    fun `test that ui is updated with with not available when chat message status is `(
        status: ChatMessageStatus,
    ) = runTest {
        initUiStateFlow()
        underTest.addVoiceClip(voiceClipMessage.copy(status = status))
        testScheduler.advanceUntilIdle()
        underTest.getUiStateFlow(voiceClipMessage.msgId).test {
            assertThat(awaitItem().timestamp).isNull()
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
            underTest.addVoiceClip(voiceClipMessage)
            testScheduler.advanceUntilIdle()
            underTest.getUiStateFlow(voiceClipMessage.msgId).test {
                val state = awaitItem()
                assertThat(state.loadProgress).isNull()
                assertThat(state.timestamp).isEqualTo(mockTimestamp)
            }
        }

    @Test
    fun `test that ui is updated with not available when download throws an exception`() =
        runTest {
            setCacheFileNotExists()
            val downloadNodeResultFlow: MutableSharedFlow<TransferEvent?> = MutableSharedFlow()
            whenever(
                downloadNodeUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(downloadNodeResultFlow.transform {
                if (it == null) {
                    throw RuntimeException("test error")
                } else {
                    emit(it)
                }
            })
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage)
            testScheduler.advanceUntilIdle()
            downloadNodeResultFlow.emit(null) //this will throw an exception on the transformed flow
            underTest.getUiStateFlow(voiceClipMessage.msgId).test {
                val actual = awaitItem()
                assertThat(actual.timestamp).isNull()
                assertThat(actual.loadProgress).isNull()
            }
        }

    @Test
    fun `test that loading state finishes properly when download completes`() =
        runTest {
            setCacheFileNotExists()
            val endEvent = TransferEvent.TransferFinishEvent(mock(), null)
            whenever(
                downloadNodeUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(
                flowOf(
                    endEvent
                )
            )
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage)
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
        underTest.addVoiceClip(voiceClipMessage)
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
    fun `test that ui is updated to not available state when voice clip cache file does not exist and play button is clicked`() =
        runTest {
            val msgId = voiceClipMessage.msgId
            whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(false)
            whenever(getCacheFileUseCase(any(), any())).thenReturn(null)
            initUiStateFlow()
            whenever(voiceClipPlayer.getCurrentPosition(msgId)).thenReturn(0)
            underTest.addVoiceClip(voiceClipMessage)
            testScheduler.advanceUntilIdle()

            // action
            underTest.onPlayOrPauseClicked(msgId)

            // verify
            underTest.getUiStateFlow(msgId).test {
                val actual = awaitItem()
                assertThat(actual.timestamp).isNull()
                assertThat(actual.loadProgress).isNull()
            }
        }

    @Test
    fun `test that ui is updated to not available state when play throws exception and play button is clicked`() =
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
            underTest.addVoiceClip(voiceClipMessage)
            testScheduler.advanceUntilIdle()

            // action
            underTest.onPlayOrPauseClicked(msgId)

            // verify
            underTest.getUiStateFlow(msgId).test {
                val actual = awaitItem()
                assertThat(actual.timestamp).isNull()
                assertThat(actual.loadProgress).isNull()
            }
        }

    @Test
    fun `test that ui is updated to progress when user seek to a position`() =
        runTest {
            val msgId = voiceClipMessage.msgId
            val progress = 0.5f
            val fakeTimestamp = "00:03"
            whenever(durationInSecondsTextMapper(any())).thenReturn(fakeTimestamp)
            initUiStateFlow()
            underTest.addVoiceClip(voiceClipMessage)
            testScheduler.advanceUntilIdle()

            underTest.onSeek(progress, msgId)

            verify(voiceClipPlayer).seekTo(
                key = msgId,
                position = (voiceClipMessage.duration.inWholeMilliseconds * progress).toInt()
            )
            underTest.getUiStateFlow(msgId).test {
                val actual = awaitItem()

                assertThat(actual.playProgress).isEqualTo(Progress(progress))
                assertThat(actual.timestamp).isEqualTo(fakeTimestamp)
            }
        }

    @Test
    fun `test that player seekTo() is not called when duration is missing in the message`() =
        runTest {
            val msgId = voiceClipMessage.msgId
            val progress = 0.5f
            val fakeTimestamp = "00:03"
            whenever(durationInSecondsTextMapper(any())).thenReturn(fakeTimestamp)
            initUiStateFlow()

            testScheduler.advanceUntilIdle()

            underTest.onSeek(progress, msgId)

            verifyNoInteractions(voiceClipPlayer)
        }

    @Test
    fun `test that start proximity sensor`() = runTest {
        val msgId = 100L
        initUiStateFlow()
        testScheduler.advanceUntilIdle()
        underTest.startProximitySensor(msgId)
        verify(rtcAudioManagerGateway).createOrUpdateAudioManager(
            true,
            AUDIO_MANAGER_PLAY_VOICE_CLIP
        )
    }

    @Test
    fun `test that start proximity sensor when is near`() {
        val msgId = 100L
        whenever(rtcAudioManagerGateway.startProximitySensor(any())).thenAnswer {
            (it.arguments[0] as OnProximitySensorListener).needToUpdate(true)
            runTest {
                initUiStateFlow()
                testScheduler.advanceUntilIdle()
                underTest.startProximitySensor(msgId)

                underTest.getUiStateFlow(msgId).test {
                    assertThat(awaitItem().proximitySensorState).isEqualTo(ProximitySensorState.Near)
                }
            }
        }
    }

    @Test
    fun `test that start proximity sensor when is far`() {
        val msgId = 100L
        whenever(rtcAudioManagerGateway.startProximitySensor(any())).thenAnswer {
            (it.arguments[0] as OnProximitySensorListener).needToUpdate(false)
            runTest {
                whenever(voiceClipPlayer.isPlaying(msgId)).thenReturn(true)
                whenever(underTest.getUiStateFlow(msgId).value.proximitySensorState).thenReturn(
                    ProximitySensorState.Near
                )

                initUiStateFlow()
                testScheduler.advanceUntilIdle()
                underTest.startProximitySensor(msgId)

                underTest.getUiStateFlow(msgId).test {
                    assertThat(awaitItem().proximitySensorState).isEqualTo(ProximitySensorState.Far)
                    assertThat(awaitItem().isPaused).isTrue()
                }
            }
        }
    }

    @Test
    fun `test that stop proximity sensor updates the sensor state`() = runTest {
        val msgId = 100L
        initUiStateFlow()
        testScheduler.advanceUntilIdle()
        underTest.stopProximitySensor(msgId)
        underTest.getUiStateFlow(msgId).test {
            assertThat(awaitItem().proximitySensorState).isEqualTo(ProximitySensorState.Unknown)
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