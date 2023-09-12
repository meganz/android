package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupDeviceNameUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupDeviceNameUseCaseTest {

    private lateinit var underTest: SetupDeviceNameUseCase

    private val getDeviceNameUseCase: GetDeviceNameUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val setDeviceNameUseCase: RenameDeviceUseCase = mock()
    private val environmentRepository: EnvironmentRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupDeviceNameUseCase(
            getDeviceNameUseCase = getDeviceNameUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            setDeviceNameUseCase = setDeviceNameUseCase,
            environmentRepository = environmentRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getDeviceNameUseCase,
            getDeviceIdUseCase,
            setDeviceNameUseCase,
            environmentRepository
        )
    }

    @Test
    fun `test that device name is not retrieved when device id is not available`() = runTest {
        whenever(getDeviceIdUseCase()).thenReturn(null)
        underTest()
        verifyNoInteractions(getDeviceNameUseCase, setDeviceNameUseCase, environmentRepository)
    }

    @Test
    fun `test that device name is retrieved when device id is available`() = runTest {
        val deviceId = "1234"
        whenever(getDeviceIdUseCase()).thenReturn(deviceId)
        whenever(getDeviceNameUseCase(deviceId)).thenReturn("Samsung S23")
        underTest()
        verifyNoInteractions(setDeviceNameUseCase, environmentRepository)
    }

    @ParameterizedTest(name = "device name is {0}")
    @NullAndEmptySource
    fun `test that device name is set when`(deviceName: String?) = runTest {
        val deviceId = "1234"
        val localDeviceName = "Samsung S23"
        val deviceInfo = mock<DeviceInfo> {
            on { device }.thenReturn(localDeviceName)
        }
        whenever(getDeviceIdUseCase()).thenReturn(deviceId)
        whenever(getDeviceNameUseCase(deviceId)).thenReturn(deviceName)
        whenever(environmentRepository.getDeviceInfo()).thenReturn(deviceInfo)
        underTest()
        verify(setDeviceNameUseCase).invoke(deviceId, localDeviceName)
    }
}
