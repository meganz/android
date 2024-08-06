package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeContentUriByHandleUseCaseTest {
    private lateinit var underTest: GetNodeContentUriByHandleUseCase

    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()
    private val getLocalFolderLinkFromMegaApiFolderUseCase =
        mock<GetLocalFolderLinkFromMegaApiFolderUseCase>()
    private val getAlbumPhotoFileUrlByNodeIdUseCase = mock<GetAlbumPhotoFileUrlByNodeIdUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val addNodeType = mock<AddNodeType>()

    private val paramHandle = 1L
    private val paramNodeId = NodeId(paramHandle)

    @BeforeAll
    fun setup() {
        underTest = GetNodeContentUriByHandleUseCase(
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            getFileUrlByNodeHandleUseCase = getFileUrlByNodeHandleUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase,
            getAlbumPhotoFileUrlByNodeIdUseCase = getAlbumPhotoFileUrlByNodeIdUseCase,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            addNodeType = addNodeType
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase,
            getFileUrlByNodeHandleUseCase,
            hasCredentialsUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase,
            getAlbumPhotoFileUrlByNodeIdUseCase,
            getNodeContentUriUseCase,
            getNodeByHandleUseCase,
            addNodeType
        )
    }

    @Test
    fun `test that the returned result is expected and should stop http server when getFileUrlByNodeHandleUseCase returns not null`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
        }

    @Test
    fun `test that the returned result is expected and should not stop http server when getFileUrlByNodeHandleUseCase returns not null`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, false)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
        }

    @Test
    fun `test that the returned result is expected and should stop http server when getAlbumPhotoFileUrlByNodeIdUseCase returns not null`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
        }

    @Test
    fun `test that the returned result is expected and should not stop http server when getAlbumPhotoFileUrlByNodeIdUseCase returns not null`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, false)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
        }

    @Test
    fun `test that the returned result is expected and should stop http server when hasCredentialsUseCase returns true`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getLocalFolderLinkFromMegaApiUseCase).invoke(paramHandle)
        }

    @Test
    fun `test that the returned result is expected and should not stop http server when hasCredentialsUseCase returns true`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, false)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getLocalFolderLinkFromMegaApiUseCase).invoke(paramHandle)
        }

    @Test
    fun `test that the returned result is expected and should stop http server when hasCredentialsUseCase returns false`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getLocalFolderLinkFromMegaApiFolderUseCase).invoke(paramHandle)
        }

    @Test
    fun `test that the returned result is expected and should not stop http server when hasCredentialsUseCase returns false`() =
        runTest {
            val expectedUrl = "url"
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, false)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(paramHandle)).thenReturn(expectedUrl)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getLocalFolderLinkFromMegaApiFolderUseCase).invoke(paramHandle)
        }

    @Test
    fun `test that the returned result is expected and getNodeContentUriUseCase is invoked when hasCredentialsUseCase returns true`() =
        runTest {
            val expectedUrl = "url"
            val expectedNode = mock<TypedFileNode>()
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(paramHandle)).thenReturn(null)
            whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(expectedNode)
            whenever(addNodeType(any())).thenReturn(expectedNode)
            whenever(getNodeContentUriUseCase(expectedNode)).thenReturn(contentUri)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getNodeContentUriUseCase).invoke(expectedNode)
        }

    @Test
    fun `test that the returned result is expected and getNodeContentUriUseCase is invoked when hasCredentialsUseCase returns false`() =
        runTest {
            val expectedUrl = "url"
            val expectedNode = mock<TypedFileNode>()
            val contentUri = NodeContentUri.RemoteContentUri(expectedUrl, true)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(paramHandle)).thenReturn(null)
            whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(expectedNode)
            whenever(addNodeType(any())).thenReturn(expectedNode)
            whenever(getNodeContentUriUseCase(expectedNode)).thenReturn(contentUri)
            val actual = underTest(paramHandle)
            assertThat(actual).isEqualTo(contentUri)
            verify(getNodeContentUriUseCase).invoke(expectedNode)
        }

    @Test
    fun `test that the IllegalStateException is thrown when getNodeByHandleUseCase returns null`() =
        runTest {
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(paramHandle)).thenReturn(null)
            whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(null)
            assertThrows<IllegalStateException> {
                underTest(paramHandle)
            }
        }

    @Test
    fun `test that the IllegalStateException is thrown when getNodeByHandleUseCase returns a folder`() =
        runTest {
            val expectedNode = mock<TypedFolderNode>()
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            whenever(getFileUrlByNodeHandleUseCase(paramHandle)).thenReturn(null)
            whenever(getAlbumPhotoFileUrlByNodeIdUseCase(paramNodeId)).thenReturn(null)
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(paramHandle)).thenReturn(null)
            whenever(getNodeByHandleUseCase(paramHandle)).thenReturn(expectedNode)
            whenever(addNodeType(any())).thenReturn(expectedNode)
            assertThrows<IllegalStateException> {
                underTest(paramHandle)
            }
        }
}