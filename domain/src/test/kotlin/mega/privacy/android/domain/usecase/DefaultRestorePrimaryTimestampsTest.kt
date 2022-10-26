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
class DefaultRestorePrimaryTimestampsTest {
    private lateinit var underTest: RestorePrimaryTimestamps

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val settingsRepository = mock<SettingsRepository>()


    @Before
    fun setUp() {
        underTest = DefaultRestorePrimaryTimestamps(
            cameraUploadRepository = cameraUploadRepository,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `test that primary camera sync records are cleared`() =
        runTest {
            underTest()
            verify(settingsRepository, times(1)).clearPrimaryCameraSyncRecords()
        }

    @Test
    fun `test time stamp is not restored when primary folder handles don't match`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 87654321L
            whenever(settingsRepository.getPrimaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(detectedHandle)
            underTest()
            verify(cameraUploadRepository, times(0)).setSyncTimeStamp(any(), any())
        }

    @Test
    fun `test time stamps are not restored when timestamps are empty or null`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 87654321L
            whenever(settingsRepository.getPrimaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(detectedHandle)
            whenever(settingsRepository.getPrimaryFolderPhotoSyncTime()).thenReturn(null)
            whenever(settingsRepository.getPrimaryFolderVideoSyncTime()).thenReturn("")
            underTest()
            verify(cameraUploadRepository, times(0)).setSyncTimeStamp(any(), any())
        }

    @Test
    fun `test time stamps are restored when timestamps are not empty or null`() =
        runTest {
            val backedUpHandle = 12345678L
            val detectedHandle = 12345678L
            whenever(settingsRepository.getPrimaryHandle()).thenReturn(backedUpHandle)
            whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(detectedHandle)
            val photoTimestamp = "12345678"
            val videoTimestamp = "12345678"
            whenever(settingsRepository.getPrimaryFolderPhotoSyncTime()).thenReturn(photoTimestamp)
            whenever(settingsRepository.getPrimaryFolderVideoSyncTime()).thenReturn(videoTimestamp)
            underTest()
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(photoTimestamp.toLong(),
                SyncTimeStamp.PRIMARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(videoTimestamp.toLong(),
                SyncTimeStamp.PRIMARY_VIDEO)
        }
}