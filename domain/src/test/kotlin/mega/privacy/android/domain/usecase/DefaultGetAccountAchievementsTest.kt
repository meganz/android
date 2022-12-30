package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAccountAchievementsTest {

    private lateinit var underTest: GetAccountAchievements
    private val accountRepository = mock<AccountRepository>()
    private val achievement = mock<MegaAchievement>()

    @Before
    fun setUp() {
        underTest = DefaultGetAccountAchievements(accountRepository)
    }

    @Test
    fun `test that data is null when is achievements enabled is false`() = runTest {
        whenever(accountRepository.areAccountAchievementsEnabled()).thenReturn(false)
        verify(accountRepository, never()).getAccountAchievements(any(), any())
        verifyNoMoreInteractions(accountRepository)
    }

    @Test
    fun `test that data is not null and mega achievement is returned`() = runTest {
        whenever(accountRepository.areAccountAchievementsEnabled()).thenReturn(true)
        whenever(accountRepository.getAccountAchievements(AchievementType.MEGA_ACHIEVEMENT_WELCOME,
            5L)).thenReturn(achievement)
        val actual = underTest(AchievementType.MEGA_ACHIEVEMENT_WELCOME, 5L)
        assertThat(actual).isSameInstanceAs(achievement)
    }
}
