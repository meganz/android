package test.mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LeftMeetingViewModelTest {

    private val callEndedDueLimit: Boolean = true

    private lateinit var underTest: LeftMeetingViewModel
    private val savedStateHandle: SavedStateHandle = mock {
        on { get<Boolean>(MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT) } doReturn true
        on { get<Boolean>(MeetingActivity.MEETING_PARTICIPANTS_LIMIT) } doReturn true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeAll
    internal fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            savedStateHandle,
        )
        whenever(savedStateHandle.get<Boolean>(MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT)).thenReturn(
            true
        )
        whenever(savedStateHandle.get<Boolean>(MeetingActivity.MEETING_PARTICIPANTS_LIMIT)).thenReturn(
            true
        )
    }

    private fun initTestClass() {
        underTest = LeftMeetingViewModel(
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `test that state update when we passing the callEndedDueLimit `() = runTest {
        whenever(savedStateHandle.get<Boolean>(MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT)).thenReturn(
            callEndedDueLimit
        )
        initTestClass()
        underTest.state.test {
            Truth.assertThat(awaitItem().callEndedDueToFreePlanLimits).isEqualTo(callEndedDueLimit)
        }
    }

    @Test
    fun `test that state update when we passing the callParticipantsLimit `() = runTest {
        whenever(savedStateHandle.get<Boolean>(MeetingActivity.MEETING_PARTICIPANTS_LIMIT)).thenReturn(
            callEndedDueLimit
        )
        initTestClass()
        underTest.state.test {
            Truth.assertThat(awaitItem().callEndedDueToTooManyParticipants)
                .isEqualTo(callEndedDueLimit)
        }
    }
}
