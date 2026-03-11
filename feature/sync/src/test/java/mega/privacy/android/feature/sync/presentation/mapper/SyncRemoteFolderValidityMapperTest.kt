package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncRemoteFolderValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncValidityResult
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncRemoteFolderValidityMapperTest {

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase =
        mock()

    private lateinit var underTest: SyncRemoteFolderValidityMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncRemoteFolderValidityMapper(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase = isFolderUsedBySyncOrBackupAcrossDevicesUseCase
        )
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(getFeatureFlagValueUseCase, isFolderUsedBySyncOrBackupAcrossDevicesUseCase)
    }

    // Feature Flag Tests

    @Test
    fun `test that when feature flag is disabled, returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }

    @Test
    fun `test that when feature flag check throws exception, returns ValidFolderSelected`() =
        runTest {
            val nodeId = NodeId(123L)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenThrow(RuntimeException("Test exception"))

            val result = underTest(nodeId)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        }

    // Camera Uploads Conflict Tests

    @Test
    fun `test that exact match with Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByCameraUpload)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_camera_uploads)
    }

    @Test
    fun `test that child of Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(isFolderUsedBySyncOrBackupAcrossDevicesUseCase(nodeId, true, true, false))
            .thenReturn(FolderUsageResult.UsedByCameraUploadChild)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_camera_uploads)
    }

    @Test
    fun `test that parent of Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByCameraUploadParent)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_camera_uploads)
    }

    // Media Uploads Conflict Tests

    @Test
    fun `test that exact match with Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUpload)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_media_uploads)
    }

    @Test
    fun `test that child of Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUploadChild)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_media_uploads)
    }

    @Test
    fun `test that parent of Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUploadParent)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_media_uploads)
    }

    // Sync/Backup Conflict Tests (cross-device)

    @Test
    fun `test that UsedBySyncOrBackup shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackup("device-id"))

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_sync_or_backup)
    }

    @Test
    fun `test that UsedBySyncOrBackupParent shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackupParent("device-id"))

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_sync_or_backup)
    }

    @Test
    fun `test that UsedBySyncOrBackupChild shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackupChild("device-id"))

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ShowSnackbar::class.java)
        val snackbarResult = result as SyncValidityResult.ShowSnackbar
        assertThat(snackbarResult.messageResId)
            .isEqualTo(sharedR.string.error_folder_part_of_sync_or_backup)
    }

    // No Conflict Tests

    @Test
    fun `test that NotUsed returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.NotUsed)

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }

    // Exception Handling

    @Test
    fun `test that exception in validation returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenThrow(RuntimeException("Test exception"))

        val result = underTest(nodeId)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }
}
