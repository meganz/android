package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.exception.chat.StartCallException
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartCallUseCaseTest {

    private lateinit var underTest: StartCallUseCase

    private val callRepository = mock<CallRepository>()
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = StartCallUseCase(callRepository, chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository, chatRepository)
    }

    @ParameterizedTest(name = " when chatId is {0}, existing call is {1} and repository call success is {2}")
    @ArgumentsSource(StartCallArgumentsProvider::class)
    fun `test that open or start call returns correctly`(
        chatId: Long,
        startCall: ChatCall?,
        success: Boolean,
    ) = runTest {
        whenever(chatRepository.getChatInvalidHandle()).thenReturn(-1L)

        if (chatId == -1L) {
            assertThrows<StartCallException> {
                underTest(chatId = chatId, audio = true, video = true)
                verify(chatRepository).getChatInvalidHandle()
                verifyNoMoreInteractions(chatRepository)
                verifyNoInteractions(callRepository)
            }
        } else {
            underTest(chatId = chatId, audio = true, video = true)
            verify(chatRepository).getChatInvalidHandle()
            verifyNoMoreInteractions(chatRepository)
            val chatRequest = mock<ChatRequest> {
                on { chatHandle } doReturn chatId
            }
            if (success) {
                whenever(
                    callRepository.startCallRinging(
                        chatId = chatId,
                        enabledVideo = true,
                        enabledAudio = true
                    )
                ).thenReturn(chatRequest)
                whenever(callRepository.getChatCall(chatId)).thenReturn(startCall)
                Truth.assertThat(underTest(chatId = chatId, audio = true, video = true))
                    .isEqualTo(startCall)
            } else {
                whenever(
                    callRepository.startCallRinging(
                        chatId = chatId,
                        enabledVideo = true,
                        enabledAudio = true
                    )
                ).thenThrow(RuntimeException())
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that start call ringing is called with correct audio parameters`(
        audio: Boolean,
    ) = runTest {
        val chatId = 123L
        underTest.invoke(chatId = chatId, audio = audio, video = false)
        verify(callRepository).startCallRinging(
            chatId = chatId,
            enabledAudio = audio,
            enabledVideo = false
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that start call ringing is called with correct video parameters`(
        video: Boolean,
    ) = runTest {
        val chatId = 123L
        underTest.invoke(chatId = chatId, audio = false, video = video)
        verify(callRepository).startCallRinging(
            chatId = chatId,
            enabledAudio = false,
            enabledVideo = video
        )
    }
}

internal class StartCallArgumentsProvider : ArgumentsProvider {

    private val chatId = 123L

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
        return Stream.of(
            Arguments.of(-1L, null, false),
            Arguments.of(chatId, null, false),
            Arguments.of(chatId, mock<ChatCall> { on { this.chatId } doReturn chatId }, true),
        )
    }
}