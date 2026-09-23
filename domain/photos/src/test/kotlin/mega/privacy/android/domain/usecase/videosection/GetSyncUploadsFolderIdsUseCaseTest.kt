package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSyncUploadsFolderIdsUseCaseTest {
    private lateinit var underTest: GetSyncUploadsFolderIdsUseCase
    private val photosRepository = mock<PhotosRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetSyncUploadsFolderIdsUseCase(photosRepository = photosRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(photosRepository)
    }

    @Test
    fun `test that ids return correctly`() = runTest {
        val cameraUploadsId: Long = 1
        val mediaUploadsId: Long = 2
        whenever(photosRepository.getMediaUploadFolderId()).thenReturn(mediaUploadsId)
        whenever(photosRepository.getCameraUploadFolderId()).thenReturn(cameraUploadsId)
        val actual = underTest()
        assertThat(actual).isNotEmpty()
        assertThat(actual.size).isEqualTo(2)
        assertThat(cameraUploadsId in actual).isTrue()
        assertThat(mediaUploadsId in actual).isTrue()
    }

    @Test
    fun `test that ids return correctly when getMediaUploadFolderId is null`() = runTest {
        val cameraUploadsId: Long = 1
        val mediaUploadsId: Long? = null
        whenever(photosRepository.getMediaUploadFolderId()).thenReturn(mediaUploadsId)
        whenever(photosRepository.getCameraUploadFolderId()).thenReturn(cameraUploadsId)
        val actual = underTest()
        assertThat(actual).isNotEmpty()
        assertThat(actual.size).isEqualTo(1)
        assertThat(cameraUploadsId in actual).isTrue()
    }
}