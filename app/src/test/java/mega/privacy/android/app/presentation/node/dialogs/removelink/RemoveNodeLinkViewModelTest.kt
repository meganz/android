package mega.privacy.android.app.presentation.node.dialogs.removelink

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
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
class RemoveNodeLinkViewModelTest {

    private val disableExportNodesUseCase: DisableExportNodesUseCase = mock()
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private val underTest = RemoveNodeLinkViewModel(
        applicationScope = applicationScope,
        disableExportNodesUseCase = disableExportNodesUseCase,
        removePublicLinkResultMapper = removePublicLinkResultMapper,
        snackBarHandler = snackBarHandler
    )

    private val handles = listOf(
        1L,
        2L
    )

    @BeforeAll
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that disableExportNodesUseCase is invoked when calling disableExport`() = runTest {
        val nodeIds = handles.map {
            NodeId(it)
        }
        whenever(disableExportNodesUseCase(nodeIds)).thenReturn(
            ResultCount(
                successCount = 1,
                errorCount = 0
            )
        )

        underTest.disableExport(handles)
        verify(disableExportNodesUseCase).invoke(nodeIds)
    }

    @AfterEach
    fun resetMock() {
        reset(
            disableExportNodesUseCase,
            removePublicLinkResultMapper,
            snackBarHandler
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}