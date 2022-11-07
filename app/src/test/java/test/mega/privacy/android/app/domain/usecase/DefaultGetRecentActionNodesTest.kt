package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetThumbnail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRecentActionNodesTest {
    private lateinit var underTest: GetRecentActionNodes

    private val getThumbnail = mock<GetThumbnail> {
        onBlocking { invoke(any()) }.thenReturn(null)
    }
    private val nodes = (0L..5L).map { value ->
        mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(value))
        }
    }

    @Before
    fun setUp() {
        underTest = DefaultGetRecentActionNodes(
            getThumbnail = getThumbnail,
            ioDispatcher = UnconfinedTestDispatcher(),
            getNodeByHandle = mock()
        )
    }

    @Test
    fun `test that if getThumbnail succeed for each element,the list returned contains as many elements as the list given in parameter`() =
        runTest {
            assertThat(underTest.invoke(nodes).size).isEqualTo(nodes.size)
        }


    @Test
    fun `test that if one of getThumbnail throws an exception when looping over the nodes, under test stills returns the list of nodes except the one who failed`() =
        runTest {
            whenever(getThumbnail(3L)).thenAnswer {
                throw IOException("Error!")
            }

            assertThat(underTest.invoke(nodes).size).isEqualTo(nodes.size - 1)
        }
}
