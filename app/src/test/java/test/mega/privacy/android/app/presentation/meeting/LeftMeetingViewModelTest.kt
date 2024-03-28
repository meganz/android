package test.mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LeftMeetingViewModelTest {

    private val callEndedDueLimit: Boolean = true

    private lateinit var underTest: LeftMeetingViewModel
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val savedStateHandle: SavedStateHandle = mock {
        on { get<Boolean>(MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT) } doReturn true
    }

    @BeforeAll
    internal fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            savedStateHandle,
        )
        whenever(savedStateHandle.get<Boolean>(MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT)).thenReturn(
            true
        )
        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.CallUnlimitedProPlan) } doReturn true
    }

    private fun initTestClass() {
        underTest = LeftMeetingViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
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


}