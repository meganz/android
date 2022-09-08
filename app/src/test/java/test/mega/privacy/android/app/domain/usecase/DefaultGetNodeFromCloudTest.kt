package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetNodeFromCloud
import mega.privacy.android.app.domain.usecase.GetNodeByFingerprint
import mega.privacy.android.app.domain.usecase.GetNodeByFingerprintAndParentNode
import mega.privacy.android.app.domain.usecase.GetNodeFromCloud
import mega.privacy.android.app.domain.usecase.GetNodesByOriginalFingerprint
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetNodeFromCloudTest {

    private lateinit var underTest: GetNodeFromCloud

    private val getNodeByFingerprint = mock<GetNodeByFingerprint>()
    private val getNodesByOriginalFingerprint = mock<GetNodesByOriginalFingerprint>()
    private val getNodeByFingerprintAndParentNode = mock<GetNodeByFingerprintAndParentNode>()

    @Before
    fun setUp() {
        underTest = DefaultGetNodeFromCloud(
            getNodeByFingerprint,
            getNodesByOriginalFingerprint,
            getNodeByFingerprintAndParentNode
        )
    }

    @Test
    fun `test that node is found by original fingerprint from parent node`() = runTest {
        val result = mock<MegaNode> {}
        val nodes = mock<MegaNodeList> {
            on { size() }.thenReturn(1)
            on { get(0) }.thenReturn(result)
        }
        whenever(getNodesByOriginalFingerprint(any(), anyOrNull())).thenReturn(nodes)

        assertThat(underTest("", mock())).isEqualTo(result)
    }

    @Test
    fun `test that node is found by fingerprint from parent node`() = runTest {
        val result = mock<MegaNode> {}
        whenever(getNodesByOriginalFingerprint(any(), anyOrNull())).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNode(any(), any())).thenReturn(result)

        assertThat(underTest("", mock())).isEqualTo(result)
    }

    @Test
    fun `test that node is found by original fingerprint without parent node`() = runTest {
        val result = mock<MegaNode> {}
        val nodes = mock<MegaNodeList> {
            on { size() }.thenReturn(1)
            on { get(0) }.thenReturn(result)
        }
        whenever(getNodesByOriginalFingerprint(any(), anyOrNull())).thenReturn(null, nodes)
        whenever(getNodeByFingerprintAndParentNode(any(), any())).thenReturn(null)

        assertThat(underTest("", mock())).isEqualTo(result)
    }

    @Test
    fun `test that node is found by fingerprint without parent node`() = runTest {
        val result = mock<MegaNode> {}
        whenever(getNodesByOriginalFingerprint(any(), anyOrNull())).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNode(any(), any())).thenReturn(null)
        whenever(getNodeByFingerprint(any())).thenReturn(result)

        assertThat(underTest("", mock())).isEqualTo(result)
    }

    @Test
    fun `test that node is not found`() = runTest {
        whenever(getNodesByOriginalFingerprint(any(), anyOrNull())).thenReturn(null)
        whenever(getNodeByFingerprintAndParentNode(any(), any())).thenReturn(null)
        whenever(getNodeByFingerprint(any())).thenReturn(null)

        assertThat(underTest("", mock())).isEqualTo(null)
    }
}
