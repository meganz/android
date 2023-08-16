package mega.privacy.android.feature.devicecenter.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetDevicesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetDevicesUseCaseTest {

    private lateinit var underTest: GetDevicesUseCase

    private val deviceCenterRepository = mock<DeviceCenterRepository>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetDevicesUseCase(
            deviceCenterRepository = deviceCenterRepository,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterRepository, isCameraUploadsEnabledUseCase)
    }

    @ParameterizedTest(name = "is camera uploads enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that get devices returns the list of backup devices`(isCameraUploadsEnabled: Boolean) =
        runTest {
            val backupInfoList = listOf<BackupInfo>(
                mock()
            )
            val currentDeviceId = "12345-6789"
            val deviceIdAndNameMap = mapOf(currentDeviceId to "Device Name One")
            val deviceNodes = listOf(
                mock<OwnDeviceNode>(),
                mock<OtherDeviceNode>()
            )

            whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
            whenever(deviceCenterRepository.getDeviceId()).thenReturn(currentDeviceId)
            whenever(deviceCenterRepository.getBackupInfo()).thenReturn(backupInfoList)
            whenever(deviceCenterRepository.getDeviceIdAndNameMap()).thenReturn(deviceIdAndNameMap)
            whenever(
                deviceCenterRepository.getDevices(
                    backupInfoList = backupInfoList,
                    currentDeviceId = currentDeviceId,
                    deviceIdAndNameMap = deviceIdAndNameMap,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                )
            ).thenReturn(deviceNodes)

            assertThat(underTest()).isEqualTo(deviceNodes)
        }
}