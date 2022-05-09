package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.repository.DefaultAlbumsRepository
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.utils.CacheFolderManager
import nz.mega.sdk.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class DefaultAlbumsRepositoryTest {
    private lateinit var underTest: AlbumsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaDBHandlerGateway = mock<MegaLocalStorageGateway>()

    private val cacheDir = File("cache")

    @Before
    fun setUp() {
        underTest = DefaultAlbumsRepository(
            apiFacade = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaLocalStorageFacade = megaDBHandlerGateway,
            context = mock { on { cacheDir }.thenReturn(cacheDir) } // Should be using CacheFolderGateway to retrieve cache dir once DK's MR is merged
        )
    }

    @Test
    fun `test that get thumbnail from server returns successfully if no error is thrown`() =
        runTest {
            val node = mock<MegaNode>()
            val thumbnailName = "Test"
            whenever(node.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(node)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
            whenever(megaApiGateway.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getThumbnailFromServer(1L)
            assertThat(actual.path).isEqualTo("${cacheDir.path}/${CacheFolderManager.THUMBNAIL_FOLDER}/$thumbnailName")
        }

    @Test(expected = MegaException::class)
    fun `test that error is thrown if fetch thumbnail from server does not return successfully`() =
        runTest {
            val node = mock<MegaNode>()
            val thumbnailName = "Test"
            whenever(node.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(node)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK + 1) }
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
