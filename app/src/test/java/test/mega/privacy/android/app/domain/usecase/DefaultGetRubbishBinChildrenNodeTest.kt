package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetRubbishBinChildrenNodeTest {

    private lateinit var underTest: GetRubbishBinChildrenNode
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getChildrenNode = mock<GetChildrenNode>()
    private val getRubbishBinFolder = mock<GetRubbishBinFolder>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @Before
    fun setUp() {
        underTest = DefaultGetRubbishBinChildrenNode(
            getNodeByHandle,
            getChildrenNode,
            getRubbishBinFolder,
            getCloudSortOrder
        )
    }

    @Test
    fun `test that invoke with -1L invoke getRubbishBinNode`() = runTest {
        underTest(-1L)

        verify(getRubbishBinFolder).invoke()
    }

    @Test
    fun `test that invoke with value except -1L invoke getNodeByHandle`() = runTest {
        val parentHandle = 0L
        underTest(parentHandle)

        verify(getNodeByHandle).invoke(parentHandle)
    }

    @Test
    fun `test that -1L invoke getChildrenNode with result of getRubbishBinNode`() = runTest {
        val result = mock<MegaNode> {}
        whenever(getRubbishBinFolder()).thenReturn(result)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        underTest(-1L)

        verify(getChildrenNode).invoke(eq(result), any())
    }

    @Test
    fun `test that -1L invoke getChildrenNode with result of getNodeByHandle`() = runTest {
        val result = mock<MegaNode> {}
        val parentHandle = 0L
        whenever(getNodeByHandle(parentHandle)).thenReturn(result)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        underTest(parentHandle)

        verify(getChildrenNode).invoke(eq(result), any())
    }

    @Test
    fun `test that underTest is invoked with value of get order sort management`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(getRubbishBinFolder()).thenReturn(mock())
        underTest(-1L)

        verify(getChildrenNode).invoke(any(), eq(sortOrder))
    }
}