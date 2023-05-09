package test.mega.privacy.android.app.presentation.achievements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.achievements.AchievementsViewModel
import mega.privacy.android.app.presentation.achievements.UIMegaAchievement
import mega.privacy.android.app.presentation.achievements.UIMegaAchievementMapper
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.DefaultMegaAchievement
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.usecase.GetAccountAchievements
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AchievementsViewModelTest {

    private lateinit var underTest: AchievementsViewModel
    private val getAccountAchievements = mock<GetAccountAchievements>()
    private val uiMegaAchievementMapper = mock<UIMegaAchievementMapper>()
    private val megaAchievement = mock<DefaultMegaAchievement>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = AchievementsViewModel(getAccountAchievements = getAccountAchievements,
            uiMegaAchievementMapper = uiMegaAchievementMapper)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.achievementType).isEqualTo(AchievementType.INVALID_ACHIEVEMENT)
        }
    }

    @Test
    fun `test that state is getting updated with ui inputs`() = runTest {
        underTest.setAchievementType(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
        underTest.setAwardCount(10L)
        underTest.setToolbarTitle("Title")

        underTest.state.test {
            val updatedState = awaitItem()
            assertEquals(AchievementType.MEGA_ACHIEVEMENT_WELCOME, updatedState.achievementType)
            assertEquals(10L, updatedState.awardCount)
            assertEquals("Title", updatedState.toolbarTitle)
        }
    }

    @Test
    fun `test that expected ui achievements mapper is returned from use case`() = runTest {
        whenever(uiMegaAchievementMapper(megaAchievement)).thenAnswer {
            UIMegaAchievement(
                invitedEmails = listOf("john@yopmail.com", "doe@yopmail.com"),
                grantedStorage = 1234L,
                grantedTransferQuota = 43493L,
                unlockedAwardsCount = 10L,
                achievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME,
                awardId = 5,
                awardExpirationTimeStamp = 8472929L,
                rewardAwardId = 45,
                rewardStorageByAwardId = 36789201L,
                rewardTransferByAwardId = 32189312L)
        }

        whenever(getAccountAchievements(AchievementType.MEGA_ACHIEVEMENT_WELCOME,
            10L)).thenAnswer { uiMegaAchievementMapper(megaAchievement) }

        underTest.state.map {
            it.copy(uiMegaAchievement = uiMegaAchievementMapper(megaAchievement))
        }.distinctUntilChanged().test {
            assertThat(awaitItem().uiMegaAchievement).isNotNull()
        }

        underTest.state.map {
            it.copy(uiMegaAchievement = uiMegaAchievementMapper(megaAchievement))
        }.distinctUntilChanged().test {
            assertThat(awaitItem().uiMegaAchievement?.achievementType).isEqualTo(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}