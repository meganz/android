package mega.privacy.android.domain.usecase.imageviewer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase.Companion.FILE
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetImageUseCaseTest {

    private lateinit var underTest: GetImageUseCase

    private val thumbnailFilePath = "/thumbMEGA/abc.jpg"
    private val previewFilePath = "/previewMEGA/xyz.jpg"
    private val fullSizeFilePath = "/tempMEGA/test.jpg"

    private val isFullSizeRequiredUseCase: IsFullSizeRequiredUseCase = mock()
    private val imageNode: TypedImageNode = mock()

    private lateinit var fetchThumbnailLambda: () -> String
    private lateinit var fetchPreviewLambda: () -> String

    @BeforeAll
    fun setUp() {
        underTest =
            GetImageUseCase(isFullSizeRequiredUseCase)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun recreateMocks() {
        fetchThumbnailLambda = mock {
            onBlocking { invoke() }.thenReturn(thumbnailFilePath)
        }
        fetchPreviewLambda = mock {
            onBlocking { invoke() }.thenReturn(previewFilePath)
        }
    }


    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that imageResult isVideo is true when node type is video`() = runTest {
        whenever(imageNode.type).thenReturn(mock<VideoFileTypeInfo>())
        underTest.invoke(imageNode, false, highPriority = false, resetDownloads = {}).test {
            assertThat(awaitItem().isVideo).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that imageResult isVideo is false when node type is image`() = runTest {
        whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
        underTest.invoke(imageNode, false, highPriority = false, resetDownloads = {}).test {
            assertThat(awaitItem().isVideo).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that fetchThumbnail is invoked`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                awaitItem()
                verify(fetchThumbnailLambda).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that imageResult thumbnailUri matches value returned by fetchThumbnail`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                assertThat(awaitItem().thumbnailUri).isEqualTo("$FILE$thumbnailFilePath")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that fetchPreview is invoked`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(true)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                awaitItem()
                verify(fetchPreviewLambda).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that imageResult previewUri matches value returned by fetchPreview`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(true)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                assertThat(awaitItem().previewUri).isEqualTo("$FILE$previewFilePath")
                cancelAndIgnoreRemainingEvents()
            }
        }


    @Test
    internal fun `test that imageResult isFullyLoaded is true after fetching preview when isFullSizeRequired is false`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(false)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)
            underTest.invoke(imageNode, false, highPriority = false, resetDownloads = {}).test {
                assertThat(awaitItem().previewUri).isEqualTo("$FILE$previewFilePath")
                assertThat(awaitItem().isFullyLoaded).isEqualTo(true)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that imageResult isFullyLoaded is false after fetching preview when isFullSizeRequired is true`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(true)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                assertThat(awaitItem().previewUri).isEqualTo("$FILE$previewFilePath")
                assertThat(awaitItem().isFullyLoaded).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that fetchFullImage is invoked when isFullSizeRequired is true`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(true)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)

            val fetchFullImageLambda: (Boolean, () -> Unit) -> Flow<ImageProgress> = mock {
                onBlocking { invoke(any(), any()) }.thenReturn(flow {
                    ImageProgress.Completed(
                        fullSizeFilePath
                    )
                })
            }
            whenever(imageNode.fetchFullImage).thenReturn(fetchFullImageLambda)
            underTest.invoke(imageNode, true, highPriority = false, resetDownloads = {}).test {
                awaitItem()
                verify(fetchFullImageLambda).invoke(any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that fetchFullImage is not invoked when isFullSizeRequired is false`() =
        runTest {
            whenever(imageNode.type).thenReturn(mock<StaticImageFileTypeInfo>())
            whenever(imageNode.fetchThumbnail).thenReturn(fetchThumbnailLambda)
            whenever(isFullSizeRequiredUseCase(any(), any())).thenReturn(false)
            whenever(imageNode.fetchPreview).thenReturn(fetchPreviewLambda)

            val fetchFullImageLambda: (Boolean, () -> Unit) -> Flow<ImageProgress> = mock {
                onBlocking { invoke(any(), any()) }.thenReturn(flow {
                    ImageProgress.Completed(
                        fullSizeFilePath
                    )
                })
            }
            whenever(imageNode.fetchFullImage).thenReturn(fetchFullImageLambda)
            underTest.invoke(imageNode, false, highPriority = false, resetDownloads = {}).test {
                awaitItem()
                verify(fetchFullImageLambda, never()).invoke(any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }
}