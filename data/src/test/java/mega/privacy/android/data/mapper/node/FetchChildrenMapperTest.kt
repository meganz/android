package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.repository.CancelTokenProvider
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class FetchChildrenMapperTest {
    private lateinit var underTest: FetchChildrenMapper

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val nodeMapper = mock<NodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()

    @BeforeEach
    internal fun setUp() {
        underTest = FetchChildrenMapper(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            nodeMapperProvider = { nodeMapper },
            cancelTokenProvider = cancelTokenProvider,
            megaSearchFilterMapper = megaSearchFilterMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    internal fun `test that the children are returned as untyped nodes when the function is called`() =
        runTest {
            val megaNode = mock<MegaNode>()
            val expectedOrder = 5
            val token = mock<MegaCancelToken>()
            val filter = mock<MegaSearchFilter>()
            val sortOrder = SortOrder.ORDER_DEFAULT_ASC
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(sortOrderIntMapper(sortOrder)).thenReturn(expectedOrder)
            whenever(megaSearchFilterMapper(NodeId(megaNode.handle))).thenReturn(filter)
            val child = mock<MegaNode>()
            megaApiGateway.stub {
                onBlocking { getChildren(filter, expectedOrder, token) }.thenReturn(listOf(child))
            }
            val expected = mock<FileNode>()
            whenever(nodeMapper(child)).thenReturn(expected)

            val func = underTest(megaNode)
            assertThat(func(sortOrder)).containsExactly(expected)
            verify(megaApiGateway).getChildren(filter, expectedOrder, token)

        }

}