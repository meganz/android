package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.DataTooLargeException
import mega.privacy.android.domain.repository.HttpConnectionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetBoundedDataBytesFromUrlUseCaseTest {

    private lateinit var underTest: GetBoundedDataBytesFromUrlUseCase

    private val httpConnectionRepository = mock<HttpConnectionRepository>()

    private val url = URL("http://127.0.0.1:4443/file.pdf")

    @BeforeEach
    fun setUp() {
        reset(httpConnectionRepository)
        underTest = GetBoundedDataBytesFromUrlUseCase(httpConnectionRepository)
    }

    @Test
    fun `test that invoke returns the bytes read within the limit`() = runTest {
        val expected = ByteArray(10)
        whenever(httpConnectionRepository.getDataBytesFromUrlWithinLimit(url, 10))
            .thenReturn(expected)

        assertThat(underTest(url, 10)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns null when the repository returns null`() = runTest {
        whenever(httpConnectionRepository.getDataBytesFromUrlWithinLimit(url, 10)).thenReturn(null)

        assertThat(underTest(url, 10)).isNull()
    }

    @Test
    fun `test that invoke propagates DataTooLargeException from the repository`() = runTest {
        whenever(httpConnectionRepository.getDataBytesFromUrlWithinLimit(url, 10))
            .thenThrow(DataTooLargeException(10))

        val exception = assertThrows<DataTooLargeException> { underTest(url, 10) }

        assertThat(exception.maxBytes).isEqualTo(10)
    }

    @Test
    fun `test that invoke rejects a non positive limit`() = runTest {
        assertThrows<IllegalArgumentException> { underTest(url, 0) }
    }
}
