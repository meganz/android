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
 * Test class for [RenameDeviceUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RenameDeviceUseCaseTest {

    private lateinit var underTest: RenameDeviceUseCase

    private val deviceCenterRepository = mock<DeviceCenterRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RenameDeviceUseCase(deviceCenterRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterRepository)
    }

    @Test
    fun `test that rename device is invoked`() = runTest {
        val deviceId = "123-456"
        val deviceName = "New Device Name"

        underTest.invoke(
            deviceId = deviceId,
            deviceName = deviceName,
        )
        verify(deviceCenterRepository).renameDevice(
            deviceId = deviceId,
            deviceName = deviceName,
        )
    }
}