package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorCrossDeviceFolderConflictsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorCrossDeviceFolderConflictsUseCaseTest {

    private lateinit var underTest: MonitorCrossDeviceFolderConflictsUseCase

    private val getUploadFolderHandleUseCase = mock<GetUploadFolderHandleUseCase>()
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase =
        mock<IsFolderUsedBySyncOrBackupAcrossDevicesUseCase>()
    private val isMediaUploadsEnabledUseCase = mock<IsMediaUploadsEnabledUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorCrossDeviceFolderConflictsUseCase(
            getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase = isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getUploadFolderHandleUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            isMediaUploadsEnabledUseCase,
            getFeatureFlagValueUseCase
        )
    }

    // Feature Flag Tests

    @Test
    fun `test that when feature flag is disabled, emits null and completes`() = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Primary Folder Conflict Tests

    @Test
    fun `test that when primary folder has conflict with Sync or Backup, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedBySyncOrBackup("other-device-id")

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is used by Camera Uploads, emits null (no conflict)`() =
        runTest {
            val primaryHandle = 123456L

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(FolderUsageResult.UsedByCameraUpload)

            underTest().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is not used, emits null`() = runTest {
        val primaryHandle = 123456L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Secondary Folder Conflict Tests

    @Test
    fun `test that when secondary folder has conflict with Sync or Backup, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val secondaryHandle = 789012L
            val conflictResult = FolderUsageResult.UsedBySyncOrBackup("other-device-id")

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
                .thenReturn(secondaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(FolderUsageResult.NotUsed)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(secondaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when media uploads is disabled, secondary folder is not checked`() = runTest {
        val primaryHandle = 123456L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Exception Handling Tests

    @Test
    fun `test that when feature flag check throws exception, exception propagates`() = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenThrow(RuntimeException("Test exception"))

        underTest().test {
            awaitError() // Exception should propagate
        }
    }

    @Test
    fun `test that when checking folder usage throws exception, emits null`() = runTest {
        val primaryHandle = 123456L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenThrow(RuntimeException("Test exception"))

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Parent/Child Relationship Tests

    @Test
    fun `test that when primary folder is parent of Sync or Backup folder, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedBySyncOrBackupParent("other-device-id")

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is child of Sync or Backup folder, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedBySyncOrBackupChild("other-device-id")

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is used by Camera Uploads parent, emits null`() = runTest {
        val primaryHandle = 123456L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByCameraUploadParent)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when primary folder is used by Camera Uploads child, emits null`() = runTest {
        val primaryHandle = 123456L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByCameraUploadChild)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when primary folder is used by Media Uploads, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedByMediaUpload

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is used by Media Uploads parent, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedByMediaUploadParent

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when primary folder is used by Media Uploads child, emits conflict result`() =
        runTest {
            val primaryHandle = 123456L
            val conflictResult = FolderUsageResult.UsedByMediaUploadChild

            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                .thenReturn(primaryHandle)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    NodeId(primaryHandle),
                    shouldCheckCameraUploads = false,
                    shouldExcludeCurrentDevice = false
                )
            ).thenReturn(conflictResult)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(conflictResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when secondary folder is used by Media Uploads, emits null`() = runTest {
        val primaryHandle = 123456L
        val secondaryHandle = 789012L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(secondaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByMediaUpload)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when secondary folder is used by Media Uploads parent, emits null`() = runTest {
        val primaryHandle = 123456L
        val secondaryHandle = 789012L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(secondaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByMediaUploadParent)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when secondary folder is used by Media Uploads child, emits null`() = runTest {
        val primaryHandle = 123456L
        val secondaryHandle = 789012L

        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(true)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
            .thenReturn(primaryHandle)
        whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))
            .thenReturn(secondaryHandle)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(primaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                NodeId(secondaryHandle),
                shouldCheckCameraUploads = false,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByMediaUploadChild)

        underTest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
