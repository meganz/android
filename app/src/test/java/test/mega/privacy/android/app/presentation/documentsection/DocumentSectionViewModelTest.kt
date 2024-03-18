package test.mega.privacy.android.app.presentation.documentsection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.documentsection.DocumentSectionViewModel
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentSectionViewModelTest {
    private lateinit var underTest: DocumentSectionViewModel

    private val getAllDocumentsUseCase = mock<GetAllDocumentsUseCase>()
    private val documentUIEntityMapper = mock<DocumentUiEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val monitorViewType = mock<MonitorViewType>()
    private val fakeMonitorNodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val fakeMonitorOfflineNodeUpdatesFlow = MutableSharedFlow<List<Offline>>()
    private val fakeMonitorViewTypeFlow = MutableSharedFlow<ViewType>()

    private val expectedDocument: DocumentUiEntity =
        mock { on { name }.thenReturn("document name") }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(
            fakeMonitorOfflineNodeUpdatesFlow
        )
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { getCloudSortOrder() }.thenReturn(SortOrder.ORDER_NONE)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = DocumentSectionViewModel(
            getAllDocumentsUseCase = getAllDocumentsUseCase,
            documentUIEntityMapper = documentUIEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorViewType = monitorViewType
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllDocumentsUseCase,
            documentUIEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorViewType
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.allDocuments).isEmpty()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.isLoading).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the audios are retrieved after the nodes are refreshed`() = runTest {
        initDocumentNodeListReturned()

        initUnderTest()
        underTest.refreshDocumentNodes()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allDocuments.size).isEqualTo(2)
            assertThat(actual.isLoading).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initDocumentNodeListReturned() {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllDocumentsUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(documentUIEntityMapper(any())).thenReturn(expectedDocument)
    }

    @Test
    fun `test that the currentViewType is correctly updated when monitorViewType is triggered`() =
        runTest {
            underTest.uiState.drop(1).test {
                fakeMonitorViewTypeFlow.emit(ViewType.GRID)
                assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
                cancelAndIgnoreRemainingEvents()
            }
        }
}