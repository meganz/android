package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.usecase.node.RemoveShareUseCase
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
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
class RemoveShareFolderViewModelTest {

    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase = mock()
    private val removeShareUseCase: RemoveShareUseCase = mock()
    private val removeShareResultMapper: RemoveShareResultMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    private val underTest = RemoveShareFolderViewModel(
        applicationScope = applicationScope,
        getOutShareByNodeIdUseCase = getOutShareByNodeIdUseCase,
        removeShareUseCase = removeShareUseCase,
        removeShareResultMapper = removeShareResultMapper,
        snackBarHandler = snackBarHandler
    )

    @BeforeAll
    fun setDispatchers() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that when getContactInfoForSharedFolder it updates contacts and total folder in state`() =
        runTest {
            val nodeIds = listOf(NodeId(1234L))
            val shareData = mock<ShareData>()
            val shareDataList = listOf(shareData)
            whenever(getOutShareByNodeIdUseCase(NodeId(1234L))).thenReturn(shareDataList)
            underTest.getContactInfoForSharedFolder(nodeIds)
            val state = underTest.state.value
            verify(getOutShareByNodeIdUseCase).invoke((NodeId(1234L)))
            Truth.assertThat(state.numberOfShareContact).isEqualTo(shareDataList.size)
            Truth.assertThat(state.numberOfShareFolder).isEqualTo(nodeIds.size)
        }

    @Test
    fun `test that when remove share is confirmed, it triggers message`() = runTest {
        val nodeIds = listOf(NodeId(1234L))
        whenever(removeShareUseCase(nodeIds)).thenReturn(
            ResultCount(
                successCount = 1,
                errorCount = 0
            )
        )
        underTest.removeShare(nodeIds)
        verify(removeShareUseCase).invoke(nodeIds)
    }

    @AfterEach
    fun resetMock() {
        reset(
            getOutShareByNodeIdUseCase,
            removeShareUseCase,
            removeShareResultMapper
        )
    }

    @AfterAll
    fun resetDispatchers() {
        Dispatchers.resetMain()
    }
}