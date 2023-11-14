package test.mega.privacy.android.app.presentation.backups

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.backups.BackupsFragment
import mega.privacy.android.app.presentation.backups.BackupsViewModel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.viewtype.FakeMonitorViewType
import nz.mega.sdk.MegaNode
import org.junit.Rule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.domain.usecase.FakeMonitorBackupFolder
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.stream.Stream

/**
 * Test class for [BackupsViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupsViewModelTest {
    private lateinit var underTest: BackupsViewModel

    private val getChildrenNode = mock<GetChildrenNode>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeHandle = mock<GetParentNodeHandle>()

    private val monitorBackupFolder = FakeMonitorBackupFolder()
    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val monitorViewType = FakeMonitorViewType()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getChildrenNode,
            getCloudSortOrder,
            getNodeByHandle,
            getParentNodeHandle,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setUnderTest(originalBackupsHandle: Long = -1L) {
        val savedStateHandle = SavedStateHandle(
            mapOf(BackupsFragment.PARAM_BACKUPS_HANDLE to originalBackupsHandle)
        )
        underTest = BackupsViewModel(
            getChildrenNode = getChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getNodeByHandle = getNodeByHandle,
            getParentNodeHandle = getParentNodeHandle,
            monitorBackupFolder = monitorBackupFolder,
            monitorNodeUpdates = monitorNodeUpdates,
            monitorViewType = monitorViewType,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `test that the backups content is populated when the original backups node handle is set`() =
        runTest {
            val originalBackupsHandle = 123456L
            val originalBackupsNodeId = NodeId(originalBackupsHandle)
            val backupsNode = mock<MegaNode> {
                on { name }.thenReturn("Parent Node Name")
            }
            val childBackupsNodeList = listOf(mock<MegaNode>())

            whenever(getNodeByHandle(any())).thenReturn(backupsNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = backupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                childBackupsNodeList
            )

            setUnderTest(originalBackupsHandle = originalBackupsHandle)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.originalBackupsNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.nodes).isEqualTo(childBackupsNodeList)
            }
        }

    @Test
    fun `test that the original and current backups node ids are still set when an exception occurs from populating the backups content`() =
        runTest {
            val originalBackupsHandle = 123456L
            val originalBackupsNodeId = NodeId(originalBackupsHandle)

            whenever(getNodeByHandle(any())).thenThrow(RuntimeException())

            setUnderTest(originalBackupsHandle = originalBackupsHandle)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.originalBackupsNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.nodes).isEmpty()
            }
        }

    @Test
    fun `test that the current backups folder name is set when refreshing the backup nodes`() =
        runTest {
            val parentBackupsNodeName = "Parent Node Name"
            val originalBackupsHandle = 123456L
            val parentBackupsNode = mock<MegaNode> {
                on { handle }.thenReturn(originalBackupsHandle)
                on { name }.thenReturn(parentBackupsNodeName)
            }

            whenever(getNodeByHandle(any())).thenReturn(parentBackupsNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = parentBackupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(emptyList())

            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            monitorBackupFolder.emit(Result.success(NodeId(789012L)))
            underTest.refreshBackupsNodes()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderName).isEqualTo(parentBackupsNodeName)
            }
        }

    @Test
    fun `test that the current backups folder name is null when refreshing the backup nodes and the parent node is null`() =
        runTest {
            whenever(getNodeByHandle(any())).thenReturn(null)

            setUnderTest(originalBackupsHandle = 123456L)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderName).isNull()
            }
        }

    @Test
    fun `test that the current backups folder name is null when refreshing the backup nodes and the user is in the root backups folder level`() =
        runTest {
            val originalBackupsHandle = 123456L
            val parentBackupsNode = mock<MegaNode> {
                on { handle }.thenReturn(originalBackupsHandle)
                on { name }.thenReturn("Parent Node Name")
            }

            whenever(getNodeByHandle(any())).thenReturn(parentBackupsNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = parentBackupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(emptyList())

            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            monitorBackupFolder.emit(Result.success(NodeId(originalBackupsHandle)))
            underTest.refreshBackupsNodes()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderName).isNull()
            }
        }

    @Test
    fun `test that a pending node refresh occurs when a node update is received`() = runTest {
        setUnderTest()
        monitorNodeUpdates.emit(NodeUpdate(mapOf()))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isPendingRefresh).isTrue()
        }
    }

    @Test
    fun `test that the user root backups folder node id is set when an update is received`() =
        runTest {
            val updatedRootBackupsFolderNodeId = NodeId(123456L)

            setUnderTest()
            monitorBackupFolder.emit(Result.success(updatedRootBackupsFolderNodeId))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.rootBackupsFolderNodeId).isEqualTo(updatedRootBackupsFolderNodeId)
            }
        }

    @Test
    fun `test that the view type is set when an update is received`() = runTest {
        val updatedViewType = ViewType.GRID

        setUnderTest()
        monitorViewType.emit(updatedViewType)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentViewType).isEqualTo(ViewType.GRID)
        }
    }

    @Test
    fun `test that calling refreshBackupNodesAndHideSelection will refresh the backups content and hide the multiple item selection`() =
        runTest {
            val originalBackupsHandle = 123456L
            val backupsNode = mock<MegaNode> {
                on { name }.thenReturn("Parent Node Name")
                on { handle }.thenReturn(originalBackupsHandle)
            }
            val childBackupsNodeList = listOf(mock<MegaNode>())

            // Prepare values for when the ViewModel is initialized
            whenever(getNodeByHandle(any())).thenReturn(backupsNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = backupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                childBackupsNodeList
            )

            setUnderTest(originalBackupsHandle = originalBackupsHandle)

            val updatedChildBackupsNodeList = listOf(mock<MegaNode>())

            // Prepare values for when the ViewModel function gets called
            whenever(
                getChildrenNode(
                    parent = backupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                updatedChildBackupsNodeList
            )

            underTest.refreshBackupsNodesAndHideSelection()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hideMultipleItemSelection).isTrue()
                assertThat(state.nodes).isEqualTo(updatedChildBackupsNodeList)
            }
        }

    @Test
    fun `test that calling updateBackupsHandle updates the current backups folder node id`() =
        runTest {
            val updatedBackupsHandle = 123456L

            setUnderTest()
            underTest.updateBackupsHandle(updatedBackupsHandle)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(NodeId(updatedBackupsHandle))
            }
        }

    @Test
    fun `test that calling getCurrentBackupsFolderHandle returns the current backups folder node handle`() =
        runTest {
            val originalBackupsHandle = 123456L
            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            assertThat(underTest.getCurrentBackupsFolderHandle()).isEqualTo(originalBackupsHandle)
        }

    @ParameterizedTest(name = "when the current backups handle is {0} and the root backups folder handle is {1}, then is user in root backups folder level is {2}")
    @ArgumentsSource(RootBackupsFolderLevelTestArgumentsSource::class)
    fun `test that the user could be in the root backups folder level`(
        currentBackupsHandle: Long,
        rootBackupsFolderHandle: Long,
        isUserInRootBackupsFolderLevel: Boolean,
    ) = runTest {
        setUnderTest(originalBackupsHandle = currentBackupsHandle)
        monitorBackupFolder.emit(Result.success(NodeId(rootBackupsFolderHandle)))
        assertThat(underTest.isUserInRootBackupsFolderLevel()).isEqualTo(
            isUserInRootBackupsFolderLevel
        )
    }

    /**
     * The implementation of the [ArgumentsProvider] to test if the User is in the Root Backups
     * Folder or not
     */
    private class RootBackupsFolderLevelTestArgumentsSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> = Stream.of(
            Arguments.of(123456L, 123456L, true),
            Arguments.of(789012L, 123456L, false),
        )
    }

    @Test
    fun `test that calling exitBackupsHandled resets the should exit backups condition to false`() =
        runTest {
            setUnderTest()
            underTest.exitBackupsHandled()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldExitBackups).isFalse()
            }
        }

    @Test
    fun `test that calling triggerBackPressHandled resets the trigger back press condition to false`() =
        runTest {
            setUnderTest()
            underTest.triggerBackPressHandled()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.triggerBackPress).isFalse()
            }
        }

    @Test
    fun `test that calling hideMultipleItemSelectionHandled resets the hide multiple item selection condition to false`() =
        runTest {
            setUnderTest()
            underTest.hideMultipleItemSelectionHandled()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.hideMultipleItemSelection).isFalse()
            }
        }

    @Test
    fun `test that calling getOrder returns the cloud sort order`() = runTest {
        val expected = SortOrder.ORDER_SIZE_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)

        setUnderTest()

        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    @Test
    fun `test that calling markHandledPendingRefresh resets the pending node refresh condition to false`() =
        runTest {
            setUnderTest()
            underTest.markHandledPendingRefresh()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isPendingRefresh).isFalse()
            }
        }

    @Test
    fun `test that the user exits the backups page when doing a back navigation and the user is in the original backups folder level`() =
        runTest {
            val originalBackupsHandle = 123456L
            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(NodeId(originalBackupsHandle))
                assertThat(state.shouldExitBackups).isTrue()
            }
        }

    @Test
    fun `test that the user exits the backups page when doing a back navigation and the parent backups node of the current backups node id is null`() =
        runTest {
            val originalBackupsHandle = 123456L
            val newCurrentBackupsFolderNodeId = 789012L

            whenever(getParentNodeHandle(any())).thenReturn(null)

            // On ViewModel initialization, set the Original Backups Node ID to the current Backups Node ID
            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            // Change the current Backups Node ID
            underTest.updateBackupsHandle(newCurrentBackupsFolderNodeId)
            // User does a Back navigation after the current Backups Node ID has been updated
            underTest.handleBackPress()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentBackupsFolderNodeId).isNotEqualTo(
                    NodeId(
                        originalBackupsHandle
                    )
                )
                assertThat(state.shouldExitBackups).isTrue()
            }
        }

    @Test
    fun `test that the user goes back to the previous backups folder level when doing a back navigation`() =
        runTest {
            val originalBackupsHandle = 123456L
            val originalBackupsNodeId = NodeId(originalBackupsHandle)
            val newCurrentBackupsFolderNodeId = 789012L

            val backupsNode = mock<MegaNode> {
                on { name }.thenReturn("Parent Node Name")
            }
            val childBackupsNodeList = listOf(mock<MegaNode>())

            // Prepare values for when the ViewModel is initialized
            whenever(getNodeByHandle(any())).thenReturn(backupsNode)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                getChildrenNode(
                    parent = backupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                childBackupsNodeList
            )

            // On ViewModel initialization, set the Original Backups Node ID to the current Backups Node ID
            setUnderTest(originalBackupsHandle = originalBackupsHandle)
            underTest.state.test {
                // Verify the state values after initializing the ViewModel
                val state = awaitItem()
                assertThat(state.originalBackupsNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(originalBackupsNodeId)
                assertThat(state.nodes).isEqualTo(childBackupsNodeList)
            }

            val updatedBackupsNode = mock<MegaNode> {
                on { name }.thenReturn("Updated Backups Node Name")
            }
            val updatedChildBackupsNodeList = listOf(mock<MegaNode>())

            // Prepare values for when the Backups Nodes get refreshed
            whenever(getNodeByHandle(any())).thenReturn(updatedBackupsNode)
            whenever(
                getChildrenNode(
                    parent = updatedBackupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                updatedChildBackupsNodeList
            )

            // Change the current Backups Node ID and refresh the Backups Nodes
            underTest.updateBackupsHandle(newCurrentBackupsFolderNodeId)
            underTest.refreshBackupsNodes()
            underTest.state.test {
                // Verify the state values after setting the new Current Backups Node ID and
                // refreshing the Backups Nodes
                val state = awaitItem()
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(
                    NodeId(newCurrentBackupsFolderNodeId)
                )
                assertThat(state.nodes).isEqualTo(updatedChildBackupsNodeList)
            }

            // Prepare values for when the users performs a Back Navigation
            // The content should be the same as when the ViewModel was initialized
            whenever(getParentNodeHandle(any())).thenReturn(originalBackupsHandle)
            whenever(getNodeByHandle(any())).thenReturn(backupsNode)
            whenever(
                getChildrenNode(
                    parent = backupsNode,
                    order = getCloudSortOrder(),
                )
            ).thenReturn(
                childBackupsNodeList
            )

            // User performs a Back navigation
            underTest.handleBackPress()
            underTest.state.test {
                // Verify the state values after performing a Back Navigation. The content should be
                // the same as when the ViewModel was initialized
                val state = awaitItem()
                assertThat(state.currentBackupsFolderNodeId).isEqualTo(NodeId(originalBackupsHandle))
                assertThat(state.nodes).isEqualTo(childBackupsNodeList)
                assertThat(state.triggerBackPress).isTrue()
            }
        }
}