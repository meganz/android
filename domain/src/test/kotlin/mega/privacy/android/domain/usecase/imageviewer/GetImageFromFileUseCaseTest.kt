package mega.privacy.android.domain.usecase.imageviewer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetImageFromFileUseCaseTest {
    private lateinit var underTest: GetImageFromFileUseCase

    private val isVideoFileUseCase = mock<IsVideoFileUseCase>()
    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val file = mock<File>()
    private val fileName = "testName"
    private val fileLength = 1L
    private val filePath = "../cache/$fileName"
    private val thumbPreviewFileName = "thumbPreviewTestName"
    private val previewCache = "../cache/previewsMEGA"
    private val previewPath = "$previewCache/$thumbPreviewFileName"

    @BeforeAll
    fun setUp() {
        underTest =
            GetImageFromFileUseCase(
                isVideoFileUseCase,
                thumbnailPreviewRepository,
                fileSystemRepository
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isVideoFileUseCase, thumbnailPreviewRepository, fileSystemRepository)
    }


    @Test
    fun `test that exception is thrown when file does not exist`() {
        runTest {
            whenever(file.exists()).thenReturn(false)
            whenever(file.canRead()).thenReturn(true)
            assertThrows<IllegalArgumentException> {
                underTest.invoke(
                    file = file
                )
            }
        }
    }

    @Test
    fun `test that exception is thrown when file can not be read`() {
        runTest {
            whenever(file.exists()).thenReturn(false)
            whenever(file.canRead()).thenReturn(true)
            assertThrows<IllegalArgumentException> {
                underTest.invoke(
                    file = file
                )
            }
        }
    }

    @ParameterizedTest(
        name = " isVideo = {0} and doesPreviewExist = {1}"
    )
    @MethodSource("provideParameters")
    fun `test that GetImageFromFileUseCase behaves correctly if`(
        isVideo: Boolean, doesPreviewExist: Boolean,
    ) = runTest {
        whenever(file.exists()).thenReturn(true)
        whenever(file.canRead()).thenReturn(true)
        whenever(file.absolutePath).thenReturn(filePath)
        whenever(file.name).thenReturn(fileName)
        whenever(file.length()).thenReturn(fileLength)
        whenever(isVideoFileUseCase(filePath)).thenReturn(isVideo)
        whenever(thumbnailPreviewRepository.getThumbnailOrPreviewFileName(fileName + fileLength)).thenReturn(
            thumbPreviewFileName
        )
        whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(previewCache)
        whenever(fileSystemRepository.doesFileExist(previewPath)).thenReturn(doesPreviewExist)

        underTest.invoke(file)

        verify(isVideoFileUseCase).invoke(filePath)
        verify(thumbnailPreviewRepository).getThumbnailOrPreviewFileName(fileName + fileLength)
        verify(thumbnailPreviewRepository).getPreviewCacheFolderPath()
        verify(fileSystemRepository, times(2)).doesFileExist(previewPath)

        if (!doesPreviewExist) {
            verify(thumbnailPreviewRepository).createPreview(
                fileName + fileLength,
                file
            )
        }
    }

    @ParameterizedTest(
        name = " isVideo = {0} and doesPreviewExist = {1}"
    )
    @MethodSource("provideParameters")
    fun `test that GetImageFromFileUseCase returns result correctly if`(
        isVideo: Boolean, doesPreviewExist: Boolean,
    ) = runTest {
        whenever(file.exists()).thenReturn(true)
        whenever(file.canRead()).thenReturn(true)
        whenever(file.absolutePath).thenReturn(filePath)
        whenever(file.name).thenReturn(fileName)
        whenever(file.length()).thenReturn(fileLength)
        whenever(isVideoFileUseCase(filePath)).thenReturn(isVideo)
        whenever(thumbnailPreviewRepository.getThumbnailOrPreviewFileName(fileName + fileLength)).thenReturn(
            thumbPreviewFileName
        )
        whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(previewCache)
        whenever(fileSystemRepository.doesFileExist(previewPath)).thenReturn(doesPreviewExist)

        val expected = ImageResult(
            isVideo = isVideo,
            previewUri = if (doesPreviewExist) "file://$previewPath" else null,
            fullSizeUri = "file://$filePath",
            isFullyLoaded = true
        )
        assertThat(underTest.invoke(file = file) == expected)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false),
        Arguments.of(true, false),
        Arguments.of(false, true),
        Arguments.of(true, true)
    )
}