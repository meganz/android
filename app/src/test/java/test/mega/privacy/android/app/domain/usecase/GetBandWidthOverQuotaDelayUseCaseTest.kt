package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetBandWidthOverQuotaDelayUseCase
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetBandWidthOverQuotaDelayUseCaseTest {
    private lateinit var underTest: GetBandWidthOverQuotaDelayUseCase
    private val repository: NodeRepository = mock()

    @Before
    fun setUp() {
        underTest = GetBandWidthOverQuotaDelayUseCase(repository = repository)
    }

    @Test
    fun `test that get banner quota time`() = runTest {
        val timer = 1000L
        whenever(repository.getBannerQuotaTime()).thenReturn(timer)
        val testTimer = underTest()
        Truth.assertThat(testTimer).isEqualTo(timer)
    }
}