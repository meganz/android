package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTextContentForFileLinkUseCaseTest {

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val startStreamingServer: StartStreamingServer = mock()
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase = mock()
    private val readStreamingContentUseCase: ReadStreamingContentUseCase = mock()

    private val underTest = GetTextContentForFileLinkUseCase(
        ioDispatcher = ioDispatcher,
        startStreamingServer = startStreamingServer,
        getFileUrlByPublicLinkUseCase = getFileUrlByPublicLinkUseCase,
        readStreamingContentUseCase = readStreamingContentUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            startStreamingServer,
            getFileUrlByPublicLinkUseCase,
            readStreamingContentUseCase,
        )
    }

    @Test
    fun `test that invoke loads content via public link streaming`() = runTest {
        val fileLink = "https://mega.nz/file/abc"
        val localUrl = "http://127.0.0.1:4443/abc"
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getFileUrlByPublicLinkUseCase(fileLink)).thenReturn(localUrl)
        whenever(readStreamingContentUseCase(localUrl)).thenReturn("public content")

        val chunks = underTest(urlFileLink = fileLink).toList()
        val content = chunks.flatten().joinToString("\n")

        assertThat(content).isEqualTo("public content")
        verify(startStreamingServer).invoke()
        verify(getFileUrlByPublicLinkUseCase).invoke(fileLink)
    }

    @Test
    fun `test that invoke throws when public link streaming URL is null`() = runTest {
        val fileLink = "https://mega.nz/file/abc"
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getFileUrlByPublicLinkUseCase(fileLink)).thenReturn(null)

        val result = runCatching {
            underTest(urlFileLink = fileLink).toList()
        }

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).contains("Failed to get streaming URL for file link")
        verify(readStreamingContentUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke emits chunked lines`() = runTest {
        val fileLink = "https://mega.nz/file/abc"
        val localUrl = "http://127.0.0.1:4443/abc"
        whenever(startStreamingServer()).thenReturn(Unit)
        whenever(getFileUrlByPublicLinkUseCase(fileLink)).thenReturn(localUrl)
        whenever(readStreamingContentUseCase(localUrl)).thenReturn("a\nb\nc\nd\ne")

        val chunks = underTest(urlFileLink = fileLink, chunkSizeLines = 2).toList()

        assertThat(chunks).hasSize(3)
        assertThat(chunks[0]).containsExactly("a", "b")
        assertThat(chunks[1]).containsExactly("c", "d")
        assertThat(chunks[2]).containsExactly("e")
    }
}
