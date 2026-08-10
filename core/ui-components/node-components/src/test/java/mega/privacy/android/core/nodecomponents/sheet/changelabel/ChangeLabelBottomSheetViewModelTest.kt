package mega.privacy.android.core.nodecomponents.sheet.changelabel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeLabelResourceMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeLabelBottomSheetViewModelTest {
    private val changeLabelUseCase = mock<UpdateNodeLabelUseCase>()
    private val getNodeLabelListUseCase = mock<GetNodeLabelListUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getNodeLabelUseCase = mock<GetNodeLabelUseCase>()
    private val nodeLabelResourceMapper: NodeLabelResourceMapper = NodeLabelResourceMapper()
    private lateinit var underTest: ChangeLabelBottomSheetViewModel
    private val node = mock<TypedFolderNode> {
        on { name }.thenReturn("name")
        on { childFileCount }.thenReturn(1)
        on { childFolderCount }.thenReturn(1)
        on { label }.thenReturn(1)
        on { id }.thenReturn(NodeId(1L))
    }

    fun initViewModel() {
        runBlocking {
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
            whenever(getNodeLabelListUseCase()).thenReturn(
                listOf(
                    NodeLabel.RED,
                    NodeLabel.ORANGE,
                    NodeLabel.YELLOW,
                    NodeLabel.GREEN,
                    NodeLabel.BLUE,
                    NodeLabel.PURPLE,
                    NodeLabel.GREY
                )
            )
        }
        underTest = ChangeLabelBottomSheetViewModel(
            changeLabelUseCase,
            getNodeLabelListUseCase,
            getNodeLabelUseCase,
            nodeLabelResourceMapper,
            getNodeByIdUseCase
        )
    }


    @BeforeAll
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `test that changeLabelUseCase is invoked when the label changes`() = runTest {
        val nodeId = NodeId(1L)
        val selectedLabel = NodeLabel.RED
        underTest.loadLabelInfo(nodeId)
        underTest.onLabelSelected(selectedLabel)
        verify(changeLabelUseCase).invoke(nodeId, selectedLabel)
    }

    @Test
    fun `test that loadLabelInfo with multiple nodeIds loads label list using first node`() =
        runTest {
            val nodeIds = listOf(NodeId(1L), NodeId(2L))
            val node2 = mock<TypedFolderNode> {
                on { name }.thenReturn("name2")
                on { childFileCount }.thenReturn(0)
                on { childFolderCount }.thenReturn(0)
                on { label }.thenReturn(2)
                on { id }.thenReturn(NodeId(2L))
            }
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
            whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(node2)
            whenever(getNodeLabelListUseCase()).thenReturn(
                listOf(
                    NodeLabel.RED,
                    NodeLabel.ORANGE,
                    NodeLabel.YELLOW,
                    NodeLabel.GREEN,
                    NodeLabel.BLUE,
                    NodeLabel.PURPLE,
                    NodeLabel.GREY
                )
            )
            underTest.loadLabelInfo(nodeIds)
            assertThat(underTest.state.value.nodeIds).isEqualTo(nodeIds)
            assertThat(underTest.state.value.labelList).isNotEmpty()
        }

    @Test
    fun `test that onLabelSelected with multiple nodeIds invokes changeLabelUseCase for each node`() =
        runTest {
            val nodeIds = listOf(NodeId(1L), NodeId(2L))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
            whenever(getNodeLabelListUseCase()).thenReturn(
                listOf(
                    NodeLabel.RED,
                    NodeLabel.ORANGE,
                    NodeLabel.YELLOW,
                    NodeLabel.GREEN,
                    NodeLabel.BLUE,
                    NodeLabel.PURPLE,
                    NodeLabel.GREY
                )
            )
            underTest.loadLabelInfo(nodeIds)
            underTest.onLabelSelected(NodeLabel.GREEN)
            advanceUntilIdle()
            verify(changeLabelUseCase).invoke(NodeId(1L), NodeLabel.GREEN)
            verify(changeLabelUseCase).invoke(NodeId(2L), NodeLabel.GREEN)
        }

    @Test
    fun `test that loadLabelInfo with empty nodeIds does not update state`() = runTest {
        val stateBefore = underTest.state.value
        underTest.loadLabelInfo(emptyList())
        assertThat(underTest.state.value).isEqualTo(stateBefore)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            changeLabelUseCase,
            getNodeLabelListUseCase
        )
    }
}
