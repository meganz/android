package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetIncomingSharesChildrenNodeTest {
    private lateinit var underTest: GetIncomingSharesChildrenNode

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_NONE)
    }
    private val getOthersSortOrder = mock<GetOthersSortOrder> {
        onBlocking { invoke() }.thenReturn(1)
    }
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetIncomingSharesChildrenNode(
            getNodeByHandle,
            getChildrenNode,
            getCloudSortOrder,
            getOthersSortOrder,
            filesRepository,
        )
    }

    @Test
    fun `test that invoke with -1L execute filesRepository getIncomingSharesNode function with others sort order`() =
        runTest {
            underTest(-1L)

            verify(filesRepository).getIncomingSharesNode(getOthersSortOrder())
        }

    @Test
    fun `test that invoke with INVALID_HANDLE executes filesRepository getIncomingSharesNode function with others sort order`() =
        runTest {
            underTest(MegaApiJava.INVALID_HANDLE)

            verify(filesRepository).getIncomingSharesNode(getOthersSortOrder())
        }

    @Test
    fun `test that invoke with valid parent handle, retrieve parent node`() =
        runTest {
            val parentHandle = 123456789L
            underTest(parentHandle)
            verify(getNodeByHandle).invoke(any())
        }

    @Test
    fun `test that if parent node can be retrieved, executes getChildrenNode with getCloudSortOrder`() =
        runTest {
            val parentHandle = 123456789L
            val result = mock<MegaNode> {}
            whenever(getNodeByHandle(parentHandle)).thenReturn(result)
            underTest(parentHandle)

            verify(getChildrenNode).invoke(result, getCloudSortOrder())
        }

    @Test
    fun `test that if parent node cannot be retrieved, return null`() =
        runTest {
            val parentHandle = 123456789L
            whenever(getNodeByHandle(parentHandle)).thenReturn(null)

            Truth.assertThat(underTest(parentHandle)).isEqualTo(null)
        }
}