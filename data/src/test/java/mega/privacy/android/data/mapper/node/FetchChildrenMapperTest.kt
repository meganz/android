package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class FetchChildrenMapperTest {
    private lateinit var underTest: FetchChildrenMapper

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    private val nodeMapper = mock<NodeMapper>()

    @BeforeEach
    internal fun setUp() {
        underTest = FetchChildrenMapper(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            nodeMapperProvider = { nodeMapper },
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    internal fun `test that the children are returned as untyped nodes when the function is called`() =
        runTest {
            val megaNode = mock<MegaNode>()
            val expectedOrder = 5
            val sortOrder = SortOrder.ORDER_DEFAULT_ASC
            whenever(sortOrderIntMapper(sortOrder)).thenReturn(expectedOrder)
            val child = mock<MegaNode>()
            megaApiGateway.stub {
                onBlocking { getChildren(megaNode, expectedOrder) }.thenReturn(listOf(child))
            }
            val expected = mock<FileNode>()
            whenever(nodeMapper(child)).thenReturn(expected)


            val func = underTest(megaNode)
            assertThat(func(sortOrder)).containsExactly(expected)

        }

}