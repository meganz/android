package mega.privacy.android.data.mapper.login

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FetchNodesUpdateMapperTest {

    private lateinit var underTest: FetchNodesUpdateMapper

    private var totalBytes = 500L
    private var transferredBytes = 350L
    private val progress = transferredBytes.toFloat() / totalBytes.toFloat()

    @Before
    fun setUp() {
        underTest = FetchNodesUpdateMapper()
    }

    @Test
    fun `test that a request with totalBytes equal to 0 returns a 0 Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(0F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(0)
        }
        Truth.assertThat(underTest(request)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that a request with negative totalBytes returns a 0 Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(0F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(-1)
        }
        Truth.assertThat(underTest(request)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that correct Progress is returned with valid totalBytes`() {
        val expectedUpdate = FetchNodesUpdate(Progress(progress), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that negative progress returns 0,99F Progress`() {
        transferredBytes = -1
        val expectedUpdate = FetchNodesUpdate(Progress(0.99F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that progress greater than 0,99 returns 0,99F Progress`() {
        transferredBytes = 499L
        val expectedUpdate = FetchNodesUpdate(Progress(0.99F), null)
        val request = mock<MegaRequest> {
            on { totalBytes }.thenReturn(totalBytes)
            on { transferredBytes }.thenReturn(transferredBytes)
        }

        Truth.assertThat(underTest(request)).isEqualTo(expectedUpdate)
    }

    @Test
    fun `test that null request returns 1F Progress`() {
        val expectedUpdate = FetchNodesUpdate(Progress(1F), null)
        Truth.assertThat(underTest(null)).isEqualTo(expectedUpdate)
    }
}