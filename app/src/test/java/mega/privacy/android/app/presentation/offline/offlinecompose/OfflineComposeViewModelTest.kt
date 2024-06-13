package mega.privacy.android.app.presentation.offline.offlinecompose

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineComposeViewModelTest {
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase = mock()
    private val monitorTransfersFinishedUseCase: MonitorTransfersFinishedUseCase = mock()
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase =
        mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
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
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            monitorTransfersFinishedUseCase = monitorTransfersFinishedUseCase,
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
            )
        )
        assertThat(underTest.uiState.value.offlineNodes).hasSize(2)
    }

    @Test
    fun `test that when back clicked, it calls its parent id`() = runTest {
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
        whenever(getOfflineNodesByParentIdUseCase(any())).thenReturn(list)
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
            )
        )

        underTest.onBackClicked()
        verify(getOfflineNodesByParentIdUseCase).invoke(parentId)
    }

    @Test
    fun `test that when select all is clicked total selected node size is equal to total offline list size`() =
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
                )
            )
            underTest.selectAll()
            assertThat(underTest.uiState.value.offlineNodes.size).isEqualTo(underTest.uiState.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that when clear all is clicked total selected node size is empty`() =
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
                )
            )
            underTest.clearSelection()
            assertThat(underTest.uiState.value.selectedNodeHandles.size).isEqualTo(0)
        }

    @Test
    fun `test that when long clicked on offline item updates selected list`() = runTest {
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
            )
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

    private suspend fun stubCommon() {
        whenever(getOfflineNodesByParentIdUseCase(-1)).thenReturn(emptyList())
        whenever(monitorTransfersFinishedUseCase()).thenReturn(emptyFlow())
        whenever(setOfflineWarningMessageVisibilityUseCase(false)).thenReturn(Unit)
        whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(emptyFlow())
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineNodesByParentIdUseCase,
            monitorTransfersFinishedUseCase,
            monitorOfflineWarningMessageVisibilityUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorViewType
        )
    }
}