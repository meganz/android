package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetFileLinkNodeContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileLinkNodeContentUriUseCaseTest {
    private lateinit var underTest: GetFileLinkNodeContentUriUseCase

    private val httpServerStart = mock<MegaApiHttpServerStartUseCase>()
    private val httpServerIsRunning = mock<MegaApiHttpServerIsRunningUseCase>()
    private val getFileUrlByPublicLinkUseCase = mock<GetFileUrlByPublicLinkUseCase>()

    private val expectedUrl = "url"

    @BeforeAll
    fun setup() {
        underTest = GetFileLinkNodeContentUriUseCase(
            httpServerStart = httpServerStart,
            httpServerIsRunning = httpServerIsRunning,
            getFileUrlByPublicLinkUseCase = getFileUrlByPublicLinkUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            httpServerStart,
            httpServerIsRunning,
            getFileUrlByPublicLinkUseCase
        )
    }

    @Test
    fun `test that remote content uri is returned and should stop http server`() = runTest {
        whenever(getFileUrlByPublicLinkUseCase(any())).thenReturn(expectedUrl)
        whenever(httpServerIsRunning()).thenReturn(0)
        whenever(httpServerIsRunning()).thenReturn(0)
        assertThat(underTest("link")).isEqualTo(
            NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = true)
        )
    }

    @Test
    fun `test that remote content uri is returned and should not stop http server`() = runTest {
        whenever(getFileUrlByPublicLinkUseCase(any())).thenReturn(expectedUrl)
        whenever(httpServerIsRunning()).thenReturn(1)
        whenever(httpServerIsRunning()).thenReturn(1)
        assertThat(underTest("link")).isEqualTo(
            NodeContentUri.RemoteContentUri(url = expectedUrl, shouldStopHttpSever = false)
        )
    }
}