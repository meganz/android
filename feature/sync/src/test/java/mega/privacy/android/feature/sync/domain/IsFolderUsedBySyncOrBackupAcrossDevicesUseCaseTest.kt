package mega.privacy.android.feature.sync.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderUsageResult
import mega.privacy.android.feature.sync.domain.usecase.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsFolderUsedBySyncOrBackupAcrossDevicesUseCaseTest {

    private val getBackupInfoUseCase: GetBackupInfoUseCase = mock()
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase = mock()

    private val underTest = IsFolderUsedBySyncOrBackupAcrossDevicesUseCase(
        getBackupInfoUseCase,
        getCameraUploadsSyncHandlesUseCase
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            getBackupInfoUseCase,
            getCameraUploadsSyncHandlesUseCase,
        )
    }

    @Test
    fun `test that if folder is used by primary Camera Uploads then result is UsedByCameraUpload`() =
        runTest {
            val nodeId = NodeId(1L)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUpload)
            verifyNoInteractions(getBackupInfoUseCase)
        }

    @Test
    fun `test that if folder is used by secondary Camera Uploads then result is UsedByCameraUpload`() =
        runTest {
            val nodeId = NodeId(2L)
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(FolderUsageResult.UsedByCameraUpload)
            verifyNoInteractions(getBackupInfoUseCase)
        }

    @Test
    fun `test that if Camera Uploads is not set up then backup info is checked`() = runTest {
        val nodeId = NodeId(3L)
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
        whenever(getBackupInfoUseCase()).thenReturn(emptyList())

        val result = underTest(nodeId)

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
        }
        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L) // Doesn't match
        whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))

        val result = underTest(nodeId)

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
                },
                mock<BackupInfo> { // The matching backup
                    on { rootHandle }.thenReturn(nodeId)
                    on { this.deviceId }.thenReturn(deviceId)
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(7L))
                    on { this.deviceId }.thenReturn("other-device-2")
                }
            )
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)
            whenever(getBackupInfoUseCase()).thenReturn(backups)

            val result = underTest(nodeId)

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
                },
                mock<BackupInfo> {
                    on { rootHandle }.thenReturn(NodeId(7L))
                    on { deviceId }.thenReturn("other-device-2")
                }
            )
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(1L to 2L)
            whenever(getBackupInfoUseCase()).thenReturn(backups)

            val result = underTest(nodeId)

            assertThat(result).isEqualTo(FolderUsageResult.NotUsed)
        }
}
