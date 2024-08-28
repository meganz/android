package mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.AchievementsOverviewViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AchievementsOverviewViewModelTest {

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()
    private val areAchievementsEnabled: AreAchievementsEnabledUseCase = mock()
    private lateinit var underTest: AchievementsOverviewViewModel

    private val fakeAchievements = AchievementsOverview(
        allAchievements = listOf(),
        awardedAchievements = listOf(),
        currentStorageInBytes = 0L,
        achievedStorageFromReferralsInBytes = 0L,
        achievedTransferFromReferralsInBytes = 0L
    )

    @BeforeEach
    fun setUp() {
        runBlocking { stubCommon() }
        initViewModel()
    }

    @AfterEach
    fun tearDown() {
        reset(
            getAccountAchievementsOverviewUseCase,
            areAchievementsEnabled
        )
    }

    private suspend fun stubCommon() {
        whenever(areAchievementsEnabled()).thenReturn(false)
        whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)
    }

    private fun initViewModel() {
        underTest = AchievementsOverviewViewModel(
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
            areAchievementsEnabled = areAchievementsEnabled,
        )
    }

    @Test
    fun `test that state contains content when the achievements overview use case returns achievements`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenReturn(fakeAchievements)

            underTest.state.test {
                assertThat(awaitItem().achievementsOverview).isEqualTo(fakeAchievements)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state contains an error when the get achievements overview use case returns an exception`() =
        runTest {
            whenever(getAccountAchievementsOverviewUseCase()).thenThrow(RuntimeException("Error"))
            initViewModel()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isInstanceOf(StateEventWithContentTriggered(R.string.cancel_subscription_error)::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
