package mega.privacy.android.feature.devicecenter.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetDeviceNameUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetDeviceNameUseCaseTest {

    private lateinit var underTest: GetDeviceNameUseCase

    private val deviceCenterRepository = mock<DeviceCenterRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDeviceNameUseCase(deviceCenterRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterRepository)
    }

    @Test
    fun `test that when invoked device name is returned`() = runTest {
        val deviceName = "Samsung S23 Ultra"
        whenever(deviceCenterRepository.getDeviceName(any())).thenReturn(deviceName)
        val actual = underTest("abcd")
        Truth.assertThat(actual).isEqualTo(deviceName)
    }
}
