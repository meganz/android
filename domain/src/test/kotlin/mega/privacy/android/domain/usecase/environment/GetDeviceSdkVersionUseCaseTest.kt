package mega.privacy.android.domain.usecase.environment

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDeviceSdkVersionUseCaseTest {

    private lateinit var underTest: GetDeviceSdkVersionUseCase

    private val environmentRepository: EnvironmentRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = GetDeviceSdkVersionUseCase(environmentRepository = environmentRepository)
    }

    @Test
    fun `test that invoke returns the same value as the repository`() {
        val expected = 34
        whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(expected)

        assertThat(underTest()).isEqualTo(expected)
    }
}
