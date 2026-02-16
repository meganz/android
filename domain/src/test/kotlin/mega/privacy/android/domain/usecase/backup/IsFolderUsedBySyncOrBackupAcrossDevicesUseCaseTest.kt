package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.DetermineNodeRelationshipUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsFolderUsedBySyncOrBackupAcrossDevicesUseCaseTest {

    private val getBackupInfoUseCase: GetBackupInfoUseCase = mock()
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase = mock()
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()

    private val underTest = IsFolderUsedBySyncOrBackupAcrossDevicesUseCase(
        getBackupInfoUseCase,
        getCameraUploadsSyncHandlesUseCase,
        determineNodeRelationshipUseCase,
        getDeviceIdUseCase,
        getFeatureFlagValueUseCase
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            getBackupInfoUseCase,
            getCameraUploadsSyncHandlesUseCase,
            determineNodeRelationshipUseCase,
            getDeviceIdUseCase,
            getFeatureFlagValueUseCase
        )
    }

    // Feature flag tests

    @Test
    fun `test that when feature flag is disabled, returns NotUsed without checking camera uploads or backups`() =
        runTest {
            val nodeId = NodeId(1L)
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
            verifyNoInteractions(getCameraUploadsSyncHandlesUseCase, getBackupInfoUseCase)
        }

    @Test
    fun `test that when feature flag throws exception, returns NotUsed`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenThrow(RuntimeException("Feature flag error"))

        val result = underTest(
            nodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
        verifyNoInteractions(getCameraUploadsSyncHandlesUseCase, getBackupInfoUseCase)
    }

    @Test
    fun `test that when feature flag is enabled, camera uploads are checked`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
        whenever(determineNodeRelationshipUseCase(nodeId, NodeId(1L)))
            .thenReturn(NodeRelationship.ExactMatch)

        val result = underTest(
            nodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUpload)
    }

    @Test
    fun `test that if folder is used by primary Camera Uploads then result is UsedByCameraUpload`() =
        runTest {
            val nodeId = NodeId(1L)
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(1L)))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUpload)
            verifyNoInteractions(getBackupInfoUseCase)
        }

    @Test
    fun `test that if folder is used by secondary Camera Uploads then result is UsedByMediaUpload`() =
        runTest {
            val nodeId = NodeId(2L)
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(1L)))
                .thenReturn(NodeRelationship.NoMatch)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(2L)))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            assertThat(result).isEqualTo(FolderUsageResult.UsedByMediaUpload)
            verifyNoInteractions(getBackupInfoUseCase)
        }

    @Test
    fun `test that if Camera Uploads is not set up then backup info is checked`() = runTest {
        val nodeId = NodeId(3L)
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
        whenever(getBackupInfoUseCase()).thenReturn(emptyList())

        val result =
            underTest(nodeId, shouldCheckCameraUploads = true, shouldExcludeCurrentDevice = false)

        assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
        verify(getCameraUploadsSyncHandlesUseCase).invoke()
        verify(getBackupInfoUseCase).invoke()
    }

    @Test
    fun `test that if folder is used by a backup then result is UsedBySyncOrBackup`() = runTest {
        val nodeId = NodeId(3L)
        val deviceId = "test-device-id"
        val backupInfo = mock<BackupInfo> {
            on { rootHandle }.thenReturn(nodeId)
            on { this.deviceId }.thenReturn(deviceId)
            on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
        }
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L) // Doesn't match
        whenever(determineNodeRelationshipUseCase(nodeId, NodeId(1L)))
            .thenReturn(NodeRelationship.NoMatch)
        whenever(determineNodeRelationshipUseCase(nodeId, NodeId(2L)))
            .thenReturn(NodeRelationship.NoMatch)
        whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
        whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
            .thenReturn(NodeRelationship.ExactMatch)

        val result =
            underTest(nodeId, shouldCheckCameraUploads = true, shouldExcludeCurrentDevice = false)

        val expected = FolderUsageResult.UsedBySyncOrBackup(deviceId)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that if folder is used by one of many backups then result is UsedBySyncOrBackup`() =
        runTest {
            // Arrange
            val nodeId = NodeId(5L)
            val deviceId = "matching-device-id"
            val backups = listOf(
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(3L))
                    on { this.deviceId }.thenReturn("other-device-1")
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                },
                mock<BackupInfo> { // The matching backup
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(deviceId)
                    on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(7L))
                    on { this.deviceId }.thenReturn("other-device-2")
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                }
            )
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
            whenever(getBackupInfoUseCase()).thenReturn(backups)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(3L)))
                .thenReturn(NodeRelationship.NoMatch)
            whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            val expected = FolderUsageResult.UsedBySyncOrBackup(deviceId)
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that if folder is not used by Camera Uploads or backups then result is NotUsed`() =
        runTest {
            val nodeId = NodeId(99L)
            val backups = listOf(
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(3L))
                    on { deviceId }.thenReturn("other-device-1")
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(7L))
                    on { deviceId }.thenReturn("other-device-2")
                    on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                }
            )
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
            whenever(getBackupInfoUseCase()).thenReturn(backups)
            whenever(determineNodeRelationshipUseCase(NodeId(99L), NodeId(1L)))
                .thenReturn(NodeRelationship.NoMatch)
            whenever(determineNodeRelationshipUseCase(NodeId(99L), NodeId(2L)))
                .thenReturn(NodeRelationship.NoMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
        }

    // Hierarchical relationship tests for Camera Uploads

    @Test
    fun `test that parent of Camera Uploads returns UsedByCameraUploadParent`() = runTest {
        // Assuming node hierarchy: /Photos (id=100) -> /Photos/Camera (id=1)
        val parentNodeId = NodeId(100L)
        val cameraUploadsNodeId = NodeId(1L)

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(cameraUploadsNodeId.longValue to 2L)
        whenever(determineNodeRelationshipUseCase(parentNodeId, cameraUploadsNodeId))
            .thenReturn(NodeRelationship.TargetIsDescendant) // target is descendant of source

        val result = underTest(
            parentNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUploadParent)
    }

    @Test
    fun `test that child of Camera Uploads returns UsedByCameraUploadChild`() = runTest {
        // Assuming node hierarchy: /Photos/Camera (id=1) -> /Photos/Camera/2023 (id=101)
        val childNodeId = NodeId(101L)
        val cameraUploadsNodeId = NodeId(1L)

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(cameraUploadsNodeId.longValue to 2L)
        whenever(determineNodeRelationshipUseCase(childNodeId, cameraUploadsNodeId))
            .thenReturn(NodeRelationship.TargetIsAncestor) // target is ancestor of source

        val result = underTest(
            childNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUploadChild)
    }

    // Hierarchical relationship tests for Media Uploads

    @Test
    fun `test that parent of Media Uploads returns UsedByMediaUploadParent`() = runTest {
        val parentNodeId = NodeId(200L)
        val mediaUploadsNodeId = NodeId(2L)

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to mediaUploadsNodeId.longValue)
        whenever(determineNodeRelationshipUseCase(parentNodeId, NodeId(1L)))
            .thenReturn(NodeRelationship.NoMatch)
        whenever(determineNodeRelationshipUseCase(parentNodeId, mediaUploadsNodeId))
            .thenReturn(NodeRelationship.TargetIsDescendant)

        val result = underTest(
            parentNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedByMediaUploadParent)
    }

    @Test
    fun `test that child of Media Uploads returns UsedByMediaUploadChild`() = runTest {
        val childNodeId = NodeId(202L)
        val mediaUploadsNodeId = NodeId(2L)

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to mediaUploadsNodeId.longValue)
        whenever(determineNodeRelationshipUseCase(childNodeId, NodeId(1L)))
            .thenReturn(NodeRelationship.NoMatch)
        whenever(determineNodeRelationshipUseCase(childNodeId, mediaUploadsNodeId))
            .thenReturn(NodeRelationship.TargetIsAncestor)

        val result = underTest(
            childNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedByMediaUploadChild)
    }

    // Hierarchical relationship tests for Backup

    @Test
    fun `test that parent of backup folder returns UsedBySyncOrBackupParent`() = runTest {
        val parentNodeId = NodeId(300L)
        val backupNodeId = NodeId(4L)
        val deviceId = "test-device"

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
        val backupInfo = mock<BackupInfo> {
            on { rootHandle }.thenReturn(backupNodeId)
            on { this.deviceId }.thenReturn(deviceId)
            on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
        }
        whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
        whenever(determineNodeRelationshipUseCase(parentNodeId, backupNodeId))
            .thenReturn(NodeRelationship.TargetIsDescendant)

        val result = underTest(
            parentNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedBySyncOrBackupParent(deviceId))
    }

    @Test
    fun `test that child of backup folder returns UsedBySyncOrBackupChild`() = runTest {
        val childNodeId = NodeId(302L)
        val backupNodeId = NodeId(4L)
        val deviceId = "test-device"

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
        val backupInfo = mock<BackupInfo> {
            on { rootHandle }.thenReturn(backupNodeId)
            on { this.deviceId }.thenReturn(deviceId)
            on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
        }
        whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
        whenever(determineNodeRelationshipUseCase(childNodeId, backupNodeId))
            .thenReturn(NodeRelationship.TargetIsAncestor)

        val result = underTest(
            childNodeId,
            shouldCheckCameraUploads = true,
            shouldExcludeCurrentDevice = false
        )

        assertThat(result).isEqualTo(FolderUsageResult.UsedBySyncOrBackupChild(deviceId))
    }

    // New test cases for shouldCheckCameraUploads and shouldExcludeCurrentDevice parameters

    @Test
    fun `test that when shouldCheckCameraUploads is false, camera uploads are skipped and backups are checked`() =
        runTest {
            val nodeId = NodeId(1L)
            val deviceId = "backup-device-id"
            val backupInfo = mock<BackupInfo> {
                on { rootHandle }.thenReturn(nodeId)
                on { this.deviceId }.thenReturn(deviceId)
                on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
            }

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
            whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )

            // Should find the backup, not Camera Uploads (even though nodeId=1 might match CU handle)
            val expected = FolderUsageResult.UsedBySyncOrBackup(deviceId)
            assertThat(result).isEqualTo(expected)
            verifyNoInteractions(getCameraUploadsSyncHandlesUseCase)
            verify(getBackupInfoUseCase).invoke()
        }

    @Test
    fun `test that when shouldCheckCameraUploads is false and no backups match, returns NotUsed`() =
        runTest {
            val nodeId = NodeId(99L)
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getBackupInfoUseCase()).thenReturn(emptyList())

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )

            assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
            verifyNoInteractions(getCameraUploadsSyncHandlesUseCase)
        }

    @Test
    fun `test that when shouldExcludeCurrentDevice is true, current device backup is excluded`() =
        runTest {
            val nodeId = NodeId(5L)
            val currentDeviceId = "current-device-id"
            val otherDeviceId = "other-device-id"

            val backups = listOf(
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(currentDeviceId)
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(10L))
                    on { this.deviceId }.thenReturn(otherDeviceId)
                    on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                }
            )

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
            whenever(getDeviceIdUseCase()).thenReturn(currentDeviceId)
            whenever(getBackupInfoUseCase()).thenReturn(backups)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(10L)))
                .thenReturn(NodeRelationship.NoMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true
            )

            // Should not find current device's backup, and other backup doesn't match
            assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
            // Verify getDeviceIdUseCase was called (once per backup during filtering)
            verify(getDeviceIdUseCase, atLeastOnce()).invoke()
        }

    @Test
    fun `test that when shouldExcludeCurrentDevice is false, all backups are checked`() =
        runTest {
            val nodeId = NodeId(5L)
            val currentDeviceId = "current-device-id"

            val backupInfo = mock<BackupInfo> {
                on { rootHandle }.thenReturn(nodeId)
                on { this.deviceId }.thenReturn(currentDeviceId)
                on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
            }

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
            whenever(getDeviceIdUseCase()).thenReturn(currentDeviceId)
            whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
            whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            // Should find the backup even though it's from current device
            val expected = FolderUsageResult.UsedBySyncOrBackup(currentDeviceId)
            assertThat(result).isEqualTo(expected)
            verifyNoInteractions(getDeviceIdUseCase)
        }

    @Test
    fun `test that when shouldExcludeCurrentDevice is true and other device matches, returns that backup`() =
        runTest {
            val nodeId = NodeId(5L)
            val currentDeviceId = "current-device-id"
            val otherDeviceId = "other-device-id"

            val backups = listOf(
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(currentDeviceId)
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(otherDeviceId)
                    on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                }
            )

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
            whenever(getDeviceIdUseCase()).thenReturn(currentDeviceId)
            whenever(getBackupInfoUseCase()).thenReturn(backups)
            whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true
            )

            // Should find the other device's backup
            val expected = FolderUsageResult.UsedBySyncOrBackup(otherDeviceId)
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that when both flags are false, all backups and camera uploads are checked`() =
        runTest {
            val nodeId = NodeId(2L)
            val deviceId = "test-device-id"
            val backupInfo = mock<BackupInfo> {
                on { rootHandle }.thenReturn(NodeId(10L))
                on { this.deviceId }.thenReturn(deviceId)
                on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
            }

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
            whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(1L)))
                .thenReturn(NodeRelationship.NoMatch)
            whenever(determineNodeRelationshipUseCase(nodeId, NodeId(2L)))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )

            // Should find Camera Uploads match
            assertThat(result).isEqualTo(FolderUsageResult.UsedByMediaUpload)
        }

    @Test
    fun `test that when shouldCheckCameraUploads is false and shouldExcludeCurrentDevice is true, only other device backups are checked`() =
        runTest {
            val nodeId = NodeId(5L)
            val currentDeviceId = "current-device-id"
            val otherDeviceId = "other-device-id"

            val backups = listOf(
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(currentDeviceId)
                    on { type }.thenReturn(BackupInfoType.BACKUP_UPLOAD)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(otherDeviceId)
                    on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                }
            )

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getDeviceIdUseCase()).thenReturn(currentDeviceId)
            whenever(getBackupInfoUseCase()).thenReturn(backups)
            whenever(determineNodeRelationshipUseCase(nodeId, nodeId))
                .thenReturn(NodeRelationship.ExactMatch)

            val result = underTest(
                nodeId,
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = true
            )

            // Should find the other device's backup, not Camera Uploads or current device
            val expected = FolderUsageResult.UsedBySyncOrBackup(otherDeviceId)
            assertThat(result).isEqualTo(expected)
            verifyNoInteractions(getCameraUploadsSyncHandlesUseCase)
            // Verify getDeviceIdUseCase was called (once per backup during filtering)
            verify(getDeviceIdUseCase, atLeastOnce()).invoke()
        }
}
