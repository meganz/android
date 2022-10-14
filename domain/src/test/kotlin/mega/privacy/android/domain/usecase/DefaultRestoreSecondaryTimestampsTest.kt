package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRestoreSecondaryTimestampsTest {
    private lateinit var underTest: RestoreSecondaryTimestamps

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val settingsRepository = mock<SettingsRepository>()


    @Before
    fun setUp() {
        underTest = DefaultRestoreSecondaryTimestamps(
            cameraUploadRepository = cameraUploadRepository,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `test that secondary camera sync records are cleared`() =
        runTest {
            underTest()
            verify(settingsRepository, times(1)).clearSecondaryCameraSyncRecords()
        }

    @Test
    fun `test time stamp is not restored when secondary folder handles don't match`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 87654321L
            whenever(settingsRepository.getSecondaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(detectedHandle)
            underTest()
            verify(cameraUploadRepository, times(0)).setSyncTimeStamp(any(), any())
        }

    @Test
    fun `test time stamps are not restored when timestamps are empty or null`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 87654321L
            whenever(settingsRepository.getSecondaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(detectedHandle)
            whenever(settingsRepository.getSecondaryHandle()).thenReturn(null)
            whenever(settingsRepository.getSecondaryFolderVideoSyncTime()).thenReturn("")
            underTest()
            verify(cameraUploadRepository, times(0)).setSyncTimeStamp(any(), any())
        }

    @Test
    fun `test time stamps are restored when timestamps are not empty or null`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 12345678L
            whenever(settingsRepository.getSecondaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(detectedHandle)
            val photoTimestamp = "12345678"
            val videoTimestamp = "12345678"
            whenever(settingsRepository.getSecondaryFolderPhotoSyncTime()).thenReturn(photoTimestamp)
            whenever(settingsRepository.getSecondaryFolderVideoSyncTime()).thenReturn(videoTimestamp)
            underTest()
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(photoTimestamp.toLong(),
                SyncTimeStamp.SECONDARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(videoTimestamp.toLong(),
                SyncTimeStamp.SECONDARY_VIDEO)
        }
}
