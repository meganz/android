package mega.privacy.android.data.mapper.login

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Test

class TemporaryWaitingErrorMapperTest {

    private lateinit var underTest: TemporaryWaitingErrorMapper

    @Before
    fun setUp() {
        underTest = TemporaryWaitingErrorMapper()
    }

    @Test
    fun `test that RETRY_NONE returns null`() {
        val result = underTest(MegaApiJava.RETRY_NONE)
        assertThat(result).isNull()
    }

    @Test
    fun `test that RETRY_CONNECTIVITY returns ConnectivityIssues`() {
        val result = underTest(MegaApiJava.RETRY_CONNECTIVITY)
        assertThat(result).isEqualTo(TemporaryWaitingError.ConnectivityIssues)
    }

    @Test
    fun `test that RETRY_SERVERS_BUSY returns ServerIssues`() {
        val result = underTest(MegaApiJava.RETRY_SERVERS_BUSY)
        assertThat(result).isEqualTo(TemporaryWaitingError.ServerIssues)
    }

    @Test
    fun `test that RETRY_API_LOCK returns APILock`() {
        val result = underTest(MegaApiJava.RETRY_API_LOCK)
        assertThat(result).isEqualTo(TemporaryWaitingError.APILock)
    }

    @Test
    fun `test that RETRY_RATE_LIMIT returns APIRate`() {
        val result = underTest(MegaApiJava.RETRY_RATE_LIMIT)
        assertThat(result).isEqualTo(TemporaryWaitingError.APIRate)
    }

    @Test
    fun `test that unknown error code returns ConnectivityIssues`() {
        val result = underTest(-1)
        assertThat(result).isEqualTo(TemporaryWaitingError.ConnectivityIssues)
    }
}