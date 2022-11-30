package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResetCameraUploadTimelinesTest {

    private lateinit var underTest: ResetCameraUploadTimelines
    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository>() {
        onBlocking {
            getInvalidHandle()
        }.thenReturn(invalidHandle)
    }
    private val getUploadFolderHandle = mock<GetUploadFolderHandle>()
    private val resetPrimaryTimeline = mock<ResetPrimaryTimeline>()
    private val resetSecondaryTimeline = mock<ResetSecondaryTimeline>()

    @Before
    fun setup() {
        underTest = DefaultResetCameraUploadTimelines(
            cameraUploadRepository = cameraUploadRepository,
            getUploadFolderHandle = getUploadFolderHandle,
            resetPrimaryTimeline = resetPrimaryTimeline,
            resetSecondaryTimeline = resetSecondaryTimeline
        )
    }

    @Test
    fun `test that reset camera upload timelines does not update anything if handle is invalid and returns false`() =
        runTest {
            val result = underTest(invalidHandle, false)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines does not update if primary folder handle already exists and returns false`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandle(any())).thenReturn(existingHandle)
            val result = underTest(existingHandle, false)
            verifyNoInteractions(resetPrimaryTimeline)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines does not update if secondary folder handle already exists and returns false`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandle(any())).thenReturn(existingHandle)
            val result = underTest(existingHandle, true)
            verifyNoInteractions(resetSecondaryTimeline)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines updates if primary folder handle does not exist and returns true`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandle(any())).thenReturn(existingHandle)
            val result = underTest(1337L, false)
            verify(cameraUploadRepository, times(1)).setPrimaryFolderHandle(any())
            verify(resetPrimaryTimeline, times(1)).invoke()
            assertThat(result).isTrue()
        }

    @Test
    fun `test that reset camera upload timelines updates if secondary folder handle does not exist and returns true`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandle(any())).thenReturn(existingHandle)
            val result = underTest(1337L, true)
            verify(cameraUploadRepository, times(1)).setSecondaryFolderHandle(any())
            verify(resetSecondaryTimeline, times(1)).invoke()
            assertThat(result).isTrue()
        }
}
