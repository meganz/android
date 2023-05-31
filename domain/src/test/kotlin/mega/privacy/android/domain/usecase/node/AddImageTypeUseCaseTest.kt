package mega.privacy.android.domain.usecase.node

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.usecase.filenode.IsValidNodeFileUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddImageTypeUseCaseTest {

    private lateinit var underTest: AddImageTypeUseCase

    private val imageRepository: ImageRepository = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val isValidNodeFileUseCase: IsValidNodeFileUseCase = mock()

    private val imageNode = mock<ImageNode>()

    @BeforeAll
    fun setUp() {
        underTest =
            AddImageTypeUseCase(imageRepository, fileSystemRepository, isValidNodeFileUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(
        imageRepository,
        fileSystemRepository,
        isValidNodeFileUseCase,
        imageNode,
    )

    @Nested
    inner class Preview {
        private lateinit var downloadPreviewLambda: (String) -> String

        @BeforeEach
        fun recreateMocks() {
            downloadPreviewLambda = mock()
            whenever(imageNode.type).thenReturn(RawFileTypeInfo("jpg", "jpg"))
            whenever(imageNode.downloadPreview).thenReturn(downloadPreviewLambda)
        }

        @Test
        fun `test that if the file exists download preview is not triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
            val result = underTest(imageNode)
            result.fetchPreview()
            // as the file already exists, download lambda is not needed
            verify(downloadPreviewLambda, never()).invoke(any())
        }

        @Test
        fun `test that if the file doesn't exists download preview is triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
            val result = underTest(imageNode)
            result.fetchPreview()
            // as the file doesn't exist, download is needed
            verify(downloadPreviewLambda).invoke(any())
        }

        @Test
        fun `test that if the file is downloaded then download preview is not triggered again`() =
            runTest {
                whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
                val result = underTest(imageNode)
                result.fetchPreview()
                //now we can assume the file is downloaded, so it exists.
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
                result.fetchPreview()
                // lambda should be triggered only once, the second will return the already downloaded file
                verify(downloadPreviewLambda).invoke(any())
            }
    }

    @Nested
    inner class Thumbnail {

        private lateinit var downloadThumbnailLambda: (String) -> String

        @BeforeEach
        fun recreateMocks() {
            downloadThumbnailLambda = mock()
            whenever(imageNode.type).thenReturn(RawFileTypeInfo("jpg", "jpg"))
            whenever(imageNode.downloadThumbnail).thenReturn(downloadThumbnailLambda)
        }

        @Test
        fun `test that if the file exists download thumbnail is not triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
            val result = underTest(imageNode)
            result.fetchThumbnail()
            // as the file already exists, download lambda is not needed
            verify(downloadThumbnailLambda, never()).invoke(any())
        }

        @Test
        fun `test that if the file doesn't exists download thumbnail is triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
            val result = underTest(imageNode)
            result.fetchThumbnail()
            // as the file doesn't exist, download is needed
            verify(downloadThumbnailLambda).invoke(any())
        }

        @Test
        fun `test that if the file is downloaded then download thumbnail is not triggered again`() =
            runTest {
                whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
                val result = underTest(imageNode)
                result.fetchThumbnail()
                //now we can assume the file is downloaded, so it exists.
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
                result.fetchThumbnail()
                // lambda should be triggered only once, the second will return the already downloaded file
                verify(downloadThumbnailLambda).invoke(any())
            }
    }

    @Nested
    inner class FullImage {

        private lateinit var downloadFullImageLambda: (String, Boolean, () -> Unit) -> Flow<ImageProgress>

        @BeforeEach
        fun recreateMocks() {
            downloadFullImageLambda = mock()
            whenever(imageNode.type).thenReturn(RawFileTypeInfo("jpg", "jpg"))
            whenever(imageNode.downloadFullImage).thenReturn(downloadFullImageLambda)
        }

        @Test
        fun `test that if the file exists download full image is not triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
            val result = underTest(imageNode)
            result.fetchFullImage(false) {}
            // as the file already exists, download lambda is not needed
            verify(downloadFullImageLambda, never()).invoke(any(), any(), any())
        }

        @Test
        fun `test that if the file doesn't exists download full image is triggered`() = runTest {
            whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
            val result = underTest(imageNode)
            result.fetchFullImage(false) {}.test {
                // as the file doesn't exist, download is needed
                verify(downloadFullImageLambda).invoke(any(), any(), any())
                cancelAndIgnoreRemainingEvents()
            }

        }

        @Test
        fun `test that if the file is downloaded then download full image is not triggered again`() =
            runTest {
                whenever(isValidNodeFileUseCase(any(), any())).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(false)
                val result = underTest(imageNode)
                result.fetchFullImage(false) {}.test {
                    // as the file doesn't exist, download is needed
                    verify(downloadFullImageLambda).invoke(any(), any(), any())
                    cancelAndIgnoreRemainingEvents()
                }
                //now we can assume the file is downloaded, so it exists.
                whenever(fileSystemRepository.doesFileExist(any())).thenReturn(true)
                result.fetchFullImage(false) {}.test {
                    // lambda should be triggered only once, the second will return the already downloaded file
                    verify(downloadFullImageLambda).invoke(any(), any(), any())
                    cancelAndIgnoreRemainingEvents()
                }
            }
    }
}