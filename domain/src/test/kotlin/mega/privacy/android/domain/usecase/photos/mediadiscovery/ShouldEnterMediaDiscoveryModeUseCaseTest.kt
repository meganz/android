package mega.privacy.android.domain.usecase.photos.mediadiscovery

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldEnterMediaDiscoveryModeUseCaseTest {
    private lateinit var underTest: ShouldEnterMediaDiscoveryModeUseCase

    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ShouldEnterMediaDiscoveryModeUseCase(
            getRootNodeUseCase = getRootNodeUseCase,
            getCloudSortOrder = getCloudSortOrder,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(getRootNodeUseCase, getCloudSortOrder, nodeRepository)
    }

    @ParameterizedTest(name = "when node's isMarkedSensitive is {0}")
    @MethodSource("provideSensitiveAndCheckSensitive")
    fun `test that media discovery cannot be entered parentHandle is invalid`(
        isSensitive: Boolean,
        isCheckSensitive: Boolean
    ) = runTest {
        val parentHandle = -1L
        val imageNode = getImageNode(isSensitive)
        val nodes = listOf(imageNode)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getTypedNodesById(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder(),
                null
            )
        ).thenReturn(nodes)
        val shouldEnter = underTest(parentHandle, isCheckSensitive)
        assertThat(shouldEnter).isFalse()
    }

    @ParameterizedTest(name = "when node's isMarkedSensitive is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that media discovery can be entered when there are media nodes and isCheckSensitive is false`(
        isSensitive: Boolean
    ) =
        runTest {
            val parentHandle = 1234L
            val imageNode = getImageNode(isSensitive)
            val nodes = listOf(imageNode)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getTypedNodesById(
                    nodeId = NodeId(parentHandle),
                    getCloudSortOrder(),
                    null
                )
            ).thenReturn(nodes)
            val shouldEnter = underTest(parentHandle, false)
            assertThat(shouldEnter).isTrue()
        }

    @Test
    fun `test that media discovery can be entered when there are media nodes, isCheckSensitive is true, and not all nodes are sensitive`() =
        runTest {
            val parentHandle = 1234L
            val imageNode = getImageNode(true)
            val videoNode = getVideoNode(false)
            val nodes = listOf(imageNode, videoNode)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getTypedNodesById(
                    NodeId(parentHandle),
                    getCloudSortOrder(),
                    null
                )
            ).thenReturn(nodes)
            val shouldEnter = underTest(parentHandle, true)
            assertThat(shouldEnter).isTrue()
        }

    @Test
    fun `test that media discovery cannot be entered when there are media nodes, isCheckSensitive is true, and all nodes are sensitive`() =
        runTest {
            val parentHandle = 1234L
            val imageNode = getImageNode(true)
            val videoNode = getVideoNode(true)
            val nodes = listOf(imageNode, videoNode)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getTypedNodesById(
                    NodeId(parentHandle),
                    getCloudSortOrder(),
                    null
                )
            ).thenReturn(nodes)
            val shouldEnter = underTest(parentHandle, true)
            assertThat(shouldEnter).isFalse()
        }

    @ParameterizedTest(name = "when node's isMarkedSensitive is {0}")
    @MethodSource("provideSensitiveAndCheckSensitive")
    fun `test that media discovery cannot be entered when there is not all media node`(
        isSensitive: Boolean,
        isCheckSensitive: Boolean
    ) = runTest {
        val parentHandle = 1234L
        val audioNode = getAudioNode(isSensitive)
        val imageNode = getImageNode(isSensitive)
        val nodes = listOf(audioNode, imageNode)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getTypedNodesById(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder(),
                null
            )
        ).thenReturn(nodes)
        val shouldEnter = underTest(parentHandle, isCheckSensitive)
        assertThat(shouldEnter).isFalse()
    }

    @ParameterizedTest(name = "when node's isMarkedSensitive is {0}")
    @MethodSource("provideSensitiveAndCheckSensitive")
    fun `test that media discovery cannot be entered when there is a svg file node`(
        isSensitive: Boolean,
        isCheckSensitive: Boolean
    ) = runTest {
        val parentHandle = 1234L
        val svgNode = getSvgNode(isSensitive)
        val imageNode = getImageNode(isSensitive)
        val nodes = listOf(svgNode, imageNode)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getTypedNodesById(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder(),
                null
            )
        ).thenReturn(nodes)
        val shouldEnter = underTest(parentHandle, isCheckSensitive)
        assertThat(shouldEnter).isFalse()
    }

    @ParameterizedTest(name = "when node's isMarkedSensitive is {0}")
    @MethodSource("provideSensitiveAndCheckSensitive")
    fun `test that media discovery cannot be entered when a folder node is found`(
        isSensitive: Boolean,
        isCheckSensitive: Boolean
    ) =
        runTest {
            val parentHandle = 1234L
            val folderNode = mock<TypedFolderNode> {
                on { isMarkedSensitive }.thenReturn(isSensitive)
            }
            val imageNode = getImageNode(isSensitive)
            val videoNode = getVideoNode(isSensitive)
            val nodes = listOf(folderNode, imageNode, videoNode)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getTypedNodesById(
                    nodeId = NodeId(parentHandle),
                    getCloudSortOrder(),
                    null
                )
            ).thenReturn(nodes)
            val shouldEnter = underTest(parentHandle, isCheckSensitive)
            assertThat(shouldEnter).isFalse()
        }

    private fun getImageNode(isSensitive: Boolean) = mock<TypedFileNode> {
        on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        on { isMarkedSensitive }.thenReturn(isSensitive)
    }

    private fun getVideoNode(isSensitive: Boolean) = mock<TypedFileNode> {
        on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
        on { isMarkedSensitive }.thenReturn(isSensitive)
    }

    private fun getAudioNode(isSensitive: Boolean) = mock<TypedFileNode> {
        on { type }.thenReturn(AudioFileTypeInfo("", "", 10.seconds))
        on { isMarkedSensitive }.thenReturn(isSensitive)
    }

    private fun getSvgNode(isSensitive: Boolean) = mock<TypedFileNode> {
        on { type }.thenReturn(SvgFileTypeInfo("", ""))
        on { isMarkedSensitive }.thenReturn(isSensitive)
    }

    private fun provideSensitiveAndCheckSensitive() = listOf(
        Arguments.of(true, true),
        Arguments.of(true, false),
        Arguments.of(false, true),
        Arguments.of(false, false),
    )
}
