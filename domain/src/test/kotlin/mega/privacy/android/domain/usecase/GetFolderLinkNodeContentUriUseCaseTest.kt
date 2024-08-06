package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFolderLinkNodeContentUriUseCaseTest {
    private lateinit var underTest: GetFolderLinkNodeContentUriUseCase

    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()
    private val getLocalFolderLinkFromMegaApiFolderUseCase =
        mock<GetLocalFolderLinkFromMegaApiFolderUseCase>()

    private val expectedUrl = "url"

    @BeforeAll
    fun setup() {
        underTest = GetFolderLinkNodeContentUriUseCase(
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiHttpServerStartUseCase,
            megaApiHttpServerIsRunningUseCase,
            getNodeContentUriUseCase,
            hasCredentialsUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
            getLocalFolderLinkFromMegaApiFolderUseCase
        )
    }

    @Test
    fun `test that remote content uri is returned and should stop http server when hasCredentialsUseCase function returns true`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(any())).thenReturn(expectedUrl)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            assertThat(underTest(mock())).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = true)
            )
        }

    @Test
    fun `test that remote content uri is returned and should stop http server when hasCredentialsUseCase function returns false`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(any())).thenReturn(expectedUrl)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
            assertThat(underTest(mock())).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = true)
            )
        }

    @Test
    fun `test that remote content uri is returned and should not stop http server when hasCredentialsUseCase function returns true`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(any())).thenReturn(expectedUrl)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            assertThat(underTest(mock())).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = false)
            )
        }

    @Test
    fun `test that remote content uri is returned and should not stop http server when hasCredentialsUseCase function returns false`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(any())).thenReturn(expectedUrl)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            assertThat(underTest(mock())).isEqualTo(
                NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = false)
            )
        }

    @Test
    fun `test that getNodeContentUriUseCase is invoked as expected when hasCredentialsUseCase function returns true`() =
        runTest {
            val fileNode = mock<TypedFileNode>()
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(any())).thenReturn(null)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            underTest(fileNode)
            verify(getNodeContentUriUseCase).invoke(fileNode)
        }

    @Test
    fun `test that getNodeContentUriUseCase is invoked as expected when hasCredentialsUseCase function returns false`() =
        runTest {
            val fileNode = mock<TypedFileNode>()
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(any())).thenReturn(null)
            whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
            underTest(fileNode)
            verify(getNodeContentUriUseCase).invoke(fileNode)
        }
}