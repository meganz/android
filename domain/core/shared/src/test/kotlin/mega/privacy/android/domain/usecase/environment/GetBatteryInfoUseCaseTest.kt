package mega.privacy.android.domain.usecase.environment

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetBatteryInfoUseCaseTest {

    private lateinit var underTest: GetBatteryInfoUseCase

    private val environmentRepository: EnvironmentRepository = mock()

    @Before
    fun setup() {
        underTest = GetBatteryInfoUseCase(environmentRepository = environmentRepository)
    }

    @Test
    fun `test that the use case returns the same value as the repository`() = runTest {
        val expected = BatteryInfo(100, true)
        whenever(environmentRepository.getBatteryInfo()).thenReturn(expected)
        assertThat(underTest()).isEqualTo(expected)
    }
}