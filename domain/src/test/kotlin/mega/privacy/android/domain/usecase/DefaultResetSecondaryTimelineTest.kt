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
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
class DefaultResetSecondaryTimelineTest {
    private lateinit var underTest: ResetSecondaryTimeline

    private val cameraUploadRepository = mock<CameraUploadRepository> {}

    @Before
    fun setUp() {
        underTest = DefaultResetSecondaryTimeline(cameraUploadRepository = cameraUploadRepository)
    }

    @Test
    fun `test that reset secondary timeline resets all secondary timestamps and clears all secondary sync records`() =
        runTest {
            underTest()

            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(
                0,
                SyncTimeStamp.SECONDARY_PHOTO
            )
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(
                0,
                SyncTimeStamp.SECONDARY_VIDEO
            )
            verify(cameraUploadRepository, times(1)).deleteAllSecondarySyncRecords()

            verifyNoMoreInteractions(cameraUploadRepository)
        }
}
