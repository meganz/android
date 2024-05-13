package mega.privacy.android.app.presentation.offline.offlinecompose

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileTotalSizeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderInformationUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

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
    private val offlineFolderInformationUseCase: GetOfflineFolderInformationUseCase = mock()
    private val getOfflineFileUseCase: GetOfflineFileUseCase = mock()
    private val getOfflineFileTotalSizeUseCase: GetOfflineFileTotalSizeUseCase = mock()
    private val getThumbnailUseCase: GetThumbnailUseCase = mock()
    private val monitorViewType: MonitorViewType = mock()
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
            offlineFolderInformationUseCase = offlineFolderInformationUseCase,
            getOfflineFileUseCase = getOfflineFileUseCase,
            getOfflineFileTotalSizeUseCase = getOfflineFileTotalSizeUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            monitorViewType = monitorViewType
        )
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
        val offlineFolderInfo = OfflineFolderInfo(
            numFolders = 0,
            numFiles = 0
        )
        val offlineList1 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.lastModifiedTime).thenReturn(100000L)

        val offlineList2 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.lastModifiedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)

        val file = mock<File>()

        whenever(getOfflineFileUseCase(offlineList1)).thenReturn(file)
        whenever(getOfflineFileUseCase(offlineList2)).thenReturn(file)
        whenever(getOfflineFileTotalSizeUseCase(file)).thenReturn(12345L)
        whenever(offlineFolderInformationUseCase(-1)).thenReturn(offlineFolderInfo)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)

        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInfoUiState(
                    id = parentId,
                    parentId = 0,
                    title = "Sample",
                    isFolder = true
                ),
                isSelected = false
            )
        )
        assertThat(underTest.uiState.value.offlineNodes).hasSize(2)
    }

    @Test
    fun `test that when back clicked, it calls its parent id`() = runTest {
        val parentId = 1
        val offlineList1 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.lastModifiedTime).thenReturn(100000L)

        val offlineList2 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.lastModifiedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInfoUiState(
                    id = parentId,
                    parentId = 0,
                    title = "Sample",
                    isFolder = true
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
            val offlineList1 = mock<OtherOfflineNodeInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.lastModifiedTime).thenReturn(100000L)

            val offlineList2 = mock<OtherOfflineNodeInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.lastModifiedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUIItem(
                    offlineNode = OfflineFileInfoUiState(
                        id = parentId,
                        parentId = 0,
                        title = "Sample",
                        isFolder = true
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
            val offlineList1 = mock<OtherOfflineNodeInformation>()
            whenever(offlineList1.isFolder).thenReturn(true)
            whenever(offlineList1.name).thenReturn("folder")
            whenever(offlineList1.handle).thenReturn("1234")
            whenever(offlineList1.lastModifiedTime).thenReturn(100000L)

            val offlineList2 = mock<OtherOfflineNodeInformation>()
            whenever(offlineList2.isFolder).thenReturn(false)
            whenever(offlineList2.name).thenReturn("file")
            whenever(offlineList2.handle).thenReturn("2345")
            whenever(offlineList2.lastModifiedTime).thenReturn(100000L)

            val list = listOf(offlineList1, offlineList2)
            whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
            underTest.onItemClicked(
                offlineNodeUIItem = OfflineNodeUIItem(
                    offlineNode = OfflineFileInfoUiState(
                        id = parentId,
                        parentId = 0,
                        title = "Sample",
                        isFolder = true
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
        val offlineList1 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList1.isFolder).thenReturn(true)
        whenever(offlineList1.name).thenReturn("folder")
        whenever(offlineList1.handle).thenReturn("1234")
        whenever(offlineList1.lastModifiedTime).thenReturn(100000L)

        val offlineList2 = mock<OtherOfflineNodeInformation>()
        whenever(offlineList2.isFolder).thenReturn(false)
        whenever(offlineList2.name).thenReturn("file")
        whenever(offlineList2.handle).thenReturn("2345")
        whenever(offlineList2.lastModifiedTime).thenReturn(100000L)

        val list = listOf(offlineList1, offlineList2)
        whenever(getOfflineNodesByParentIdUseCase(parentId)).thenReturn(list)
        underTest.onItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInfoUiState(
                    id = parentId,
                    parentId = 0,
                    title = "Sample",
                    isFolder = true
                ),
                isSelected = false
            )
        )
        underTest.onLongItemClicked(
            offlineNodeUIItem = OfflineNodeUIItem(
                offlineNode = OfflineFileInfoUiState(
                    id = 1,
                    handle = 1234,
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
        whenever(offlineFolderInformationUseCase(-1)).thenReturn(
            OfflineFolderInfo(
                numFolders = 0,
                numFiles = 0
            )
        )
        whenever(getOfflineFileUseCase(any())).thenReturn(mock())
        whenever(getOfflineFileTotalSizeUseCase(any())).thenReturn(-1L)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineNodesByParentIdUseCase,
            monitorTransfersFinishedUseCase,
            monitorOfflineWarningMessageVisibilityUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineNodeUpdatesUseCase,
            offlineFolderInformationUseCase,
            getOfflineFileUseCase,
            getOfflineFileTotalSizeUseCase,
            getThumbnailUseCase,
            monitorViewType
        )
    }
}