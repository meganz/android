package mega.privacy.android.domain.usecase.imageviewer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
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
class GetImageByOfflineNodeHandleUseCaseTest {
    private lateinit var underTest: GetImageByOfflineNodeHandleUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val isVideoFileUseCase = mock<IsVideoFileUseCase>()
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val offlineNodeInformation = mock<IncomingShareOfflineNodeInformation>()

    private val nodeHandle = 1L
    private val offlineNodeHandle = "2"
    private val file = mock<File>()
    private val fileName = "test"
    private val filePath = "../files/$fileName"
    private val thumbPreviewFileName = "thumbPreviewTestName"
    private val previewCache = "../cache/previewsMEGA"
    private val previewPath = "$previewCache/$thumbPreviewFileName"
    private val thumbnailCache = "../cache/thumbnailsMEGA"
    private val thumbnailPath = "$thumbnailCache/$thumbPreviewFileName"


    @BeforeAll
    fun setUp() {
        underTest =
            GetImageByOfflineNodeHandleUseCase(
                nodeRepository,
                getOfflineFileUseCase,
                isVideoFileUseCase,
                thumbnailPreviewRepository,
                fileSystemRepository,
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getOfflineFileUseCase,
            isVideoFileUseCase,
            thumbnailPreviewRepository,
            fileSystemRepository
        )
    }

    @Test
    fun `test that exception is thrown when offline node information returned is null`() {
        runTest {
            whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)).thenReturn(null)

            assertThrows<IllegalArgumentException> {
                underTest.invoke(
                    nodeHandle = nodeHandle,
                )
            }
        }
    }

    @Test
    fun `test that exception is thrown when offline file does not exist`() {
        runTest {
            val file = mock<File> {
                on { exists() }.thenReturn(false)
            }
            whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)).thenReturn(
                offlineNodeInformation
            )
            whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(file)
            assertThrows<IllegalArgumentException> {
                underTest.invoke(
                    nodeHandle = nodeHandle,
                )
            }
        }
    }


    @ParameterizedTest(
        name = " isVideo = {0}, doesPreviewExist = {1} and doesThumbnailExist = {2}"
    )
    @MethodSource("provideParameters")
    fun `test that GetImageByOfflineNodeHandleUseCase behaves correctly if`(
        isVideo: Boolean, doesPreviewExist: Boolean, doesThumbnailExist: Boolean,
    ) = runTest {
        whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)).thenReturn(
            offlineNodeInformation
        )
        whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(file)
        whenever(file.exists()).thenReturn(true)
        whenever(file.absolutePath).thenReturn(filePath)
        whenever(isVideoFileUseCase(filePath)).thenReturn(isVideo)
        whenever(offlineNodeInformation.handle).thenReturn(offlineNodeHandle)
        whenever(thumbnailPreviewRepository.getThumbnailOrPreviewFileName(offlineNodeHandle.toLong())).thenReturn(
            thumbPreviewFileName
        )
        whenever(thumbnailPreviewRepository.getThumbnailCacheFolderPath()).thenReturn(thumbnailCache)
        whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(previewCache)
        whenever(fileSystemRepository.doesFileExist(previewPath)).thenReturn(doesPreviewExist)
        whenever(fileSystemRepository.doesFileExist(thumbnailPath)).thenReturn(doesThumbnailExist)

        underTest.invoke(
            nodeHandle = nodeHandle,
        )

        verify(isVideoFileUseCase).invoke(filePath)
        verify(thumbnailPreviewRepository).getThumbnailOrPreviewFileName(offlineNodeHandle.toLong())
        verify(thumbnailPreviewRepository).getThumbnailCacheFolderPath()
        verify(thumbnailPreviewRepository).getPreviewCacheFolderPath()
        verify(fileSystemRepository).doesFileExist(thumbnailPath)
        verify(fileSystemRepository, times(2)).doesFileExist(previewPath)

        if (!doesPreviewExist) {
            verify(thumbnailPreviewRepository).createPreview(
                offlineNodeHandle.toLong(),
                file
            )
        }
    }

    @ParameterizedTest(
        name = " isVideo = {0}, doesPreviewExist = {1} and doesThumbnailExist = {2}"
    )
    @MethodSource("provideParameters")
    fun `test that GetImageByOfflineNodeHandleUseCase returns result correctly if`(
        isVideo: Boolean, doesPreviewExist: Boolean, doesThumbnailExist: Boolean,
    ) = runTest {
        whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)).thenReturn(
            offlineNodeInformation
        )
        whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(file)
        whenever(file.exists()).thenReturn(true)
        whenever(file.absolutePath).thenReturn(filePath)
        whenever(isVideoFileUseCase(filePath)).thenReturn(isVideo)
        whenever(offlineNodeInformation.handle).thenReturn(offlineNodeHandle)
        whenever(thumbnailPreviewRepository.getThumbnailOrPreviewFileName(offlineNodeHandle.toLong())).thenReturn(
            thumbPreviewFileName
        )
        whenever(thumbnailPreviewRepository.getThumbnailCacheFolderPath()).thenReturn(thumbnailCache)
        whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(previewCache)
        whenever(fileSystemRepository.doesFileExist(previewPath)).thenReturn(doesPreviewExist)
        whenever(fileSystemRepository.doesFileExist(thumbnailPath)).thenReturn(doesThumbnailExist)

        val expected = ImageResult(
            isVideo = isVideo,
            thumbnailUri = if (doesThumbnailExist) "file://$thumbnailPath" else null,
            previewUri = if (doesPreviewExist) "file://$previewPath" else null,
            fullSizeUri = "file://$filePath",
            isFullyLoaded = true
        )
        assertThat(underTest.invoke(nodeHandle = nodeHandle) == expected)
    }

    @ParameterizedTest(
        name = " isVideo = {0}"
    )
    @MethodSource("provideParameters")
    fun `test that GetImageByOfflineNodeHandleUseCase returns result by skipping preview generation for invalid handle with`(
        isVideo: Boolean
    ) = runTest {
        whenever(nodeRepository.getOfflineNodeInformation(nodeHandle)).thenReturn(
            offlineNodeInformation
        )
        whenever(getOfflineFileUseCase(offlineNodeInformation)).thenReturn(file)
        whenever(file.exists()).thenReturn(true)
        whenever(file.absolutePath).thenReturn(filePath)
        whenever(isVideoFileUseCase(filePath)).thenReturn(isVideo)
        whenever(offlineNodeInformation.handle).thenReturn("invalidHandle")


        val expected = ImageResult(
            isVideo = isVideo,
            fullSizeUri = "file://$filePath",
            isFullyLoaded = true
        )
        assertThat(underTest.invoke(nodeHandle = nodeHandle) == expected)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(false, false, true),
        Arguments.of(true, false, false),
        Arguments.of(true, false, true),
        Arguments.of(false, true, false),
        Arguments.of(false, true, true),
        Arguments.of(true, true, false),
        Arguments.of(true, true, true)
    )
}
