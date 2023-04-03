package mega.privacy.android.domain.usecase.achievements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AreAchievementsEnabledUseCaseTest {
    private lateinit var underTest: AreAchievementsEnabledUseCase

    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = AreAchievementsEnabledUseCase(
            accountRepository = accountRepository
        )
    }

    @Test
    fun `test that achievements are enabled if repository returns true`() =
        runTest {
            whenever(accountRepository.areAccountAchievementsEnabled()).thenReturn(true)
            assertThat(underTest()).isTrue()
        }

    @Test
    fun `test that achievements are disabled if repository returns false`() =
        runTest {
            whenever(accountRepository.areAccountAchievementsEnabled()).thenReturn(false)
            assertThat(underTest()).isFalse()
        }
}
