package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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

    @ParameterizedTest(name = "when the folder type is {0}")
    @MethodSource("provideDeviceFolderTypeParameters")
    fun `test that an error UI folder status is returned without a specific error message`(
        type: BackupInfoType,
    ) {
        assertThat(underTest(type, DeviceFolderStatus.Error(errorSubState = null))).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = null)
        )
        verifyNoInteractions(deviceFolderUINodeErrorMessageMapper)
    }

    @ParameterizedTest(name = "when the folder type is {0}")
    @MethodSource("provideDeviceFolderTypeParameters")
    fun `test that an error UI folder status is returned with a specific error message`(
        type: BackupInfoType,
    ) {
        val errorSubState = SyncError.INSUFFICIENT_DISK_SPACE
        val specificErrorMessage =
            R.string.device_center_list_view_item_sub_state_insufficient_disk_space

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(
            underTest(type, DeviceFolderStatus.Error(errorSubState = errorSubState))
        ).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @ParameterizedTest(name = "when the folder type is {0}")
    @MethodSource("provideDeviceFolderTypeParameters")
    fun `test that a blocked UI folder status is returned with a specific error message`(
        type: BackupInfoType,
    ) {
        val errorSubState = SyncError.ACCOUNT_BLOCKED
        val specificErrorMessage = R.string.device_center_list_view_item_sub_state_account_blocked

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(
            underTest(type, DeviceFolderStatus.Error(errorSubState = errorSubState))
        ).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @ParameterizedTest(name = "when the folder type is {0}")
    @MethodSource("provideDeviceFolderTypeParameters")
    fun `test that an overquota UI folder status is returned with a specific error message`(
        type: BackupInfoType,
    ) {
        val errorSubState = SyncError.STORAGE_OVERQUOTA
        val specificErrorMessage = R.string.device_center_list_view_item_sub_state_storage_overquota

        whenever(deviceFolderUINodeErrorMessageMapper(errorSubState)).thenReturn(
            specificErrorMessage
        )
        assertThat(
            underTest(type, DeviceFolderStatus.Error(errorSubState = errorSubState))
        ).isEqualTo(
            DeviceCenterUINodeStatus.Error(specificErrorMessage = specificErrorMessage)
        )
    }

    @ParameterizedTest(name = "when the folder type is {0} and status is {1}, its ui node status equivalent is {2}")
    @MethodSource("provideDeviceFolderTypeAndStatusParameters")
    fun `test that all other folder status mappings are correct`(
        type: BackupInfoType,
        deviceFolderStatus: DeviceFolderStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(underTest(type, deviceFolderStatus)).isEqualTo(expectedNodeUiStatus)
    }

    @ParameterizedTest(name = "when the device status is {0}, its ui node status equivalent is {1}")
    @MethodSource("provideDeviceStatusParameters")
    fun `test that device status mappings are correct`(
        deviceStatus: DeviceStatus,
        expectedNodeUiStatus: DeviceCenterUINodeStatus,
    ) {
        assertThat(underTest(deviceStatus)).isEqualTo(expectedNodeUiStatus)
    }

    private fun provideDeviceFolderTypeParameters() = Stream.of(
        Arguments.of(BackupInfoType.TWO_WAY_SYNC),
        Arguments.of(BackupInfoType.UP_SYNC),
        Arguments.of(BackupInfoType.DOWN_SYNC),
        Arguments.of(BackupInfoType.CAMERA_UPLOADS),
        Arguments.of(BackupInfoType.MEDIA_UPLOADS),
        Arguments.of(BackupInfoType.BACKUP_UPLOAD),
    )

    private fun provideDeviceFolderTypeAndStatusParameters() = Stream.of(
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Updating
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UpdatingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.TWO_WAY_SYNC,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),

        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Updating
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UpdatingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.UP_SYNC,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),

        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Updating
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UpdatingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.DOWN_SYNC,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),

        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Uploading
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UploadingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.CAMERA_UPLOADS,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),

        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Uploading
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UploadingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.MEDIA_UPLOADS,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),

        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Unknown,
            DeviceCenterUINodeStatus.Unknown
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Inactive,
            DeviceCenterUINodeStatus.Inactive
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Error(null),
            DeviceCenterUINodeStatus.Error(null)
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Paused,
            DeviceCenterUINodeStatus.Paused
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Disabled,
            DeviceCenterUINodeStatus.Disabled
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Updating(0),
            DeviceCenterUINodeStatus.Updating
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.Updating(50),
            DeviceCenterUINodeStatus.UpdatingWithPercentage(50)
        ),
        Arguments.of(
            BackupInfoType.BACKUP_UPLOAD,
            DeviceFolderStatus.UpToDate,
            DeviceCenterUINodeStatus.UpToDate
        ),
    )

    private fun provideDeviceStatusParameters() = Stream.of(
        Arguments.of(DeviceStatus.Unknown, DeviceCenterUINodeStatus.Unknown),
        Arguments.of(DeviceStatus.Inactive, DeviceCenterUINodeStatus.Inactive),
        Arguments.of(DeviceStatus.AttentionNeeded, DeviceCenterUINodeStatus.AttentionNeeded),
        Arguments.of(DeviceStatus.Updating, DeviceCenterUINodeStatus.Updating),
        Arguments.of(DeviceStatus.UpToDate, DeviceCenterUINodeStatus.UpToDate),
        Arguments.of(DeviceStatus.NothingSetUp, DeviceCenterUINodeStatus.NothingSetUp),
    )

    @ParameterizedTest(name = "when the folder type is {0}")
    @MethodSource("provideDeviceFolderTypeParameters")
    fun `test that a non matching device status returns a default UI device status`(
        type: BackupInfoType,
    ) {
        assertThat(underTest(type, DeviceFolderStatus.Unknown))
            .isEqualTo(DeviceCenterUINodeStatus.Unknown)
    }
}