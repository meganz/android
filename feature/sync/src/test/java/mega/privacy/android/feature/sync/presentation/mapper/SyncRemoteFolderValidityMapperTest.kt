package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.formatter.FolderConflictMessageFormatter
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncRemoteFolderValidityMapper
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncValidityResult
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncRemoteFolderValidityMapperTest {

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase =
        mock()
    private val folderConflictMessageFormatter: FolderConflictMessageFormatter = mock()

    private lateinit var underTest: SyncRemoteFolderValidityMapper

    private val remoteFolderDisplayName = "Photos"
    private val formattedMessage =
        "\"Photos\" cloud folder is already part of Camera Uploads on this device. Choose another."

    @BeforeEach
    fun setUp() {
        Analytics.initialise(mock<AnalyticsTracker>())
        underTest = SyncRemoteFolderValidityMapper(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase = isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            folderConflictMessageFormatter = folderConflictMessageFormatter,
        )
    }

    @AfterEach
    fun resetAndTearDown() {
        Analytics.initialise(null)
        reset(
            getFeatureFlagValueUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            folderConflictMessageFormatter,
        )
    }

    @Test
    fun `test that when feature flag is disabled, returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }

    @Test
    fun `test that when feature flag check throws exception, returns ValidFolderSelected`() =
        runTest {
            val nodeId = NodeId(123L)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenThrow(RuntimeException("Test exception"))

            val result = underTest(nodeId, remoteFolderDisplayName)

            assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
        }

    @Test
    fun `test that exact match with Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByCameraUpload)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                eq(FolderUsageResult.UsedByCameraUpload),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that child of Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(isFolderUsedBySyncOrBackupAcrossDevicesUseCase(nodeId, true, true, false))
            .thenReturn(FolderUsageResult.UsedByCameraUploadChild)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that parent of Camera Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByCameraUploadParent)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that exact match with Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUpload)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that child of Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUploadChild)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that parent of Media Uploads shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedByMediaUploadParent)
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that UsedBySyncOrBackup shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackup("device-id"))
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that UsedBySyncOrBackupParent shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackupParent("device-id"))
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that UsedBySyncOrBackupChild shows correct snackbar`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.UsedBySyncOrBackupChild("device-id"))
        whenever(
            folderConflictMessageFormatter.formatFromFolderUsage(
                eq(remoteFolderDisplayName),
                eq(sharedR.string.sync_label_cloud_folder),
                any(),
            )
        ).thenReturn(formattedMessage)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isEqualTo(SyncValidityResult.ShowSnackbarMessage(formattedMessage))
    }

    @Test
    fun `test that NotUsed returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenReturn(FolderUsageResult.NotUsed)

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }

    @Test
    fun `test that exception in validation returns ValidFolderSelected`() = runTest {
        val nodeId = NodeId(123L)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = true,
                useCache = false
            )
        )
            .thenThrow(RuntimeException("Test exception"))

        val result = underTest(nodeId, remoteFolderDisplayName)

        assertThat(result).isInstanceOf(SyncValidityResult.ValidFolderSelected::class.java)
    }
}
