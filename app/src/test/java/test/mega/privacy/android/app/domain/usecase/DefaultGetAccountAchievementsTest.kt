package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.DefaultGetAccountAchievements
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAccountAchievementsTest {

    private lateinit var underTest: GetAccountAchievements
    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetAccountAchievements(accountRepository)
    }

    @Test
    fun `test that data is null when is achievements enabled is false`() = runTest {
        whenever(accountRepository.isAccountAchievementsEnabled()).thenReturn(false)
        assertNull(underTest(AchievementType.MEGA_ACHIEVEMENT_WELCOME, 0L))
    }

    @Test
    fun `test that data is not null and mega achievement is returned`() = runTest {
        val megaAchievement = mock<MegaAchievement> {
            on { grantedStorage }.thenReturn(124683L)
            on { grantedTransferQuota }.thenReturn(9964L)
            on { unlockedAwardsCount }.thenReturn(9833429L)
            on { achievementType }.thenReturn(AchievementType.MEGA_ACHIEVEMENT_WELCOME)
            on { awardId }.thenReturn(5)
            on { awardExpirationTimeStamp }.thenReturn(57835L)
            on { rewardAwardId }.thenReturn(4121)
            on { rewardStorageByAwardId }.thenReturn(37123L)
            on { rewardTransferByAwardId }.thenReturn(9746L)
        }
        whenever(accountRepository.isAccountAchievementsEnabled()).thenReturn(true)
        whenever(accountRepository.getAccountAchievements(AchievementType.MEGA_ACHIEVEMENT_WELCOME,
            0L)).thenReturn(megaAchievement)
        val actual = underTest(AchievementType.MEGA_ACHIEVEMENT_WELCOME, 0L)
        assertNotNull(actual)
        assertThat(actual).isSameInstanceAs(megaAchievement)
    }
}