package mega.privacy.android.domain.usecase.environment

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalDeviceNameUseCaseTest {

    private lateinit var underTest: GetLocalDeviceNameUseCase

    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeEach
    fun setup() {
        underTest = GetLocalDeviceNameUseCase(environmentRepository = environmentRepository)
    }

    @Test
    fun `test that invoke returns the same value as the repository`() {
        val expected = "manufacturer deviceName"
        whenever(environmentRepository.getDeviceName()).thenReturn(expected)

        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    fun `test that invoke calls repository getDeviceName`() {
        whenever(environmentRepository.getDeviceName()).thenReturn("device")

        underTest()

        verify(environmentRepository).getDeviceName()
    }
}
