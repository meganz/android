package mega.privacy.android.app.presentation.node.label

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeLabelBottomSheetViewModelTest {
    private val changeLabelUseCase = mock<UpdateNodeLabelUseCase>()
    private val getNodeLabelListUseCase = mock<GetNodeLabelListUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val nodeLabelMapper = NodeLabelMapper()
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
                    NodeLabel.YELLLOW,
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
            nodeLabelMapper,
            nodeLabelResourceMapper,
        )
    }


    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @Test
    fun `test that changeLabelUseCase is invoked when the label changes`() = runTest {
        val nodeId = NodeId(1L)
        val selectedLabel = NodeLabel.RED
        underTest.loadLabelInfo(node)
        underTest.onLabelSelected(selectedLabel)
        verify(changeLabelUseCase).invoke(nodeId, selectedLabel)
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }

    @AfterEach
    fun resetMocks() {
        reset(
            changeLabelUseCase,
            getNodeLabelListUseCase
        )
    }
}