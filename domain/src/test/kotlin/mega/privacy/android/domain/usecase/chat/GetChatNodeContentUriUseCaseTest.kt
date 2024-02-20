package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
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
internal class GetChatNodeContentUriUseCaseTest {
    private lateinit var underTest: GetChatNodeContentUriUseCase
    private val nodeRepository: NodeRepository = mock()
    private val getLocalFileForNode: GetLocalFileForNode = mock()
    private val httpServerStart: MegaApiHttpServerStartUseCase = mock()
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = GetChatNodeContentUriUseCase(
            nodeRepository,
            getLocalFileForNode,
            httpServerStart,
            httpServerIsRunning
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getLocalFileForNode,
            httpServerStart,
            httpServerIsRunning
        )
    }

    @Test
    fun `test that local content uri is returned`() = runTest {
        val localFile = mock<File>()
        whenever(getLocalFileForNode(any())).thenReturn(localFile)
        val fileNode = mock<ChatImageFile>()
        assertThat(underTest(fileNode)).isEqualTo(
            NodeContentUri.LocalContentUri(
                localFile
            )
        )
    }

    @Test
    fun `test that remote content uri is returned and should stop http server`() = runTest {
        whenever(getLocalFileForNode(any())).thenReturn(null)
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
        whenever(getLocalFileForNode(any())).thenReturn(null)
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