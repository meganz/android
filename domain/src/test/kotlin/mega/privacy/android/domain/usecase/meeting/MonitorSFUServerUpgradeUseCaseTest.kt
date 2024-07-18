package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.CallNotificationType
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSFUServerUpgradeUseCaseTest {
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private lateinit var underTest: MonitorSFUServerUpgradeUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorSFUServerUpgradeUseCase(monitorChatCallUpdatesUseCase)
    }

    @ParameterizedTest(name = "when chat call is {0} and emits {1}")
    @MethodSource("provideParams")
    fun `test that it emits correct value when invoked `(
        chatCall: ChatCall,
        expected: Boolean,
    ) = runTest {
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(chatCall))
        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun provideParams(): Stream<Arguments> = Stream.of(
        Arguments.of(mock<ChatCall> {
            on { chatId }.thenReturn(1L)
            on { changes }.thenReturn(listOf(ChatCallChanges.Status))
            on { status }.thenReturn(ChatCallStatus.TerminatingUserParticipation)
            on { termCode }.thenReturn(ChatCallTermCodeType.ProtocolVersion)
        }, true),
        Arguments.of(mock<ChatCall> {
            on { chatId }.thenReturn(1L)
            on { changes }.thenReturn(listOf(ChatCallChanges.Status))
            on { status }.thenReturn(ChatCallStatus.Connecting)
            on { termCode }.thenReturn(ChatCallTermCodeType.TooManyClients)
        }, false),
        Arguments.of(mock<ChatCall> {
            on { chatId }.thenReturn(1L)
            on { changes }.thenReturn(listOf(ChatCallChanges.GenericNotification))
            on { notificationType }.thenReturn(CallNotificationType.SFUError)
            on { termCode }.thenReturn(ChatCallTermCodeType.ProtocolVersion)
        }, true),
        Arguments.of(mock<ChatCall> {
            on { chatId }.thenReturn(1L)
            on { changes }.thenReturn(listOf(ChatCallChanges.Status))
            on { notificationType }.thenReturn(CallNotificationType.SFUDeny)
            on { termCode }.thenReturn(ChatCallTermCodeType.TooManyClients)
        }, false),
    )

}
