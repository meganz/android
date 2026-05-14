package mega.privacy.android.domain.usecase.texteditor

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReadStreamingContentUseCaseTest {

    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase = mock()

    private val underTest = ReadStreamingContentUseCase(
        getDataBytesFromUrlUseCase = getDataBytesFromUrlUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(getDataBytesFromUrlUseCase)
    }

    @Test
    fun `test that invoke returns empty string when bytes are null`() = runTest {
        whenever(getDataBytesFromUrlUseCase(any())).thenReturn(null)

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that invoke returns content without trailing newline stripped`() = runTest {
        val content = "hello world"
        whenever(getDataBytesFromUrlUseCase(any()))
            .thenReturn(content.toByteArray(Charsets.UTF_8))

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEqualTo("hello world")
    }

    @Test
    fun `test that invoke strips single trailing newline`() = runTest {
        val content = "line1\nline2\n"
        whenever(getDataBytesFromUrlUseCase(any()))
            .thenReturn(content.toByteArray(Charsets.UTF_8))

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEqualTo("line1\nline2")
    }

    @Test
    fun `test that invoke preserves content with multiple trailing newlines`() = runTest {
        val content = "line1\n\n"
        whenever(getDataBytesFromUrlUseCase(any()))
            .thenReturn(content.toByteArray(Charsets.UTF_8))

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEqualTo("line1\n")
    }

    @Test
    fun `test that invoke returns empty string when bytes are empty`() = runTest {
        whenever(getDataBytesFromUrlUseCase(any()))
            .thenReturn(ByteArray(0))

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that invoke preserves multiline content without trailing newline`() = runTest {
        val content = "line1\nline2\nline3"
        whenever(getDataBytesFromUrlUseCase(any()))
            .thenReturn(content.toByteArray(Charsets.UTF_8))

        val result = underTest("http://127.0.0.1:4443/file")

        assertThat(result).isEqualTo("line1\nline2\nline3")
    }
}
