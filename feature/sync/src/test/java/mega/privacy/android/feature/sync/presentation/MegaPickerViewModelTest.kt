package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.SetSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerState
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaPickerViewModelTest {

    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()

    private val childrenNodes: List<TypedNode> = mock()

    private lateinit var underTest: MegaPickerViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()

    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(
            setSelectedMegaFolderUseCase,
            getRootNodeUseCase,
            getTypedNodesFromFolder,
            getNodeByHandleUseCase
        )
    }

    @Test
    fun `test that viewmodel fetches root folder and its children upon initialization`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            emit(childrenNodes)
        })
        initViewModel()
        val expectedState = MegaPickerState(
            currentFolder = rootFolder, nodes = childrenNodes
        )

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that folder click fetches children of clicked folder`() = runTest {
        initViewModel()
        val clickedFolderId = NodeId(9845748L)
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }
        whenever(getTypedNodesFromFolder(clickedFolderId)).thenReturn(flow {
            emit(childrenNodes)
        })
        val expectedState = MegaPickerState(
            currentFolder = clickedFolder, nodes = childrenNodes
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that back click fetches children of parent folder`() = runTest {
        val currentFolderId = NodeId(43434L)
        val parentFolderId = NodeId(9845748L)
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { parentId } doReturn parentFolderId
        }
        val parentFolder: TypedFolderNode = mock {
            on { id } doReturn parentFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
        })
        whenever(getNodeByHandleUseCase(parentFolderId.longValue)).thenReturn(parentFolder)
        whenever(getTypedNodesFromFolder(parentFolderId)).thenReturn(flow {
            emit(childrenNodes)
        })
        val expectedState = MegaPickerState(
            currentFolder = parentFolder, nodes = childrenNodes
        )
        initViewModel()

        underTest.handleAction(MegaPickerAction.BackClicked)

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that folder selection sets selected folder`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some secret folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
        })
        initViewModel()

        underTest.handleAction(MegaPickerAction.CurrentFolderSelected)

        verify(setSelectedMegaFolderUseCase).invoke(
            RemoteFolder(
                currentFolderId.longValue,
                currentFolderName
            )
        )
    }

    private fun initViewModel() {
        underTest = MegaPickerViewModel(
            setSelectedMegaFolderUseCase,
            getRootNodeUseCase,
            getTypedNodesFromFolder,
            getNodeByHandleUseCase
        )
    }
}