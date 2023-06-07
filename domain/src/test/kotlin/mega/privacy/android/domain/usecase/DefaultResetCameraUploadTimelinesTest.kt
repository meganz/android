package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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
    private val getUploadFolderHandleUseCase = mock<GetUploadFolderHandleUseCase>()
    private val resetPrimaryTimeline = mock<ResetPrimaryTimeline>()
    private val resetSecondaryTimeline = mock<ResetSecondaryTimeline>()

    @Before
    fun setup() {
        underTest = DefaultResetCameraUploadTimelines(
            cameraUploadRepository = cameraUploadRepository,
            getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
            resetPrimaryTimeline = resetPrimaryTimeline,
            resetSecondaryTimeline = resetSecondaryTimeline
        )
    }

    @Test
    fun `test that reset camera upload timelines does not update anything if handle is invalid and returns false`() =
        runTest {
            val result = underTest(invalidHandle, CameraUploadFolderType.Primary)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines does not update if primary folder handle already exists and returns false`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandleUseCase(any())).thenReturn(existingHandle)
            val result = underTest(existingHandle, CameraUploadFolderType.Primary)
            verifyNoInteractions(resetPrimaryTimeline)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines does not update if secondary folder handle already exists and returns false`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandleUseCase(any())).thenReturn(existingHandle)
            val result = underTest(existingHandle, CameraUploadFolderType.Secondary)
            verifyNoInteractions(resetSecondaryTimeline)
            assertThat(result).isFalse()
        }

    @Test
    fun `test that reset camera upload timelines updates if primary folder handle does not exist and returns true`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandleUseCase(any())).thenReturn(existingHandle)
            val result = underTest(1337L, CameraUploadFolderType.Primary)
            verify(resetPrimaryTimeline).invoke()
            assertThat(result).isTrue()
        }

    @Test
    fun `test that reset camera upload timelines updates if secondary folder handle does not exist and returns true`() =
        runTest {
            val existingHandle = 69L
            whenever(getUploadFolderHandleUseCase(any())).thenReturn(existingHandle)
            val result = underTest(1337L, CameraUploadFolderType.Secondary)
            verify(resetSecondaryTimeline).invoke()
            assertThat(result).isTrue()
        }
}
