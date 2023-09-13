package mega.privacy.android.data.repository.thumbnailpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThumbnailPreviewRepositoryImplTest {

    private lateinit var underTest: ThumbnailPreviewRepository

    private val megaApi = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val stringWrapper = mock<StringWrapper>()

    private val cacheDir = File("cache")
    private val thumbnailName = "thumbnailName"
    private val thumbnailPath =
        "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
    private val thumbnailFile = File(thumbnailPath)
    private val nodeHandle = 123L
    private val megaNode = mock<MegaNode>()

    @BeforeAll
    fun setUp() {
        underTest = ThumbnailPreviewRepositoryImpl(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
            stringWrapper = stringWrapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(megaApi, megaApiFolder, cacheGateway)
    }

    @Test
    fun `test that get thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaNode.hasThumbnail()).thenReturn(true)
            whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnailFile)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApi.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getThumbnailFromServer(nodeHandle)
            assertThat(actual?.path).isEqualTo(thumbnailPath)
        }
    }

    @Test
    fun `test that get thumbnail from server doesn't returns successfully`() {
        runTest {
            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaNode.hasThumbnail()).thenReturn(true)
            whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnailFile)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(megaApi.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<MegaException> {
                underTest.getThumbnailFromServer(nodeHandle)
            }
        }
    }

    @Test
    fun `test that get public node thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiFolder.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnailFile)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiFolder.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getPublicNodeThumbnailFromServer(nodeHandle)
            assertThat(actual?.path).isEqualTo(thumbnailPath)
        }
    }

    @Test
    fun `test that get public node thumbnail from server returns doesn't successfully`() {
        runTest {
            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiFolder.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnailFile)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(megaApiFolder.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<MegaException> {
                underTest.getPublicNodeThumbnailFromServer(nodeHandle)
            }
        }
    }

    @Test
    fun `test that get public node thumbnail from local returns not successful`() {
        runTest {
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(null)

            val actual = underTest.getPublicNodeThumbnailFromLocal(nodeHandle)
            assertThat(actual?.path).isEqualTo(null)
        }
    }

    @Test
    fun `test that get public node thumbnail from local returns successfully if no error is thrown`() {
        runTest {
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail: File = mock()

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(thumbnail.exists()).thenReturn(true)
            whenever(thumbnail.path).thenReturn(expectedPath)
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val actual = underTest.getPublicNodeThumbnailFromLocal(nodeHandle)
            assertThat(actual?.path).isEqualTo(expectedPath)
        }
    }

    @Test
    fun `test that get thumbnail or preview file name returns correctly for nodeHandle`() =
        runTest {
            val expected = megaApi.handleToBase64(nodeHandle) + ".jpg"
            assertThat(underTest.getThumbnailOrPreviewFileName(nodeHandle)).isEqualTo(expected)
        }

    @Test
    fun `test that get thumbnail or preview file name returns correctly for string`() =
        runTest {
            val testString = "test"
            val testStringEncoded = "testEncoded"
            whenever(stringWrapper.encodeBase64(testString)).thenReturn(
                testStringEncoded
            )
            val expected = "$testStringEncoded.jpg"
            assertThat(underTest.getThumbnailOrPreviewFileName(testString)).isEqualTo(
                expected
            )
        }

    @Test
    fun `test that createPreview throws IllegalArgumentException if previewFile is null`() =
        runTest {
            val testString = "test"
            val testStringEncoded = "testEncoded"
            val previewFileName = "$testStringEncoded.jpg"
            whenever(underTest.getThumbnailOrPreviewFileName(testString)).thenReturn(
                previewFileName
            )
            whenever(cacheGateway.getCacheFile(any(), any())).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest.createPreview(testString, mock())
            }

        }

    @Test
    fun `test that createPreview invokes createPreview of SDK and returns expected result`() =
        runTest {
            val testString = "test"
            val testStringEncoded = "testEncoded"
            val previewFileName = "$testStringEncoded.jpg"
            val file = mock<File>()
            val previewFile = mock<File>()
            val sourcePath = "../cache/sample.jpg"
            val destinationPath = ".../previewsMEGA/$previewFileName"
            whenever(underTest.getThumbnailOrPreviewFileName(testString)).thenReturn(
                previewFileName
            )
            whenever(
                cacheGateway.getCacheFile(
                    any(),
                    any()
                )
            ).thenReturn(previewFile)
            whenever(file.absolutePath).thenReturn(sourcePath)
            whenever(previewFile.absolutePath).thenReturn(destinationPath)
            val expected = true
            whenever(megaApi.createPreview(sourcePath, destinationPath)).thenReturn(expected)
            assertThat(underTest.createPreview(testString, file)).isEqualTo(expected)
        }

    @Test
    fun `test that setThumbnail invokes megaApi setThumbnail if node is not null`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
        whenever(megaApi.setThumbnail(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mock()
            )
        }
        underTest.setThumbnail(nodeHandle, thumbnailPath)
        advanceUntilIdle()
        verify(megaApi).setThumbnail(any(), any(), any())
    }

    @Test
    fun `test that setThumbnail does not invoke megaApi setThumbnail if node is null`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(null)
        underTest.setThumbnail(nodeHandle, any())
        verify(megaApi, never()).setThumbnail(any(), any(), any())
    }

    @Test
    fun `test that setPreview invokes megaApi setPreview if node is not null`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(megaNode)
        whenever(megaApi.setPreview(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mock()
            )
        }
        underTest.setPreview(nodeHandle, thumbnailPath)
        advanceUntilIdle()
        verify(megaApi).setPreview(any(), any(), any())
    }

    @Test
    fun `test that setPreview does not invoke megaApi setPreview if node is null`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(nodeHandle)).thenReturn(null)
        underTest.setPreview(nodeHandle, any())
        verify(megaApi, never()).setPreview(any(), any(), any())
    }
}
