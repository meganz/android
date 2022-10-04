package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResetCameraUploadTimeStampsTest {
    private lateinit var underTest: ResetCameraUploadTimeStamps

    private val cameraUploadRepository = mock<CameraUploadRepository>()


    @Before
    fun setUp() {
        underTest = DefaultResetCameraUploadTimeStamps(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that invoke with true clear camera sync records`() =
        runTest {
            underTest(true)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.PRIMARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.PRIMARY_VIDEO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.SECONDARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.SECONDARY_VIDEO)
            verify(cameraUploadRepository, times(1)).saveShouldClearCamSyncRecords(true)
        }


    @Test
    fun `test that invoke with false clear camera sync records`() =
        runTest {
            underTest(false)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.PRIMARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.PRIMARY_VIDEO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.SECONDARY_PHOTO)
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(0,
                SyncTimeStamp.SECONDARY_VIDEO)
            verify(cameraUploadRepository, times(1)).saveShouldClearCamSyncRecords(false)
        }
}