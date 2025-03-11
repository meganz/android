package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [DeviceCenterUINodeStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceCenterUINodeStatusMapperTest {
    private lateinit var underTest: DeviceCenterUINodeStatusMapper

    private val deviceFolderUINodeErrorMessageMapper = mock<DeviceFolderUINodeErrorMessageMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DeviceCenterUINodeStatusMapper(deviceFolderUINodeErrorMessageMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceFolderUINodeErrorMessageMapper)
    }

    @Test
    fun `test that an error UI folder status is returned without a specific error message`() {
        assertThat(underTest(DeviceFolderStatus.Error(errorSubState = null))).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = null)
        )
        verifyNoInteractions(deviceFolderUINodeErrorMessageMapper)
    }

    @Test
    fun `test that an error UI folder status is returned with a specific error message`() {
        val errorSubState = SyncError.INSUFFICIENT_DISK_SPACE
        val specificErrorMessage =
            R.string.device_center_list_view_item_sub_state_insufficient_disk_space

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(underTest(DeviceFolderStatus.Error(errorSubState = errorSubState))).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @Test
    fun `test that a blocked UI folder status is returned with a specific error message`() {
        val errorSubState = SyncError.ACCOUNT_BLOCKED
        val specificErrorMessage = R.string.device_center_list_view_item_sub_state_account_blocked

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(underTest(DeviceFolderStatus.Error(errorSubState = errorSubState))).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @Test
    fun `test that an overquota UI folder status is returned with a specific error message`() {
        val errorSubState = SyncError.STORAGE_OVERQUOTA
        val specificErrorMessage = R.string.device_center_list_view_item_sub_state_storage_overquota

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(underTest(DeviceFolderStatus.Error(errorSubState = errorSubState))).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @ParameterizedTest(name = "when the folder status is {0}, its ui node status equivalent is {1}")
    @MethodSource("provideDeviceFolderStatusParameters")
    fun `test that all other folder status mappings are correct`(
        deviceFolderStatus: DeviceFolderStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(underTest(deviceFolderStatus)).isEqualTo(expectedNodeUiStatus)
    }

    @ParameterizedTest(name = "when the device status is {0}, its ui node status equivalent is {1}")
    @MethodSource("provideDeviceStatusParameters")
    fun `test that device status mappings are correct`(
        deviceStatus: DeviceStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(underTest(deviceStatus)).isEqualTo(expectedNodeUiStatus)
    }

    private fun provideDeviceFolderStatusParameters() = Stream.of(
        Arguments.of(DeviceFolderStatus.Unknown, DeviceCenterUINodeStatus.Unknown),
        Arguments.of(DeviceFolderStatus.Inactive, DeviceCenterUINodeStatus.Inactive),
        Arguments.of(DeviceFolderStatus.Error(null), DeviceCenterUINodeStatus.Error(null)),
        Arguments.of(DeviceFolderStatus.Paused, DeviceCenterUINodeStatus.Paused),
        Arguments.of(DeviceFolderStatus.Disabled, DeviceCenterUINodeStatus.Disabled),
        Arguments.of(DeviceFolderStatus.Updating(0), DeviceCenterUINodeStatus.Updating),
        Arguments.of(
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UpdatingWithPercentage(50)
        ),
        Arguments.of(DeviceFolderStatus.UpToDate, DeviceCenterUINodeStatus.UpToDate),
    )

    private fun provideDeviceStatusParameters() = Stream.of(
        Arguments.of(DeviceStatus.Unknown, DeviceCenterUINodeStatus.Unknown),
        Arguments.of(DeviceStatus.Inactive, DeviceCenterUINodeStatus.Inactive),
        Arguments.of(DeviceStatus.AttentionNeeded, DeviceCenterUINodeStatus.AttentionNeeded),
        Arguments.of(DeviceStatus.Updating, DeviceCenterUINodeStatus.Updating),
        Arguments.of(DeviceStatus.UpToDate, DeviceCenterUINodeStatus.UpToDate),
        Arguments.of(DeviceStatus.NothingSetUp, DeviceCenterUINodeStatus.NothingSetUp),
    )

    @Test
    fun `test that a non matching device status returns a default UI device status`() {
        assertThat(underTest(DeviceFolderStatus.Unknown)).isEqualTo(DeviceCenterUINodeStatus.Unknown)
    }
}