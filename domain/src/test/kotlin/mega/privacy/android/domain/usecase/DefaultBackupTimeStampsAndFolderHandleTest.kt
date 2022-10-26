package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBackupTimeStampsAndFolderHandleTest {
    private lateinit var underTest: BackupTimeStampsAndFolderHandle

    private val primaryHandle = 1L
    private val secondaryHandle = 2L
    private val timestamps = "123456"

    private val cameraUploadRepository = mock<CameraUploadRepository> {
        onBlocking {
            getPrimarySyncHandle()
        }.thenReturn(primaryHandle)
        onBlocking {
            getSecondarySyncHandle()
        }.thenReturn(secondaryHandle)
        onBlocking {
            getSyncTimeStamp(SyncTimeStamp.PRIMARY_PHOTO)
        }.thenReturn(timestamps)
        onBlocking {
            getSyncTimeStamp(SyncTimeStamp.PRIMARY_VIDEO)
        }.thenReturn(timestamps)
        onBlocking {
            getSyncTimeStamp(SyncTimeStamp.SECONDARY_PHOTO)
        }.thenReturn(timestamps)
        onBlocking {
            getSyncTimeStamp(SyncTimeStamp.SECONDARY_VIDEO)
        }.thenReturn(timestamps)
    }

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultBackupTimeStampsAndFolderHandle(
            cameraUploadRepository = cameraUploadRepository,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `test that time stamps are backed up when invoked`() =
        runTest {
            underTest()
            verify(settingsRepository, times(1)).backupTimestampsAndFolderHandle(primaryHandle,
                secondaryHandle,
                timestamps,
                timestamps,
                timestamps,
                timestamps)
        }

    @Test
    fun `test that invalid handles are backed up when existing handles are null`() =
        runTest {
            val invalidHandle = -1L
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(null)
            whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(null)
            underTest()
            verify(settingsRepository, times(1)).backupTimestampsAndFolderHandle(
                invalidHandle,
                invalidHandle,
                timestamps,
                timestamps,
                timestamps,
                timestamps,
            )
        }
}
