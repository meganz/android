package mega.privacy.android.app.presentation.node.label

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
import nz.mega.sdk.MegaNode
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
    private val changeLabelUseCase: UpdateNodeLabelUseCase = mock()
    private val getNodeLabelListUseCase: GetNodeLabelListUseCase = mock()
    private val nodeLabelMapper = NodeLabelMapper()
    private val nodeLabelResourceMapper: NodeLabelResourceMapper = NodeLabelResourceMapper()

    private val underTest: ChangeLabelBottomSheetViewModel = ChangeLabelBottomSheetViewModel(
        changeLabelUseCase = changeLabelUseCase,
        getNodeLabelListUseCase = getNodeLabelListUseCase,
        nodeLabelMapper = nodeLabelMapper,
        nodeLabelResourceMapper = nodeLabelResourceMapper
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that when label change it calls changeLabelUseCase`() = runTest {
        val nodeId = NodeId(1L)
        val selectedLabel = NodeLabel.RED
        underTest.onLabelSelected(nodeId, selectedLabel)
        verify(changeLabelUseCase).invoke(nodeId, selectedLabel)
    }

    @Test
    fun `test that when getNodeLabel list called it fills new list`() = runTest {
        val fileNode: FileNode = mock {
            whenever(it.label).thenReturn(MegaNode.NODE_LBL_RED)
        }
        whenever(getNodeLabelListUseCase()).thenReturn(buildList {
            add(NodeLabel.RED)
            add(NodeLabel.ORANGE)
            add(NodeLabel.YELLLOW)
            add(NodeLabel.GREEN)
            add(NodeLabel.BLUE)
            add(NodeLabel.PURPLE)
            add(NodeLabel.GREY)
        })
        underTest.loadLabelInfo(fileNode)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.labelList).isNotEmpty()
        }
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