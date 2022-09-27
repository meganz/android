package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.domain.usecase.DefaultGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
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
    private val mockNodes = (0L..5L).map { id ->
        mock<MegaNode> { on { handle }.thenReturn(id) }
    }
    private val nodesList = mock<MegaNodeList> {
        on { size() }.thenReturn(mockNodes.size)
        on { get(any()) }.thenAnswer { mockNodes[it.arguments[0] as Int] }
    }
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper> {
        on { invoke(any()) }.thenReturn(
            VideoFileTypeInfo("", ""))
    }

    @Before
    fun setUp() {
        underTest = DefaultGetRecentActionNodes(
            getThumbnail = getThumbnail,
            ioDispatcher = UnconfinedTestDispatcher(),
            fileTypeInfoMapper = fileTypeInfoMapper,
        )
    }

    @Test
    fun `test that if getThumbnail succeed for each element,the list returned contains as many elements as the list given in parameter`() =
        runTest {
            assertThat(underTest.invoke(nodesList).size).isEqualTo(nodesList.size())
        }


    @Test
    fun `test that if one of getThumbnail throws an exception when looping over the nodes, under test stills returns the list of nodes except the one who failed`() =
        runTest {
            whenever(getThumbnail(3L)).thenAnswer {
                throw IOException("Error!")
            }

            assertThat(underTest.invoke(nodesList).size).isEqualTo(mockNodes.size - 1)
        }
}