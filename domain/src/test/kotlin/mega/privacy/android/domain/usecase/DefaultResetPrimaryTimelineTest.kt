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
class DefaultResetPrimaryTimelineTest {
    private lateinit var underTest: ResetPrimaryTimeline

    private val cameraUploadRepository = mock<CameraUploadRepository> {}

    @Before
    fun setUp() {
        underTest = DefaultResetPrimaryTimeline(cameraUploadRepository = cameraUploadRepository)
    }

    @Test
    fun `test that reset primary timeline resets all primary timestamps and clears all primary sync records`() =
        runTest {
            underTest()

            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(
                0,
                SyncTimeStamp.PRIMARY_PHOTO
            )
            verify(cameraUploadRepository, times(1)).setSyncTimeStamp(
                0,
                SyncTimeStamp.PRIMARY_VIDEO
            )
            verify(cameraUploadRepository, times(1)).deleteAllPrimarySyncRecords()

            verifyNoMoreInteractions(cameraUploadRepository)
        }
}
