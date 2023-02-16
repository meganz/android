package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
internal class DefaultCheckCameraUploadTest {
    lateinit var underTest: CheckCameraUpload

    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val backupTimeStampsAndFolderHandle = mock<BackupTimeStampsAndFolderHandle>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val clearSyncRecords = mock<ClearSyncRecords>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val disableCameraUploadSettings = mock<DisableCameraUploadSettings>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()

    @Before
    fun setUp() {
        underTest = DefaultCheckCameraUpload(
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isNodeInRubbish = isNodeInRubbish,
            backupTimeStampsAndFolderHandle = backupTimeStampsAndFolderHandle,
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            clearCacheDirectory = clearCacheDirectory,
            clearSyncRecords = clearSyncRecords,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            disableCameraUploadSettings = disableCameraUploadSettings,
            disableMediaUploadSettings = disableMediaUploadSettings,
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
            verify(backupTimeStampsAndFolderHandle, times(1)).invoke()
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
            verify(backupTimeStampsAndFolderHandle, times(1)).invoke()
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
            verify(resetMediaUploadTimeStamps, times(1)).invoke()
            verify(disableMediaUploadSettings, times(1)).invoke()
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
            verify(resetCameraUploadTimeStamps, times(1)).invoke(false)
            verify(clearCacheDirectory, times(1)).invoke()
            verify(disableCameraUploadSettings, times(1)).invoke()
            verify(clearSyncRecords, times(1)).invoke()
        }
}