package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNodeContentUriUseCaseTest {
    private lateinit var underTest: GetNodeContentUriUseCase
    private val nodeRepository: NodeRepository = mock()
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase = mock()
    private val httpServerStart: MegaApiHttpServerStartUseCase = mock()
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = GetNodeContentUriUseCase(
            nodeRepository,
            getNodePreviewFileUseCase,
            httpServerStart,
            httpServerIsRunning
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getNodePreviewFileUseCase,
            httpServerStart,
            httpServerIsRunning
        )
    }

    @Test
    fun `test that local content uri is returned`() = runTest {
        whenever(getNodePreviewFileUseCase(any())).thenReturn(File("path"))
        val fileNode = mock<ChatImageFile>()
        assertThat(underTest(fileNode)).isEqualTo(
            NodeContentUri.LocalContentUri(
                File("path")
            )
        )
    }

    @Test
    fun `test that remote content uri is returned and should stop http server`() = runTest {
        whenever(getNodePreviewFileUseCase(any())).thenReturn(null)
        whenever(httpServerIsRunning()).thenReturn(0)
        val chatFile = mock<ChatImageFile>()
        val link = "link"
        whenever(httpServerIsRunning()).thenReturn(0)
        whenever(nodeRepository.getLocalLink(chatFile)).thenReturn(link)
        assertThat(underTest(chatFile)).isEqualTo(
            NodeContentUri.RemoteContentUri(
                link,
                true
            )
        )
    }

    @Test
    fun `test that remote content uri is returned and should not stop http server`() = runTest {
        whenever(getNodePreviewFileUseCase(any())).thenReturn(null)
        whenever(httpServerIsRunning()).thenReturn(1)
        val chatFile = mock<ChatImageFile>()
        val link = "link"
        whenever(httpServerIsRunning()).thenReturn(1)
        whenever(nodeRepository.getLocalLink(chatFile)).thenReturn(link)
        assertThat(underTest(chatFile)).isEqualTo(
            NodeContentUri.RemoteContentUri(
                link,
                false
            )
        )
    }
}