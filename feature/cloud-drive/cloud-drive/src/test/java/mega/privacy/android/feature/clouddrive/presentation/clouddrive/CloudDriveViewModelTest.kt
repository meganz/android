package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CloudDriveViewModelTest {

    private lateinit var underTest: CloudDriveViewModel

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase = mock()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val testNodeHandle = 123L
    private val testNodeId = NodeId(testNodeHandle)
    private var savedStateHandle: SavedStateHandle = SavedStateHandle.Companion.invoke(
        route = CloudDrive(testNodeHandle)
    )

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        runBlocking {
            commonStub()
        }
        underTest = CloudDriveViewModel(
            getNodeByIdUseCase = getNodeByIdUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            savedStateHandle = savedStateHandle,
            defaultDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            getNodeByIdUseCase,
            getFileBrowserNodeChildrenUseCase,
            durationInSecondsTextMapper,
            fileTypeIconMapper
        )
    }

    private suspend fun commonStub() {
        // Skip initial loading scenario till mapper is created
        whenever(getNodeByIdUseCase(eq(testNodeId)))
            .thenThrow(IllegalArgumentException("Node not found"))
        whenever(getFileBrowserNodeChildrenUseCase(testNodeHandle))
            .thenReturn(emptyList())
    }

    @Test
    fun `test that initial state is set correctly`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.currentFolderId).isEqualTo(testNodeId)
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.items).isEmpty()
            assertThat(initialState.selectedItems).isEmpty()
            assertThat(initialState.isInSelectionMode).isFalse()
            assertThat(initialState.navigateToFolderEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that onItemClicked triggers navigation event for folder`() = runTest {
        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node } doReturn folderNode
            on { id } doReturn testNodeId
        }

        underTest.onItemClicked(nodeUiItem)

        underTest.uiState.test {
            val updatedState = awaitItem()

            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(testNodeId))
        }
    }


    @Test
    fun `test that onNavigateToFolderEventConsumed consumes the navigation event`() = runTest {
        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node } doReturn folderNode
            on { id } doReturn testNodeId
        }
        underTest.onItemClicked(nodeUiItem)
        underTest.uiState.test {
            val stateAfterClick = awaitItem()
            underTest.onNavigateToFolderEventConsumed()
            val stateAfterConsume = awaitItem()

            assertThat(stateAfterClick.navigateToFolderEvent).isEqualTo(triggered(testNodeId))
            assertThat(stateAfterConsume.navigateToFolderEvent).isEqualTo(consumed())
        }
    }
} 