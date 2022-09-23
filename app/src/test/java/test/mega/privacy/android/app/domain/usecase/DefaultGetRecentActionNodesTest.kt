package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRecentActionNodesTest {
    private lateinit var underTest: GetRecentActionNodes
    private val getThumbnail = mock<GetThumbnail>()

    @Before
    fun setUp() {
        underTest = DefaultGetRecentActionNodes(
            getThumbnail = getThumbnail,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that if getThumbnail succeed for each element,the list returned contains as many elements as the list given in parameter`() =
        runTest {
            val megaNode0 = mock<MegaNode> {
                on { handle }.thenReturn(0L)
                on { modificationTime }.thenReturn(0L)
                on { isVideo() }.thenReturn(false)
            }
            val megaNode1 = mock<MegaNode> {
                on { handle }.thenReturn(1L)
                on { modificationTime }.thenReturn(0L)
                on { isVideo() }.thenReturn(false)
            }

            val nodes = mock<MegaNodeList> {
                on { get(0) }.thenReturn(megaNode0)
                on { get(1) }.thenReturn(megaNode1)
                on { size() }.thenReturn(2)
            }

            assertThat(underTest.invoke(nodes).size).isEqualTo(nodes.size())
        }


    @Test
    fun `test that if one of getThumbnail throws an exception when looping over the nodes, under test stills returns the list of nodes except the one who failed`() =
        runTest {
            val megaNode0 = mock<MegaNode> {
                on { handle }.thenReturn(0L)
                on { modificationTime }.thenReturn(0L)
                on { isVideo() }.doAnswer { throw Exception() }

            }
            val megaNode1 = mock<MegaNode> {
                on { handle }.thenReturn(1L)
                on { modificationTime }.thenReturn(0L)
                on { isVideo() }.thenReturn(false)
            }

            val nodes = mock<MegaNodeList> {
                on { get(0) }.thenReturn(megaNode0)
                on { get(1) }.thenReturn(megaNode1)
                on { size() }.thenReturn(2)
            }

            assertThat(underTest.invoke(nodes).size).isEqualTo(1)
        }
}