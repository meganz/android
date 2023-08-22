package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MegaNodeRepositoryImplTest {

    private lateinit var underTest: MegaNodeRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val testDispatcher = UnconfinedTestDispatcher()


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
            sortOrderIntMapper = mock(),
            nodeMapper = mock(),
            fileTypeInfoMapper = mock(),
            offlineNodeInformationMapper = mock(),
            fileGateway = mock(),
            chatFilesFolderUserAttributeMapper = mock(),
            streamingGateway = mock(),
            getLinksSortOrder = mock(),
            cancelTokenProvider = mock()
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
            Truth.assertThat(result).isEqualTo(result)
        }

    @Test
    fun `test getOutShares returns null if node is not found`() = runTest {
        val id = 1L
        val nodeId = NodeId(id)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)

        val result = underTest.getOutShares(nodeId)
        Truth.assertThat(result).isNull()
    }
}