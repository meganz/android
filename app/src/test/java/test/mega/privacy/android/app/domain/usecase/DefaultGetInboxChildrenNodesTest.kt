package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetInboxChildrenNodes
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.HasInboxChildren
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class of [DefaultGetInboxChildrenNodes]
 */
@ExperimentalCoroutinesApi
class DefaultGetInboxChildrenNodesTest {

    private lateinit var underTest: DefaultGetInboxChildrenNodes

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder> {
        onBlocking { invoke() }.thenReturn(SortOrder.ORDER_NONE)
    }
    private val getInboxNode = mock<GetInboxNode>()
    private val hasInboxChildren = mock<HasInboxChildren>()

    @Before
    fun setUp() {
        underTest = DefaultGetInboxChildrenNodes(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getInboxNode = getInboxNode,
            hasInboxChildren = hasInboxChildren,
        )
    }

    @Test
    fun `test that an empty list is returned if the inbox node is null`() = runTest {
        whenever(hasInboxChildren()).thenReturn(true)
        whenever(getInboxNode()).thenReturn(null)

        val result = underTest()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that an empty list is returned if the inbox node does not have children nodes`() =
        runTest {
            whenever(hasInboxChildren()).thenReturn(false)

            val result = underTest()

            assertThat(result).isEmpty()
        }

    @Test
    fun `test that the inbox children nodes are returned`() = runTest {
        val testInboxNode = mock<MegaNode>()

        whenever(hasInboxChildren()).thenReturn(true)
        whenever(getInboxNode()).thenReturn(testInboxNode)

        underTest()

        verify(getChildrenNode).invoke(
            parent = testInboxNode,
            order = getCloudSortOrder(),
        )
    }
}