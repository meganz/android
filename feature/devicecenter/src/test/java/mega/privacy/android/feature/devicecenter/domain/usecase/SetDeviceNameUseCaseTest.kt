package mega.privacy.android.feature.devicecenter.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetDeviceNameUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetDeviceNameUseCaseTest {

    private lateinit var underTest: SetDeviceNameUseCase

    private val deviceCenterRepository = mock<DeviceCenterRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetDeviceNameUseCase(deviceCenterRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterRepository)
    }

    @Test
    fun `test that set device name is invoked`() = runTest {
        val deviceName = "Test Device Name"
        underTest(deviceName)
        verify(deviceCenterRepository).setDeviceName(deviceName)
    }
}