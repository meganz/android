package mega.privacy.android.data.repository.thumbnailpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
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

    private val cacheDir = File("cache")

    val nodeHandle = 123L
    val path = "/test/path"
    private val megaNode = mock<MegaNode>()

    @BeforeAll
    fun setUp() {
        underTest = ThumbnailPreviewRepositoryImpl(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(megaApi, megaApiFolder, cacheGateway)
    }

    @Test
    fun `test that get thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApi.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

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

            val actual = underTest.getThumbnailFromServer(1L)
            assertThat(actual?.path).isEqualTo(expectedPath)
        }
    }

    @Test
    fun `test that get thumbnail from server returns doesn't successfully`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApi.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

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
                underTest.getThumbnailFromServer(1L)
            }
        }
    }

    @Test
    fun `test that get public node thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

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

            val actual = underTest.getPublicNodeThumbnailFromServer(1L)
            assertThat(actual?.path).isEqualTo(expectedPath)
        }
    }

    @Test
    fun `test that get public node thumbnail from server returns doesn't successfully`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

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
                underTest.getPublicNodeThumbnailFromServer(1L)
            }
        }
    }

    @Test
    fun `test that get public node thumbnail from local returns not successful`() {
        runTest {
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(null)

            val actual = underTest.getPublicNodeThumbnailFromLocal(1L)
            assertThat(actual?.path).isEqualTo(null)
        }
    }

    @Test
    fun `test that get public node thumbnail from local returns successfully if no error is thrown`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail: File = mock()

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(thumbnail.exists()).thenReturn(true)
            whenever(thumbnail.path).thenReturn(expectedPath)
            whenever(megaApiFolder.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val actual = underTest.getPublicNodeThumbnailFromLocal(1L)
            assertThat(actual?.path).isEqualTo(expectedPath)
        }
    }

    @Test
    fun `test that get thumbnail or preview file name returns correctly`() = runTest {
        val expected = megaApi.handleToBase64(nodeHandle) + ".jpg"
        assertThat(underTest.getThumbnailOrPreviewFileName(nodeHandle)).isEqualTo(expected)
    }
}
