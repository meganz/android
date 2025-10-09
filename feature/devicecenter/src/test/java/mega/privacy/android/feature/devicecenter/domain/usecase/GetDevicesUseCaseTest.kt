package mega.privacy.android.feature.devicecenter.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdAndNameMapUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.usecase.mapper.DeviceNodeMapper
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

    private val deviceNodeMapper = mock<DeviceNodeMapper>()
    private val getBackupInfoUseCase = mock<GetBackupInfoUseCase>()
    private val getDeviceIdAndNameMapUseCase = mock<GetDeviceIdAndNameMapUseCase>()
    private val getDeviceIdUseCase = mock<GetDeviceIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetDevicesUseCase(
            deviceNodeMapper = deviceNodeMapper,
            getBackupInfoUseCase = getBackupInfoUseCase,
            getDeviceIdAndNameMapUseCase = getDeviceIdAndNameMapUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            deviceNodeMapper,
            getBackupInfoUseCase,
            getDeviceIdAndNameMapUseCase,
            getDeviceIdUseCase,
        )
    }

    @ParameterizedTest(name = "is camera uploads enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that get devices returns the list of backup devices`(isCameraUploadsEnabled: Boolean) =
        runTest {
            val backupInfoList = listOf(mock<BackupInfo>())
            val currentDeviceId = "12345-6789"
            val deviceIdAndNameMap = mapOf(currentDeviceId to "Device Name One")
            val deviceNodes = listOf(mock<OwnDeviceNode>(), mock<OtherDeviceNode>())

            whenever(getDeviceIdUseCase()).thenReturn(currentDeviceId)
            whenever(getBackupInfoUseCase()).thenReturn(backupInfoList)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(deviceIdAndNameMap)
            whenever(
                deviceNodeMapper(
                    backupInfoList = backupInfoList,
                    currentDeviceId = currentDeviceId,
                    deviceIdAndNameMap = deviceIdAndNameMap,
                )
            ).thenReturn(deviceNodes)

            assertThat(
                underTest()
            ).isEqualTo(
                deviceNodes
            )
        }
}
