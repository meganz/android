package mega.privacy.android.data.mapper.login

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FetchNodesUpdateMapperImplTest {

    private lateinit var underTest: FetchNodesUpdateMapper

    private var totalBytes = 500L
    private var transferredBytes = 350L
    private val progress = transferredBytes.toFloat() / totalBytes.toFloat()

    @Before
    fun setUp() {
        underTest = FetchNodesUpdateMapper()
    }

    @Test
    fun `test that null MegaError returns updated without temporary error`() {
        val expectedUpdate = FetchNodesUpdate(mock(), null)
        Truth.assertThat(underTest(mock(), null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error API_EAGAIN and value RETRY_CONNECTIVITY returns ConnectivityIssues`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.ConnectivityIssues)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_CONNECTIVITY.toLong())
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error API_EAGAIN and value RETRY_SERVERS_BUSY returns ServerIssues`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.ServerIssues)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_SERVERS_BUSY.toLong())
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error API_EAGAIN and value RETRY_API_LOCK returns APILock`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.APILock)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_API_LOCK.toLong())
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error API_EAGAIN and value RETRY_RATE_LIMIT returns APIRate`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.APIRate)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_RATE_LIMIT.toLong())
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error API_EAGAIN and value not contemplated returns ServerIssues`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.ServerIssues)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_NONE.toLong())
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that error different than API_EAGAIN returns ServerIssues`() {
        val expectedUpdate = FetchNodesUpdate(mock(), FetchNodesTemporaryError.ServerIssues)
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN + 1)
        }
        Truth.assertThat(underTest(mock(), error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that a request with totalBytes equal to 0 returns a 0 Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(0F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(0)
        }
        Truth.assertThat(underTest(request, null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that a request with negative totalBytes returns a 0 Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(0F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(-1)
        }
        Truth.assertThat(underTest(request, null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that correct Progress is returned with valid totalBytes`() {
        val expectedUpdate = FetchNodesUpdate(Progress(progress), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request, null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that negative progress returns 0,99F Progress`() {
        transferredBytes = -1
        val expectedUpdate = FetchNodesUpdate(Progress(0.99F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request, null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that progress greater than 0,99 returns 0,99F Progress`() {
        transferredBytes = 499L
        val expectedUpdate = FetchNodesUpdate(Progress(0.99F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request, null)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that correct Progress is returned with valid totalBytes and error API_EAGAIN and value RETRY_RATE_LIMIT returns APIRate`() {
        val expectedUpdate = FetchNodesUpdate(Progress(progress), FetchNodesTemporaryError.APIRate)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EAGAIN)
            on { value }.thenReturn(MegaApiJava.RETRY_RATE_LIMIT.toLong())
        }

        Truth.assertThat(underTest(request, error)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that null request returns 1F Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(1F), null)
        Truth.assertThat(underTest(null, null)).isEqualTo(expectedUpdate)
    }
}