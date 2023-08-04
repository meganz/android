package mega.privacy.android.feature.devicecenter.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    @BeforeAll
    fun setUp() {
        underTest = GetDevicesUseCase(deviceCenterRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterRepository)
    }

    @Test
    fun `test that get devices returns the list of backup devices`() = runTest {
        val currentDeviceId = "12345-6789"
        val backupInfoList = listOf<BackupInfo>(
            mock()
        )
        val deviceIdAndNameMap = mapOf(currentDeviceId to "Device Name One")
        val deviceNodes = listOf(
            mock<OwnDeviceNode>(),
            mock<OtherDeviceNode>()
        )

        whenever(deviceCenterRepository.getDeviceId()).thenReturn(currentDeviceId)
        whenever(deviceCenterRepository.getBackupInfo()).thenReturn(backupInfoList)
        whenever(deviceCenterRepository.getDeviceIdAndNameMap()).thenReturn(deviceIdAndNameMap)
        whenever(
            deviceCenterRepository.getDevices(
                currentDeviceId = currentDeviceId,
                backupInfoList = backupInfoList,
                deviceIdAndNameMap = deviceIdAndNameMap,
            )
        ).thenReturn(deviceNodes)

        assertThat(underTest()).isEqualTo(deviceNodes)
    }
}