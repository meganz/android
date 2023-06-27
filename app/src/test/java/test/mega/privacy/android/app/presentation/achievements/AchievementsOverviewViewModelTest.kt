package test.mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.achievements.AchievementsOverviewViewModel
import mega.privacy.android.app.presentation.achievements.model.AchievementsUIState
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

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
    fun `test that initial state is empty`() = runTest {
        assertEquals(AchievementsUIState(), underTest.state.value)
    }

    @Test
    fun `test that state contains content when the achievements overview use case returns achievements`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)
            val expectedUiContent =
                AchievementsUIState(
                    achievementsOverview = fakeAchievements,
                    areAllRewardsExpired = true,
                    currentStorage = 0,
                )

            underTest.state.test {
                val initialState = awaitItem()
                val contentState = awaitItem()
                assertEquals(expectedUiContent, contentState)
            }
        }

    @Test
    fun `test that state contains an error when the get achievements overview use case returns an exception`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenThrow(RuntimeException("Error"))

            underTest.state.test {
                val initialState = awaitItem()
                val errorState = awaitItem()
                assertEquals(AchievementsUIState(showError = true), errorState)
            }
        }
}
