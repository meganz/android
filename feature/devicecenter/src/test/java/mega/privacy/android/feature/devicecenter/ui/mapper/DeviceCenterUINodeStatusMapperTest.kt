package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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

    @ParameterizedTest(name = "when isDevice is {0} and errorSubState is {1}")
    @MethodSource("provideBlockedParameters")
    fun `test that a blocked UI node status is returned`(
        isDevice: Boolean,
        errorSubState: BackupInfoSubState?,
    ) {
        assertThat(
            underTest(
                isDevice = isDevice,
                status = DeviceCenterNodeStatus.Blocked(errorSubState = errorSubState),
            )
        ).isEqualTo(DeviceCenterUINodeStatus.Blocked)
    }

    private fun provideBlockedParameters() = Stream.of(
        Arguments.of(true, BackupInfoSubState.ACCOUNT_BLOCKED),
        Arguments.of(true, null),
        Arguments.of(false, null),
    )

    @Test
    fun `test that a folder error UI node status is returned if the blocked folder node status has a sub state`() {
        val errorMessageRes = R.string.device_center_list_view_item_sub_state_account_blocked
        whenever(deviceFolderUINodeErrorMessageMapper(any())).thenReturn(errorMessageRes)
        assertThat(
            underTest(
                isDevice = false,
                status = DeviceCenterNodeStatus.Blocked(errorSubState = BackupInfoSubState.ACCOUNT_BLOCKED),
            )
        ).isEqualTo(DeviceCenterUINodeStatus.FolderError(errorMessageRes))
    }

    @ParameterizedTest(name = "when isDevice is {0} and errorSubState is {1}")
    @MethodSource("provideOverquotaParameters")
    fun `test that an overquota UI node status is returned`(
        isDevice: Boolean,
        errorSubState: BackupInfoSubState?,
    ) {
        assertThat(
            underTest(
                isDevice = isDevice,
                status = DeviceCenterNodeStatus.Overquota(errorSubState = errorSubState),
            )
        ).isEqualTo(DeviceCenterUINodeStatus.Overquota)
    }

    private fun provideOverquotaParameters() = Stream.of(
        Arguments.of(true, BackupInfoSubState.STORAGE_OVERQUOTA),
        Arguments.of(true, null),
        Arguments.of(false, null),
    )

    @Test
    fun `test that a folder error UI node status is returned if the overquota folder node status has a sub state`() {
        val errorMessageRes = R.string.device_center_list_view_item_sub_state_storage_overquota
        whenever(deviceFolderUINodeErrorMessageMapper(any())).thenReturn(errorMessageRes)
        assertThat(
            underTest(
                isDevice = false,
                status = DeviceCenterNodeStatus.Overquota(errorSubState = BackupInfoSubState.STORAGE_OVERQUOTA),
            )
        ).isEqualTo(DeviceCenterUINodeStatus.FolderError(errorMessageRes))
    }

    @ParameterizedTest(name = "when the node status is {0}, its ui node status equivalent is {1}")
    @MethodSource("provideParameters")
    fun `test that all other mappings are correct`(
        deviceCenterNodeStatus: DeviceCenterNodeStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(
            underTest(
                isDevice = true,
                status = deviceCenterNodeStatus,
            )
        ).isEqualTo(expectedNodeUiStatus)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(DeviceCenterNodeStatus.Stopped, DeviceCenterUINodeStatus.Stopped),
        Arguments.of(DeviceCenterNodeStatus.Disabled, DeviceCenterUINodeStatus.Disabled),
        Arguments.of(DeviceCenterNodeStatus.Offline, DeviceCenterUINodeStatus.Offline),
        Arguments.of(DeviceCenterNodeStatus.UpToDate, DeviceCenterUINodeStatus.UpToDate),
        Arguments.of(DeviceCenterNodeStatus.Paused, DeviceCenterUINodeStatus.Paused),
        Arguments.of(DeviceCenterNodeStatus.Initializing, DeviceCenterUINodeStatus.Initializing),
        Arguments.of(DeviceCenterNodeStatus.Scanning, DeviceCenterUINodeStatus.Scanning),
        Arguments.of(
            DeviceCenterNodeStatus.Syncing(50),
            DeviceCenterUINodeStatus.SyncingWithPercentage(50),
        ),
        Arguments.of(
            DeviceCenterNodeStatus.Syncing(0),
            DeviceCenterUINodeStatus.Syncing,
        ),
        Arguments.of(
            DeviceCenterNodeStatus.NoCameraUploads,
            DeviceCenterUINodeStatus.CameraUploadsDisabled,
        ),
    )

    @Test
    fun `test that a non matching device status returns a default ui device status`() {
        assertThat(
            underTest(
                isDevice = true,
                status = DeviceCenterNodeStatus.Unknown,
            )
        ).isEqualTo(DeviceCenterUINodeStatus.Unknown)
    }
}