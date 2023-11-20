package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenOrStartCallUseCaseTest {

    private lateinit var underTest: OpenOrStartCallUseCase

    private val callRepository = mock<CallRepository>()
    private val startCallUseCase = mock<StartCallUseCase>()

    @BeforeAll
    fun setup() {
        underTest = OpenOrStartCallUseCase(callRepository, startCallUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository, startCallUseCase)
    }

    @ParameterizedTest(name = " when chatId is {0}, existing call is {1} and started call is {2}")
    @ArgumentsSource(OpenOrStartCallArgumentsProvider::class)
    fun `test that open or start call returns correctly`(
        chatId: Long,
        openCall: ChatCall?,
        startCall: ChatCall?,
    ) = runTest {
        if (chatId == -1L) {
            Truth.assertThat(underTest(chatId = chatId, video = true)).isNull()
        } else {
            whenever(callRepository.getChatCall(chatId)).thenReturn(openCall)
            whenever(startCallUseCase(chatId = chatId, video = true))
                .thenReturn(startCall)
            Truth.assertThat(underTest(chatId = chatId, video = true))
                .isEqualTo(openCall ?: startCall)
        }
    }

}

internal class OpenOrStartCallArgumentsProvider : ArgumentsProvider {

    private val chatId = 123L

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
        return Stream.of(
            Arguments.of(-1L, null, null),
            Arguments.of(chatId, null, null),
            Arguments.of(chatId, mock<ChatCall>(), null),
            Arguments.of(chatId, null, mock<ChatCall> { on { this.chatId } doReturn chatId }),
        )
    }
}