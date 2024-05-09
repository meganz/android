package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MegaNodeRepositoryImplTest {

    private lateinit var underTest: MegaNodeRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getLinksSortOrder: GetLinksSortOrder = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val nodeId = NodeId(123456L)
    private val megaNode: MegaNode = mock {
        on { handle } doReturn 123456L
    }


    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = MegaNodeRepositoryImpl(
            context = mock(),
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = mock(),
            megaChatApiGateway = mock(),
            ioDispatcher = testDispatcher,
            megaLocalStorageGateway = mock(),
            shareDataMapper = mock(),
            megaExceptionMapper = mock(),
            sortOrderIntMapper = sortOrderIntMapper,
            nodeMapper = mock(),
            fileTypeInfoMapper = mock(),
            fileGateway = mock(),
            chatFilesFolderUserAttributeMapper = mock(),
            streamingGateway = mock(),
            getLinksSortOrder = getLinksSortOrder,
            cancelTokenProvider = mock(),
            getCloudSortOrder = getCloudSortOrder,
            megaSearchFilterMapper = mock(),
        )
    }

    @Test
    fun `test getOutShares fetches out shares from megaApiGateway if the node is found`() =
        runTest {
            val id = 1L
            val nodeId = NodeId(id)
            val node = mock<MegaNode> {
                on { handle }.thenReturn(id)
            }
            val expected = List<MegaShare>(5) { mock() }
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(node)
            whenever(megaApiGateway.getOutShares(node)).thenReturn(expected)

            val result = underTest.getOutShares(nodeId)
            assertThat(result).isEqualTo(result)
        }

    @Test
    fun `test getOutShares returns null if node is not found`() = runTest {
        val id = 1L
        val nodeId = NodeId(id)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)

        val result = underTest.getOutShares(nodeId)
        assertThat(result).isNull()
    }

    @Test
    fun `test that getInShares returns list of mega nodes`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(megaApiGateway.getInShares(sortOrderIntMapper(SortOrder.ORDER_NONE))).thenReturn(
            listOf(megaNode)
        )
        val actual = underTest.getInShares()
        assertThat(actual.first().handle).isEqualTo(nodeId.longValue)
    }

    @Test
    fun `test that getPublicLinks returns list of mega nodes`() = runTest {
        whenever(getLinksSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(megaApiGateway.getPublicLinks(sortOrderIntMapper(getLinksSortOrder())))
            .thenReturn(listOf(megaNode))
        val actual = underTest.getPublicLinks()
        assertThat(actual.first().handle).isEqualTo(nodeId.longValue)
    }

}