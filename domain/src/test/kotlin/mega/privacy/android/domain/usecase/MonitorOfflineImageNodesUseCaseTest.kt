package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.imagepreview.mapper.OfflineFileInformationToImageNodeMapper
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOfflineImageNodesUseCaseTest {
    private lateinit var underTest: MonitorOfflineImageNodesUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val photosRepository = mock<PhotosRepository>()
    private val getOfflineFileInformationByIdUseCase = mock<GetOfflineFileInformationByIdUseCase>()
    private val offlineFileInformationToImageNodeMapper =
        mock<OfflineFileInformationToImageNodeMapper>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorOfflineImageNodesUseCase(
            nodeRepository,
            photosRepository,
            getOfflineFileInformationByIdUseCase,
            offlineFileInformationToImageNodeMapper
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        nodeRepository,
        photosRepository,
        getOfflineFileInformationByIdUseCase,
        offlineFileInformationToImageNodeMapper
    )

    @Test
    fun `test that image nodes are returned when offline nodes are present`() = runTest {
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "testPath",
            name = "test",
            parentId = 0,
            type = "0",
            origin = 0,
            handleIncoming = "0"
        )
        val imageNode = mock<ImageNode>()
        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(flowOf(listOf(offlineNode)))
        whenever(photosRepository.fetchImageNode(NodeId(123L), false)).thenReturn(imageNode)

        val result = underTest("testPath").first()
        assertThat(result).containsExactly(imageNode)
    }

    @Test
    fun `test that empty list is returned when no offline nodes are present`() = runTest {
        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(flowOf(emptyList()))

        val result = underTest("testPath").first()
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that nodes are filtered when path doesn't match`() = runTest {
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "testPath",
            name = "test",
            parentId = 0,
            type = "0",
            origin = 0,
            handleIncoming = "0"
        )
        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(flowOf(listOf(offlineNode)))

        val result = underTest("testPath2").first()
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that nodes are filtered when type is folder`() = runTest {
        val offlineNode = Offline(
            id = 1,
            handle = "123",
            path = "testPath",
            name = "test",
            parentId = 0,
            type = Offline.FOLDER,
            origin = 0,
            handleIncoming = "0"
        )
        whenever(nodeRepository.monitorOfflineNodeUpdates()).thenReturn(flowOf(listOf(offlineNode)))

        val result = underTest("testPath").first()
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that image node is mapped from offline file information when fetchImageNode returns null`() =
        runTest {
            val offlineNode = Offline(
                id = 1,
                handle = "123",
                path = "testPath",
                name = "test",
                parentId = 0,
                type = Offline.FILE,
                origin = 0,
                handleIncoming = "0"
            )
            val imageNode = mock<ImageNode>()
            val offlineFileInfo = mock<OfflineFileInformation>()
            whenever(nodeRepository.monitorOfflineNodeUpdates())
                .thenReturn(flowOf(listOf(offlineNode)))
            whenever(photosRepository.fetchImageNode(NodeId(123L), false)).thenReturn(null)
            whenever(getOfflineFileInformationByIdUseCase(NodeId(123L), true))
                .thenReturn(offlineFileInfo)
            whenever(offlineFileInformationToImageNodeMapper(offlineFileInfo, false))
                .thenReturn(imageNode)

            val result = underTest("testPath", filterSvg = false).first()
            assertThat(result).contains(imageNode)
        }
}