package mega.privacy.android.app.presentation.node.label

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetRouteNodeIdArg
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
    private val stateHandle = mock<SavedStateHandle>()
    private val nodeLabelResourceMapper: NodeLabelResourceMapper = NodeLabelResourceMapper()
    private lateinit var underTest: ChangeLabelBottomSheetViewModel
    private val node = mock<TypedFolderNode> {
        on { name }.thenReturn("name")
        on { childFileCount }.thenReturn(1)
        on { childFolderCount }.thenReturn(1)
        on { label }.thenReturn(1)
    }

    fun initViewModel() {
        runBlocking {
            whenever(stateHandle.get<Long>(changeLabelBottomSheetRouteNodeIdArg)).thenReturn(1L)
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
            getNodeByIdUseCase,
            stateHandle
        )
    }


    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @Test
    fun `test that when label change it calls changeLabelUseCase`() = runTest {
        val nodeId = NodeId(1L)
        val selectedLabel = NodeLabel.RED
        underTest.onLabelSelected(selectedLabel)
        verify(changeLabelUseCase).invoke(nodeId, selectedLabel)
    }

    @Test
    fun `test that when getNodeLabel list called it fills new list`() = runTest {
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