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
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingRemindersUseCase
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
    private val getCallsMeetingInvitations = mock<GetCallsMeetingInvitationsUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val setCallsMeetingInvitations = mock<SetCallsMeetingInvitationsUseCase>()
    private val getCallsMeetingReminders = mock<GetCallsMeetingRemindersUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val setCallsMeetingReminders = mock<SetCallsMeetingRemindersUseCase>()

    private val getFeatureFlagValue = mock<GetFeatureFlagValueUseCase>()
    private val sendStatisticsMeetings = mock<SendStatisticsMeetingsUseCase>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = SettingsCallsViewModel(
            getCallsSoundNotifications = getCallsSoundNotifications,
            setCallsSoundNotifications = setCallsSoundNotifications,
            getCallsMeetingInvitations = getCallsMeetingInvitations,
            setCallsMeetingInvitations = setCallsMeetingInvitations,
            getCallsMeetingReminders = getCallsMeetingReminders,
            setCallsMeetingReminders = setCallsMeetingReminders,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetings,
            getFeatureFlagValue = getFeatureFlagValue,
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
            Truth.assertThat(initial.callsMeetingInvitations).isNull()
            Truth.assertThat(initial.callsMeetingReminders).isNull()
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
            underTest.setNewCallsSoundNotifications(false)
            scheduler.advanceUntilIdle()
            verify(setCallsSoundNotifications).invoke(CallsSoundNotifications.Disabled)
        }

    @Test
    fun `test that the option returned by getCallsMeetingInvitations is set as the status of calls meeting invitations`() =
        runTest {
            whenever(getCallsMeetingInvitations()).thenReturn(flowOf(CallsMeetingInvitations.Enabled))

            underTest.uiState.map { it.callsMeetingInvitations }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Enabled)
            }
        }

    @Test
    fun `test that status of calls meeting invitations is updated when a new value is emitted`() =
        runTest {
            whenever(getCallsMeetingInvitations()).thenReturn(
                flowOf(
                    CallsMeetingInvitations.Enabled,
                    CallsMeetingInvitations.Disabled
                )
            )

            underTest.uiState.map { it.callsMeetingInvitations }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Enabled)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsMeetingInvitations calls the set use case with the correct value`() =
        runTest {
            underTest.setNewCallsMeetingInvitations(false)
            scheduler.advanceUntilIdle()
            verify(setCallsMeetingInvitations).invoke(CallsMeetingInvitations.Disabled)
        }

    @Test
    fun `test that the option returned by getCallsMeetingReminders is set as the status of calls meeting reminders`() =
        runTest {
            whenever(getCallsMeetingReminders()).thenReturn(flowOf(CallsMeetingReminders.Enabled))

            underTest.uiState.map { it.callsMeetingReminders }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Enabled)
            }
        }

    @Test
    fun `test that status of calls meeting reminders is updated when a new value is emitted`() =
        runTest {
            whenever(getCallsMeetingReminders()).thenReturn(
                flowOf(
                    CallsMeetingReminders.Enabled,
                    CallsMeetingReminders.Disabled
                )
            )

            underTest.uiState.map { it.callsMeetingReminders }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Enabled)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsMeetingReminders calls the set use case with the correct value`() =
        runTest {
            underTest.setNewCallsMeetingReminders(false)
            scheduler.advanceUntilIdle()
            verify(setCallsMeetingReminders).invoke(CallsMeetingReminders.Disabled)
        }
}
