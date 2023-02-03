package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultImageRepositoryTest {
    private lateinit var underTest: ImageRepository

    private val context: Context = mock()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val fileManagementPreferencesGateway = mock<FileManagementPreferencesGateway>()
    private val fileGateway = mock<FileGateway>()

    private val cacheDir = File("cache")

    @Before
    fun setUp() {
        underTest = DefaultImageRepository(
            context = context,
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
            fileManagementPreferencesGateway = fileManagementPreferencesGateway,
            fileGateway = fileGateway,
        )
    }

    @Test
    fun `test that get thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            val node = mock<MegaNode>()
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(node.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(node)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiGateway.getThumbnail(any(), any(), any())).thenAnswer {
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

    @Test(expected = MegaException::class)
    fun `test that get thumbnail from server returns doesn't successfully`() {
        runTest {
            val node = mock<MegaNode>()
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(node.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(node)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(megaApiGateway.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            underTest.getThumbnailFromServer(1L)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if node is null`() {
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(null)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false
            )
        }
    }


    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if node is not file`() {
        runTest {
            val node = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(node)
            whenever(node.isFile).thenReturn(false)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if full image file is null`() {
        runTest {
            val node = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(node)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(null)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false
            )
        }
    }
}