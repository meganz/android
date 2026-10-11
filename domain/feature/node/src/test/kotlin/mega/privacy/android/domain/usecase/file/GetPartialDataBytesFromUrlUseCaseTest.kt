package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.HttpConnectionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPartialDataBytesFromUrlUseCaseTest {
    private lateinit var underTest: GetPartialDataBytesFromUrlUseCase

    private val httpConnectionRepository = mock<HttpConnectionRepository>()

    @BeforeEach
    fun setUp() {
        underTest = GetPartialDataBytesFromUrlUseCase(httpConnectionRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(httpConnectionRepository)
    }

    @Test
    fun `test that the bytes returned by the repository are returned`() = runTest {
        val url = URL("http://localhost/stream")
        val maxBytes = 65536
        val expected = byteArrayOf(1, 2, 3)
        whenever(httpConnectionRepository.getDataBytesFromUrl(url, maxBytes)).thenReturn(expected)

        assertThat(underTest(url, maxBytes)).isEqualTo(expected)
    }

    @Test
    fun `test that null is returned when the repository returns null`() = runTest {
        val url = URL("http://localhost/stream")
        whenever(httpConnectionRepository.getDataBytesFromUrl(url, 10)).thenReturn(null)

        assertThat(underTest(url, 10)).isNull()
    }
}
