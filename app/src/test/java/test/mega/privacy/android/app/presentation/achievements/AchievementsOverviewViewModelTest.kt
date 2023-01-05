package test.mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.megaachievements.AchievementsOverviewViewModel
import mega.privacy.android.app.main.megaachievements.AchievementsUI
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverview
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsOverviewViewModelTest {

    private val getAccountAchievementsOverview: GetAccountAchievementsOverview = mock()

    private lateinit var underTest: AchievementsOverviewViewModel

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        initViewModel()
    }

    private fun initViewModel() {
        underTest = AchievementsOverviewViewModel(getAccountAchievementsOverview)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is progress`() = runTest {
        assertEquals(AchievementsUI.Progress, underTest.state.value)
    }

    @Test
    fun `test that state contains content when the use case returns a successful result`() =
        runTest {
            val achievements = AchievementsOverview(listOf(), listOf(), 0L, 0L, 0L)
            val expectedUiContent =
                AchievementsUI.Content(achievements, areAllRewardsExpired = true)
            whenever(getAccountAchievementsOverview()).thenReturn(achievements)

            underTest.state.test {
                val initialState = awaitItem()
                val contentState = awaitItem()

                assertEquals(expectedUiContent, contentState)
            }
        }

    @Test
    fun `test that state contains an error when the use case returns an exception`() = runTest {
        whenever(getAccountAchievementsOverview()).thenThrow(RuntimeException("Error"))

        underTest.state.test {
            val initialState = awaitItem()
            val errorState = awaitItem()

            assertEquals(AchievementsUI.Error, errorState)
        }
    }
}
