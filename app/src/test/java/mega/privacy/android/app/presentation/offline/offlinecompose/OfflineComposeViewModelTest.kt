package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineComposeViewModelTest {
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase = mock()
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private lateinit var underTest: OfflineComposeViewModel

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = OfflineComposeViewModel(
            savedStateHandle = savedStateHandle,
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            setOfflineWarningMessageVisibilityUseCase = setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase = monitorOfflineWarningMessageVisibilityUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorViewType = monitorViewType,
            monitorConnectivityUseCase = monitorConnectivityUseCase
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the online status is updated correctly`(isOnline: Boolean) = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(isOnline)

        initViewModel()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(isOnline)
        }
    }

    @Test
    fun `test that dismissOfflineWarning calls setOfflineWarningMessageVisibilityUseCase`() =
        runTest {
            underTest.dismissOfflineWarning()
            verify(setOfflineWarningMessageVisibilityUseCase).invoke(false)
        }

    @Test
    fun `test that folder is clicked then it shows child list`() = runTest {
        val parentId = 1
        val offlineList1 = mock<OfflineFileInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.addedTime).thenReturn(100000L)

        val offlineList2 = mock<OfflineFileInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.addedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)

        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInformation(
                    id = parentId,
                    parentId = 0,
                    name = "Sample",
                    isFolder = true,
                    handle = "1234",
                    lastModifiedTime = 100000L,
                    path = ""
                ),
                isSelected = false
            ),
            rootFolderOnly = false
        )
        assertThat(underTest.uiState.value.offlineNodes).hasSize(2)
    }

    @Test
    fun `test that navigation even is sent when folder is clicked from homepage`() = runTest {
        val item = OfflineNodeUIItem(
            offlineNode = OfflineFileInformation(
                id = 1,
                parentId = 0,
                name = "Sample",
                isFolder = true,
                handle = "1234",
                lastModifiedTime = 100000L,
                path = "",
                absolutePath = ""
            ),
            isSelected = false
        )

        underTest.onItemClicked(
            offlineNodeUIItem = item,
            rootFolderOnly = true
        )

        underTest.uiState.test {
            val newItem = awaitItem()
            assertThat(((newItem.openFolderInPageEvent as? StateEventWithContentTriggered<*>)?.content as? OfflineFileInformation))
                .isEqualTo(item.offlineNode)
        }

    }

    @Test
    fun `test that its parent id is called when back clicked`() = runTest {
        val parentId = 1
        val offlineList1 = mock<OfflineFileInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.addedTime).thenReturn(100000L)

        val offlineList2 = mock<OfflineFileInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.addedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(any(), anyOrNull())).thenReturn(list)
        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInformation(
                    id = parentId,
                    parentId = 0,
                    name = "Sample",
                    isFolder = true,
                    handle = "1234",
                    lastModifiedTime = 100000L,
                    path = ""
                ),
                isSelected = false
            ),
            rootFolderOnly = false
        )

        underTest.onBackClicked()
        verify(getOfflineNodesByParentIdUseCase).invoke(parentId, null)
    }

    @Test
    fun `test that the selected node size is equal to the total offline list size when select all is clicked`() =
        runTest {
            val parentId = 1
            val offlineList1 = mock<OfflineFileInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.addedTime).thenReturn(100000L)

            val offlineList2 = mock<OfflineFileInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.addedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUIItem(
                    offlineNode = OfflineFileInformation(
                        id = parentId,
                        parentId = 0,
                        name = "Sample",
                        isFolder = true,
                        handle = "1234",
                        lastModifiedTime = 100000L,
                        path = ""
                    ),
                    isSelected = false
                ),
                rootFolderOnly = false
            )
            underTest.selectAll()
            assertThat(underTest.uiState.value.offlineNodes.size).isEqualTo(underTest.uiState.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that the total selected node size is empty when clear all is clicked`() =
        runTest {
            val parentId = 1
            val offlineList1 = mock<OfflineFileInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.addedTime).thenReturn(100000L)

            val offlineList2 = mock<OfflineFileInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.addedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUIItem(
                    offlineNode = OfflineFileInformation(
                        id = parentId,
                        parentId = 0,
                        name = "Sample",
                        isFolder = true,
                        handle = "1234",
                        lastModifiedTime = 100000L,
                        path = ""
                    ),
                    isSelected = false
                ),
                rootFolderOnly = false
            )
            underTest.clearSelection()
            assertThat(underTest.uiState.value.selectedNodeHandles.size).isEqualTo(0)
        }

    @Test
    fun `test that the list is updated when the offline item is long clicked`() = runTest {
        val parentId = 1
        val offlineList1 = mock<OfflineFileInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.addedTime).thenReturn(100000L)

        val offlineList2 = mock<OfflineFileInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.addedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInformation(
                    id = parentId,
                    parentId = 0,
                    name = "Sample",
                    isFolder = true,
                    handle = "1234",
                    lastModifiedTime = 100000L,
                    path = ""
                ),
                isSelected = false
            ),
            rootFolderOnly = false
        )
        underTest.onLongItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInformation(
                    id = 1,
                    handle = "1234",
                    lastModifiedTime = 100000L,
                    path = ""
                ),
            )
        )
        assertThat(underTest.uiState.value.selectedNodeHandles.size).isEqualTo(1)
    }

    @Test
    fun `test that navigateToPath navigates to children if found`() = runTest {
        val parentId = -1
        val childId = 3453
        val grandChildId = 845
        val child = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "folder"
            on { id } doReturn childId
        }
        val grandChild = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "subFolder"
            on { id } doReturn grandChildId
        }
        val path = File.separator + child.name + File.separator + grandChild.name + File.separator

        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(listOf(child))
        whenever(getOfflineNodesByParentIdUseCase(childId)).thenReturn(listOf(grandChild))
        whenever(getOfflineNodesByParentIdUseCase(grandChildId)).thenReturn(listOf(mock()))

        underTest.navigateToPath(
            path = path,
            rootFolderOnly = false
        )
        assertThat(underTest.uiState.value.parentId).isEqualTo(grandChildId)
    }

    @Test
    fun `test that navigateToPath set highlighted elements correctly`() = runTest {
        val parentId = -1
        val childId = 3453
        val folder = mock<OfflineFileInformation> {
            on { isFolder } doReturn true
            on { name } doReturn "folder"
            on { id } doReturn childId
        }
        val highlighted = mock<OfflineFileInformation> {
            on { isFolder } doReturn false
            on { name } doReturn "file1"
            on { id } doReturn 564
        }
        val file2 = mock<OfflineFileInformation> {
            on { isFolder } doReturn false
            on { name } doReturn "file2"
            on { id } doReturn 63456
        }
        val path = File.separator + folder.name + File.separator
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(listOf(folder))
        whenever(getOfflineNodesByParentIdUseCase(childId)).thenReturn(listOf(highlighted, file2))

        underTest.navigateToPath(
            path = path,
            rootFolderOnly = false,
            fileNames = arrayOf(highlighted.name)
        )
        assertThat(underTest.uiState.value.offlineNodes.find { it.isHighlighted }?.offlineNode)
            .isEqualTo(highlighted)
        assertThat(underTest.uiState.value.offlineNodes.find { !it.isHighlighted }?.offlineNode)
            .isEqualTo(file2)
    }

    private suspend fun stubCommon() {
        whenever(getOfflineNodesByParentIdUseCase(-1)).thenReturn(emptyList())
        whenever(setOfflineWarningMessageVisibilityUseCase(false)).thenReturn(Unit)
        whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(emptyFlow())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
    }

    @AfterEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getOfflineNodesByParentIdUseCase,
            monitorOfflineWarningMessageVisibilityUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorViewType
        )
    }
}