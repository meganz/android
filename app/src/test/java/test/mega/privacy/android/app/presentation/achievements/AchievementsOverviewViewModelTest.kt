package test.mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.AchievementsOverviewViewModel
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsOverviewViewModelTest {

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()
    private val areAchievementsEnabled: AreAchievementsEnabledUseCase = mock()
    private lateinit var underTest: AchievementsOverviewViewModel

    private val scheduler = TestCoroutineScheduler()

    private val fakeAchievements = AchievementsOverview(
        allAchievements = listOf(),
        awardedAchievements = listOf(),
        currentStorageInBytes = 0L,
        achievedStorageFromReferralsInBytes = 0L,
        achievedTransferFromReferralsInBytes = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        initViewModel()
    }

    private fun initViewModel() {
        underTest = AchievementsOverviewViewModel(
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
            areAchievementsEnabled = areAchievementsEnabled,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that state contains content when the achievements overview use case returns achievements`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)

            underTest.state.test {
                awaitItem()
                assertThat(awaitItem().achievementsOverview).isEqualTo(fakeAchievements)
            }
        }

    @Test
    fun `test that state contains an error when the get achievements overview use case returns an exception`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenThrow(RuntimeException("Error"))

            underTest.state.test {
                awaitItem()
                assertThat(awaitItem().errorMessage).isInstanceOf(StateEventWithContentTriggered(R.string.cancel_subscription_error)::class.java)
            }
        }
}
