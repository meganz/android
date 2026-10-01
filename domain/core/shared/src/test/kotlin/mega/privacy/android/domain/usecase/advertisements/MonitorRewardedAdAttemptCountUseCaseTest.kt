package mega.privacy.android.domain.usecase.advertisements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AdsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorRewardedAdAttemptCountUseCaseTest {
    private lateinit var underTest: MonitorRewardedAdAttemptCountUseCase
    private val adsRepository = mock<AdsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorRewardedAdAttemptCountUseCase(adsRepository = adsRepository)
    }

    @Test
    fun `test that invoke returns the flow from the repository`() = runTest {
        whenever(adsRepository.monitorRewardedAdAttemptCount()).thenReturn(flowOf(0, 1, 4, 7))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(4)
            assertThat(awaitItem()).isEqualTo(7)
            awaitComplete()
        }
    }
}
