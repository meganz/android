package mega.privacy.android.domain.usecase.advertisements

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AdsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetRewardedAdAttemptCountUseCaseTest {
    private lateinit var underTest: ResetRewardedAdAttemptCountUseCase
    private val adsRepository = mock<AdsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ResetRewardedAdAttemptCountUseCase(adsRepository = adsRepository)
    }

    @Test
    fun `test that invoke calls reset on the repository`() = runTest {
        underTest()

        verify(adsRepository).resetRewardedAdAttemptCount()
    }
}
