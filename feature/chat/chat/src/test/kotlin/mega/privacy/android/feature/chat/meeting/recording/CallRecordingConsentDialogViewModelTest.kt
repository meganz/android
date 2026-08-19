package mega.privacy.android.feature.chat.meeting.recording

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingConsentUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallRecordingConsentDialogViewModelTest {
    private lateinit var underTest: CallRecordingConsentDialogViewModel

    private val monitorCallRecordingConsentEventUseCase =
        mock<MonitorCallRecordingConsentEventUseCase>()

    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()

    private val hangChatCallByChatIdUseCase = mock<HangChatCallByChatIdUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = CallRecordingConsentDialogViewModel(
            monitorCallRecordingConsentEventUseCase = monitorCallRecordingConsentEventUseCase,
            broadcastCallRecordingConsentEventUseCase = broadcastCallRecordingConsentEventUseCase,
            hangChatCallByChatIdUseCase = hangChatCallByChatIdUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorCallRecordingConsentEventUseCase,
            broadcastCallRecordingConsentEventUseCase,
            hangChatCallByChatIdUseCase,
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorCallRecordingConsentEventUseCase.stub {
            on { invoke() } doReturn flow {
                awaitCancellation()
            }
        }
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(CallRecordingConsentUiState.Loading)
        }
    }

    @Test
    fun `test that pending approval returns Requires approval state with matching chat id`() =
        runTest {
            val expectedChatId = 1234L
            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Pending(expectedChatId))
                    awaitCancellation()
                }
            }

            underTest.state.test {
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingConsentUiState.ConsentRequired(expectedChatId)
                )
            }
        }

    @ParameterizedTest
    @MethodSource("providePendingApprovalStates")
    fun `test that non pending approval returns ConsentAlreadyHandled state`(status: CallRecordingConsentStatus) =
        runTest {
            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(status)
                    awaitCancellation()
                }
            }

            underTest.state.test {
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingConsentUiState.ConsentAlreadyHandled
                )
            }
        }

    private fun providePendingApprovalStates() = listOf(
        CallRecordingConsentStatus.None,
        CallRecordingConsentStatus.Requested(1L),
        CallRecordingConsentStatus.Granted(1L),
        CallRecordingConsentStatus.Denied(1L),
    )

    @Test
    fun `test that Granted status is emitted when accepted method is called`() = runTest {
        val chatId = 12345L
        underTest.accept(chatId)

        verify(broadcastCallRecordingConsentEventUseCase).invoke(
            CallRecordingConsentStatus.Granted(
                chatId
            )
        )
    }

    @Test
    fun `test that Denied status is emitted when decline method is called`() = runTest {
        val chatId = 12345L
        underTest.decline(chatId)

        verify(broadcastCallRecordingConsentEventUseCase).invoke(
            CallRecordingConsentStatus.Denied(
                chatId
            )
        )
    }

    @Test
    fun `test that call is hung up when decline method is called`() = runTest {
        val chatId = 12345L
        underTest.decline(chatId)

        verify(hangChatCallByChatIdUseCase).invoke(chatId)
    }

    @Test
    fun `test that when onDisplayed is called then Requested is emitted`() = runTest {
        val chatId = 123L
        underTest.onDisplayed(chatId)

        verify(broadcastCallRecordingConsentEventUseCase).invoke(
            CallRecordingConsentStatus.Requested(
                chatId
            )
        )
    }

    @Test
    fun `test that Requested events following Pending events with the same chat Id does not emit a new value`() =
        runTest {
            val expectedChatId = 12345L
            val consentFlow = MutableStateFlow<CallRecordingConsentStatus>(
                CallRecordingConsentStatus.Pending(expectedChatId)
            )
            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn consentFlow
            }

            underTest.state.test {
                assertThat(awaitItem()).isEqualTo(
                    CallRecordingConsentUiState.ConsentRequired(expectedChatId)
                )
                consentFlow.emit(CallRecordingConsentStatus.Requested(expectedChatId))
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }
}