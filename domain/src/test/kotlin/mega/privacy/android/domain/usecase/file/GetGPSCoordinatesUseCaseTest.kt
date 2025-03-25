package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetGPSCoordinatesUseCaseTest {

    private lateinit var underTest: GetGPSCoordinatesUseCase

    private lateinit var fileSystemRepository: FileSystemRepository

    private val isVideoFileUseCase = mock<IsVideoFileUseCase>()
    private val isImageFileUseCase = mock<IsImageFileUseCase>()

    @BeforeAll
    fun setUp() {
        fileSystemRepository = mock()
        underTest = GetGPSCoordinatesUseCase(
            fileSystemRepository,
            isVideoFileUseCase,
            isImageFileUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            isVideoFileUseCase,
            isImageFileUseCase,
        )
    }

    @Test
    fun `test that the GPS coordinates of file type video are retrieved`() =
        runTest {
            val result = Pair(6.0, 9.0)
            whenever(fileSystemRepository.getVideoGPSCoordinates(anyValueClass())).thenReturn(result)
            Truth.assertThat(underTest(UriPath("foo"), true)).isEqualTo(result)
            verifyNoInteractions(isVideoFileUseCase)
            verifyNoInteractions(isImageFileUseCase)
        }

    @Test
    fun `test that the GPS coordinates of file type photo are retrieved`() =
        runTest {
            val result = Pair(6.0, 9.0)
            whenever(fileSystemRepository.getPhotoGPSCoordinates(anyValueClass())).thenReturn(result)
            Truth.assertThat(underTest(UriPath("foo"), false)).isEqualTo(result)
            verifyNoInteractions(isVideoFileUseCase)
            verifyNoInteractions(isImageFileUseCase)
        }

    @Test
    fun `test that the GPS coordinates of undetermined photo file are retrieved`() =
        runTest {
            val result = Pair(6.0, 9.0)
            val uriPath = UriPath("foo")
            whenever(fileSystemRepository.getPhotoGPSCoordinates(uriPath)) doReturn (result)
            whenever(isVideoFileUseCase(uriPath)) doReturn false
            whenever(isImageFileUseCase(uriPath)) doReturn true
            Truth.assertThat(underTest(uriPath, null)).isEqualTo(result)
        }

    @Test
    fun `test that the GPS coordinates of undetermined video file are retrieved`() =
        runTest {
            val result = Pair(6.0, 9.0)
            val uriPath = UriPath("foo")
            whenever(fileSystemRepository.getVideoGPSCoordinates(uriPath)) doReturn (result)
            whenever(isVideoFileUseCase(uriPath)) doReturn true
            whenever(isImageFileUseCase(uriPath)) doReturn false
            Truth.assertThat(underTest(uriPath, null)).isEqualTo(result)
        }

    @Test
    fun `test that null is returned when undetermined file is not a video or photo`() =
        runTest {
            val uriPath = UriPath("foo")
            whenever(isVideoFileUseCase(uriPath)) doReturn false
            whenever(isImageFileUseCase(uriPath)) doReturn false
            Truth.assertThat(underTest(uriPath, null)).isNull()
            verifyNoInteractions(fileSystemRepository)
        }
}
