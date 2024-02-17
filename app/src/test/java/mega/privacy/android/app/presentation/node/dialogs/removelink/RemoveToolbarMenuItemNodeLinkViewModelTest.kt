package mega.privacy.android.app.presentation.node.dialogs.removelink

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveToolbarMenuItemNodeLinkViewModelTest {

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
}