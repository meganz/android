package mega.privacy.android.feature.chat.meeting.left

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
class LeftMeetingViewModelTest {

    @Test
    fun `test that uiState is None when the call ended without a limit being hit`() = runTest {
        val underTest = createViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.None)
        }
    }

    @Test
    fun `test that uiState is FreePlanParticipantsLimit when the call ended due to free plan limits`() =
        runTest {
            val underTest = createViewModel(callEndedDueToFreePlanLimits = true)

            underTest.uiState.test {
                assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.FreePlanParticipantsLimit)
            }
        }

    @Test
    fun `test that uiState is TooManyParticipants when the call ended due to too many participants`() =
        runTest {
            val underTest = createViewModel(callEndedDueToTooManyParticipants = true)

            underTest.uiState.test {
                assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.TooManyParticipants)
            }
        }

    @Test
    fun `test that uiState is FreePlanParticipantsLimit when the call ended due to both limits`() =
        runTest {
            val underTest = createViewModel(
                callEndedDueToFreePlanLimits = true,
                callEndedDueToTooManyParticipants = true,
            )

            underTest.uiState.test {
                assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.FreePlanParticipantsLimit)
            }
        }

    @Test
    fun `test that onNoticeShown clears the free plan participants limit notice`() = runTest {
        val underTest = createViewModel(callEndedDueToFreePlanLimits = true)

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.FreePlanParticipantsLimit)

            underTest.onNoticeShown()

            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.None)
        }
    }

    @Test
    fun `test that onNoticeShown clears the too many participants notice`() = runTest {
        val underTest = createViewModel(callEndedDueToTooManyParticipants = true)

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.TooManyParticipants)

            underTest.onNoticeShown()

            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.None)
        }
    }

    @Test
    fun `test that the cleared notice is retained for a later collector`() = runTest {
        val underTest = createViewModel(callEndedDueToFreePlanLimits = true)

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.FreePlanParticipantsLimit)

            underTest.onNoticeShown()

            assertThat(awaitItem()).isEqualTo(LeftMeetingUiState.None)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(underTest.uiState.value).isEqualTo(LeftMeetingUiState.None)
    }

    private fun createViewModel(
        callEndedDueToFreePlanLimits: Boolean = false,
        callEndedDueToTooManyParticipants: Boolean = false,
    ) = LeftMeetingViewModel(
        args = LeftMeetingViewModel.Args(
            callEndedDueToFreePlanLimits = callEndedDueToFreePlanLimits,
            callEndedDueToTooManyParticipants = callEndedDueToTooManyParticipants,
        )
    )
}
