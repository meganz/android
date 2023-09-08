package mega.privacy.android.feature.devicecenter.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.feature.devicecenter.data.mapper.DeviceNodeMapper
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
 * Test class for [DeviceCenterRepository]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterRepositoryImplTest {
    private lateinit var underTest: DeviceCenterRepository

    private val deviceNodeMapper = mock<DeviceNodeMapper>()
    private val megaApiGateway = mock<MegaApiGateway>()

    @BeforeAll
    fun setUp() {
        underTest = DeviceCenterRepositoryImpl(
            deviceNodeMapper = deviceNodeMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            deviceNodeMapper,
            megaApiGateway
        )
    }

    @ParameterizedTest(name = "is camera uploads enabled")
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

            whenever(
                deviceNodeMapper(
                    backupInfoList = backupInfoList,
                    currentDeviceId = currentDeviceId,
                    deviceIdAndNameMap = deviceIdAndNameMap,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                )
            ).thenReturn(deviceNodes)
            assertThat(
                underTest.getDevices(
                    backupInfoList = backupInfoList,
                    currentDeviceId = currentDeviceId,
                    deviceIdAndNameMap = deviceIdAndNameMap,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                )
            ).isEqualTo(deviceNodes)
        }
}
