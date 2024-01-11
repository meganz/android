package mega.privacy.android.app.presentation.node.dialogs.removelink

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveNodeLinkViewModelTest {

    private val disableExportNodesUseCase: DisableExportNodesUseCase = mock()
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper = mock()
    private val underTest = RemoveNodeLinkViewModel(
        disableExportNodesUseCase = disableExportNodesUseCase,
        removePublicLinkResultMapper = removePublicLinkResultMapper
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
    fun `test that when node link is removed it calls disableExportNodesUseCase`() = runTest {
        val nodeIds = handles.map {
            NodeId(it)
        }
        underTest.disableExport(handles)
        verify(disableExportNodesUseCase).invoke(nodeIds)
    }

    @AfterEach
    fun resetMock() {
        reset(
            disableExportNodesUseCase,
            removePublicLinkResultMapper
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}