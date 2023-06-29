package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AreCameraUploadsFolderInRubbishBinUseCaseTest {
    lateinit var underTest: AreCameraUploadsFoldersInRubbishBinUseCase

    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val backupTimeStampsAndFolderHandle = mock<BackupTimeStampsAndFolderHandle>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val clearSyncRecords = mock<ClearSyncRecords>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val disableCameraUploadsSettingsUseCase = mock<DisableCameraUploadsSettingsUseCase>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()

    @BeforeAll
    fun setUp() {
        underTest = AreCameraUploadsFoldersInRubbishBinUseCase(
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isNodeInRubbish = isNodeInRubbish,
            backupTimeStampsAndFolderHandle = backupTimeStampsAndFolderHandle,
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            clearCacheDirectory = clearCacheDirectory,
            clearSyncRecords = clearSyncRecords,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            disableCameraUploadsSettingsUseCase = disableCameraUploadsSettingsUseCase,
            disableMediaUploadSettings = disableMediaUploadSettings,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isSecondaryFolderEnabled,
            isNodeInRubbish,
            backupTimeStampsAndFolderHandle,
            resetCameraUploadTimeStamps,
            clearCacheDirectory,
            clearSyncRecords,
            resetMediaUploadTimeStamps,
            disableCameraUploadsSettingsUseCase,
            disableMediaUploadSettings,
        )
    }

    @Test
    fun `test that when primary folder is in rubbish upload job should be stopped`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(false)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(true)
            val expected = underTest(false, primaryHandle, secondaryHandle)
            assertThat(true).isEqualTo(expected.shouldStopProcess)
        }

    @Test
    fun `test that when secondary folder is in rubbish upload job should be stopped`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(true)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(true)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            val expected = underTest(false, primaryHandle, secondaryHandle)
            assertThat(true).isEqualTo(expected.shouldStopProcess)
        }

    @Test
    fun `test that time stamps and folder handles are backed up when camera upload should be disabled and secondary folder is in rubbish`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(true)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(true)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            underTest(true, primaryHandle, secondaryHandle)
            verify(backupTimeStampsAndFolderHandle).invoke()
        }

    @Test
    fun `test that time stamps and folder handles are backed up when camera upload should be disabled and primary folder is in rubbish`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(false)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(false)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(true)
            underTest(true, primaryHandle, secondaryHandle)
            verify(backupTimeStampsAndFolderHandle).invoke()
        }

    @Test
    fun `test that media upload is disabled when camera upload should be disabled and secondary folder is in rubbish`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(true)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(true)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            val expected = underTest(true, primaryHandle, secondaryHandle)
            assertThat(true).isEqualTo(expected.shouldStopProcess)
            assertThat(false).isEqualTo(expected.shouldSendEvent)
            verify(resetMediaUploadTimeStamps).invoke()
            verify(disableMediaUploadSettings).invoke()
        }

    @Test
    fun `test that both camera upload and media upload is disabled when camera upload should be disabled and primary folder is in rubbish`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(false)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(false)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(true)
            val expected = underTest(true, primaryHandle, secondaryHandle)
            assertThat(true).isEqualTo(expected.shouldStopProcess)
            assertThat(true).isEqualTo(expected.shouldSendEvent)
            verify(resetCameraUploadTimeStamps).invoke(false)
            verify(clearCacheDirectory).invoke()
            verify(disableCameraUploadsSettingsUseCase).invoke()
            verify(clearSyncRecords).invoke()
        }
}
