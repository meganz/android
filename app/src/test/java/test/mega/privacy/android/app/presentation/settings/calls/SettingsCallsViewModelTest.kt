package test.mega.privacy.android.app.presentation.settings.calls


import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.calls.SettingsCallsViewModel
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.meeting.SetCallsSoundNotifications
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsCallsViewModelTest {

    private lateinit var underTest: SettingsCallsViewModel

    private val getCallsSoundNotifications = mock<GetCallsSoundNotifications> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val setCallsSoundNotifications = mock<SetCallsSoundNotifications>()

    private val scheduler = TestCoroutineScheduler()

    private val sendStatisticsMeetings = mock<SendStatisticsMeetingsUseCase>()

    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = SettingsCallsViewModel(
            getCallsSoundNotifications = getCallsSoundNotifications,
            setCallsSoundNotifications = setCallsSoundNotifications,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetings,
            ioDispatcher = standardDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            Truth.assertThat(initial.soundNotifications).isNull()
        }
    }

    @Test
    fun `test that the option returned by getCallsSoundNotifications is set the status of call notification sounds`() =
        runTest {
            whenever(getCallsSoundNotifications()).thenReturn(flowOf(CallsSoundNotifications.Enabled))

            underTest.uiState.map { it.soundNotifications }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundNotifications.Enabled)
            }
        }

    @Test
    fun `test that status of call notification sounds is updated when a new value is emitted`() =
        runTest {
            whenever(getCallsSoundNotifications()).thenReturn(
                flowOf(
                    CallsSoundNotifications.Enabled,
                    CallsSoundNotifications.Disabled
                )
            )

            underTest.uiState.map { it.soundNotifications }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundNotifications.Enabled)
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundNotifications.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsSoundNotifications calls the set use case with the correct value`() =
        runTest {
            underTest.setNewCallsSoundNotifications(CallsSoundNotifications.Disabled)
            scheduler.advanceUntilIdle()
            verify(setCallsSoundNotifications).invoke(CallsSoundNotifications.Disabled)
        }
}